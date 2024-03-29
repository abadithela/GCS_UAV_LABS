/*
Copyright (C) 2001, 2007 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers.rpf;

import com.sun.opengl.util.texture.*;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.formats.dds.DDSCompressor;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.retrieve.*;
import gov.nasa.worldwind.util.*;

import java.awt.image.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: RPFTiledImageLayer.java 12778 2009-11-10 01:20:37Z tgaskins $
 */
public class RPFTiledImageLayer extends TiledImageLayer
{
    private AVList creationParams;
    private final RPFGenerator rpfGenerator;
    private final Object fileLock = new Object();

    public static final String RPF_ROOT_PATH = "rpf.RootPath";
    public static final String RPF_DATA_SERIES_ID = "rpf.DataSeriesId";

    static Collection<Tile> createTopLevelTiles(AVList params)
    {
        if (params == null)
        {
            String message = Logging.getMessage("nullValue.LayerConfigParams");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        LevelSet levels = new LevelSet(initParams(params));
        Sector sector = levels.getSector();

        Level level = levels.getFirstLevel();
        Angle dLat = level.getTileDelta().getLatitude();
        Angle dLon = level.getTileDelta().getLongitude();
        Angle latOrigin = levels.getTileOrigin().getLatitude();
        Angle lonOrigin = levels.getTileOrigin().getLongitude();

        // Determine the row and column offset from the common World Wind global tiling origin.
        int firstRow = Tile.computeRow(dLat, sector.getMinLatitude(), latOrigin);
        int firstCol = Tile.computeColumn(dLon, sector.getMinLongitude(), lonOrigin);
        int lastRow = Tile.computeRow(dLat, sector.getMaxLatitude(), latOrigin);
        int lastCol = Tile.computeColumn(dLon, sector.getMaxLongitude(), lonOrigin);

        int nLatTiles = lastRow - firstRow + 1;
        int nLonTiles = lastCol - firstCol + 1;

        ArrayList<Tile> topLevels = new ArrayList<Tile>(nLatTiles * nLonTiles);

        Angle p1 = Tile.computeRowLatitude(firstRow, dLat, latOrigin);
        for (int row = firstRow; row <= lastRow; row++)
        {
            Angle p2;
            p2 = p1.add(dLat);

            Angle t1 = Tile.computeColumnLongitude(firstCol, dLon, lonOrigin);
            for (int col = firstCol; col <= lastCol; col++)
            {
                Angle t2;
                t2 = t1.add(dLon);

                topLevels.add(new Tile(new Sector(p1, p2, t1, t2), level, row, col));
                t1 = t2;
            }
            p1 = p2;
        }

        return topLevels;
    }

    static String getFileIndexCachePath(String rootPath, String dataSeriesId)
    {
        String path = null;
        if (rootPath != null && dataSeriesId != null)
        {
            path = WWIO.formPath(
                rootPath,
                dataSeriesId,
                "rpf_file_index.idx");
        }
        return path;
    }

    public static RPFTiledImageLayer fromRestorableState(String stateInXml)
    {
        if (stateInXml == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return new RPFTiledImageLayer(stateInXml);
    }

    public RPFTiledImageLayer(String stateInXml)
    {
        this(xmlStateToParams(stateInXml));

        RestorableSupport rs;
        try
        {
            rs = RestorableSupport.parse(stateInXml);
        }
        catch (Exception e)
        {
            // Parsing the document specified by stateInXml failed.
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", stateInXml);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message, e);
        }

        Boolean b = rs.getStateValueAsBoolean("rpf.LayerEnabled");
        if (b != null)
            this.setEnabled(b);

        Double d = rs.getStateValueAsDouble("rpf.Opacity");
        if (d != null)
            this.setOpacity(d);

        d = rs.getStateValueAsDouble("rpf.MinActiveAltitude");
        if (d != null)
            this.setMinActiveAltitude(d);

        d = rs.getStateValueAsDouble("rpf.MaxActiveAltitude");
        if (d != null)
            this.setMaxActiveAltitude(d);

        String s = rs.getStateValueAsString("rpf.LayerName");
        if (s != null)
            this.setName(s);

        b = rs.getStateValueAsBoolean("rpf.UseMipMaps");
        if (b != null)
            this.setUseMipMaps(b);

        b = rs.getStateValueAsBoolean("rpf.UseTransparentTextures");
        if (b != null)
            this.setUseTransparentTextures(b);

        RestorableSupport.StateObject so = rs.getStateObject("avlist");
        if (so != null)
        {
            RestorableSupport.StateObject[] avpairs = rs.getAllStateObjects(so, "");
            if (avpairs != null)
            {
                for (RestorableSupport.StateObject avp : avpairs)
                {
                    if (avp != null)
                        setValue(avp.getName(), avp.getValue());
                }
            }
        }
    }

    public RPFTiledImageLayer(AVList params)
    {
        super(new LevelSet(initParams(params)));

        this.initRPFFileIndex(params);
        this.creationParams = params.copy();
        this.rpfGenerator = new RPFGenerator(params);

        this.setValue(AVKey.CONSTRUCTION_PARAMETERS, params);
        //this.setUseMipMaps(true);
        this.setUseTransparentTextures(true);
        this.setName(makeTitle(params));
    }

    protected void initRPFFileIndex(AVList params)
    {
        // Load the RPFFileIndex associated with this RPFTiledImageLayer, and update the layer's expiry time according
        // to the last modified time on the RPFFileIndex.

        FileStore fileStore = WorldWind.getDataFileStore();

        // Root path and data series ID parameters should have already been validated in initParams().
        String rootPath = params.getStringValue(RPF_ROOT_PATH);
        String dataSeriesId = params.getStringValue(RPF_DATA_SERIES_ID);
        File file = fileStore.newFile(getFileIndexCachePath(rootPath, dataSeriesId));

        RPFFileIndex fileIndex = (RPFFileIndex) params.getValue(RPFGenerator.RPF_FILE_INDEX);
        if (fileIndex == null)
        {
            fileIndex = initFileIndex(file);
            if (fileIndex == null)
            {
                String message = Logging.getMessage("nullValue.RPFFileIndexIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }
            params.setValue(RPFGenerator.RPF_FILE_INDEX, fileIndex);
        }

        // Default to expiring data on the date the DDS converter was updated. If the RPFFileIndex's last-modified time
        // is newer than the default expiry time, then use newer of the two. This ensures that layer imagery always
        // reflects whats in the RPFFileIndex. If the layer has been re-imported (data has been added, or data has been
        // removed), then all previously created layer imagery will be expired (but not necessarily the preprocessed
        // data).
        Long expiryTime = new GregorianCalendar(2009, Calendar.FEBRUARY, 25).getTimeInMillis();
        if (file != null && file.lastModified() > expiryTime)
        {
            expiryTime = file.lastModified();
        }
        this.setExpiryTime(expiryTime);
    }

    private static AVList initParams(AVList params)
    {
        if (params == null)
        {
            String message = Logging.getMessage("nullValue.LayerConfigParams");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String rootPath = params.getStringValue(RPF_ROOT_PATH);
        if (rootPath == null)
        {
            String message = Logging.getMessage("nullValue.RPFRootPath");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String dataSeriesId = params.getStringValue(RPF_DATA_SERIES_ID);
        if (dataSeriesId == null)
        {
            String message = Logging.getMessage("nullValue.RPFDataSeriesIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Use a dummy value for service.
        if (params.getValue(AVKey.SERVICE) == null)
            params.setValue(AVKey.SERVICE, "file://" + RPFGenerator.class.getName() + "?");

        // Use a dummy value for dataset-name.
        if (params.getValue(AVKey.DATASET_NAME) == null)
            params.setValue(AVKey.DATASET_NAME, dataSeriesId);

        if (params.getValue(AVKey.LEVEL_ZERO_TILE_DELTA) == null)
        {
            Angle delta = Angle.fromDegrees(36);
            params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(delta, delta));
        }

        if (params.getValue(AVKey.TILE_WIDTH) == null)
            params.setValue(AVKey.TILE_WIDTH, 512);
        if (params.getValue(AVKey.TILE_HEIGHT) == null)
            params.setValue(AVKey.TILE_HEIGHT, 512);
        if (params.getValue(AVKey.FORMAT_SUFFIX) == null)
            params.setValue(AVKey.FORMAT_SUFFIX, ".dds");
        if (params.getValue(AVKey.NUM_LEVELS) == null)
            params.setValue(AVKey.NUM_LEVELS, 14); // approximately 0.5 meters per pixel
        if (params.getValue(AVKey.NUM_EMPTY_LEVELS) == null)
            params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);

        params.setValue(AVKey.TILE_URL_BUILDER, new URLBuilder());

        // RPFTiledImageLayer is typically constructed either by the {@link RPFTiledImageProcessor}, or from restorable
        // state XML. In the first case, either the sector parameter or the RPFFileIndex parameter are specified by the
        // processor. In the latter case, the sector is restored as part of the state xml.
        Sector sector = (Sector) params.getValue(AVKey.SECTOR);
        if (sector == null)
        {
            RPFFileIndex fileIndex = (RPFFileIndex) params.getValue(RPFGenerator.RPF_FILE_INDEX);
            if (fileIndex != null && fileIndex.getIndexProperties() != null)
                sector = fileIndex.getIndexProperties().getBoundingSector();

            if (sector == null)
            {
                String message = Logging.getMessage("RPFTiledImageLayer.NoGeographicBoundingBox");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            params.setValue(AVKey.SECTOR, sector);
        }

        if (params.getValue(AVKey.DATA_CACHE_NAME) == null)
        {
            String cacheName = WWIO.formPath(rootPath, dataSeriesId);
            params.setValue(AVKey.DATA_CACHE_NAME, cacheName);
        }

        return params;
    }

    private static RPFFileIndex initFileIndex(File file)
    {
        ByteBuffer buffer;
        try
        {
            buffer = WWIO.mapFile(file);
        }
        catch (Exception e)
        {
            String message = "Exception while attempting to map file: " + file;
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            buffer = null;
        }

        RPFFileIndex fileIndex = null;
        try
        {
            if (buffer != null)
            {
                fileIndex = new RPFFileIndex();
                fileIndex.load(buffer);
            }
        }
        catch (Exception e)
        {
            String message = "Exception while attempting to load RPFFileIndex: " + file;
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            fileIndex = null;
        }

        return fileIndex;
    }

    private static String makeTitle(AVList params)
    {
        StringBuilder sb = new StringBuilder();

        Object o = params.getValue(RPFGenerator.RPF_FILE_INDEX);
        if (o != null && o instanceof RPFFileIndex)
        {
            RPFFileIndex fileIndex = (RPFFileIndex) o;
            if (fileIndex.getIndexProperties() != null)
            {
                if (fileIndex.getIndexProperties().getDescription() != null)
                    sb.append(fileIndex.getIndexProperties().getDescription());
                else
                    sb.append(fileIndex.getIndexProperties().getDataSeriesIdentifier());
            }
        }

        if (sb.length() == 0)
        {
            String rootPath = params.getStringValue(RPF_ROOT_PATH);
            String dataSeriesId = params.getStringValue(RPF_DATA_SERIES_ID);
            if (rootPath != null && dataSeriesId != null)
            {
                sb.append(rootPath).append(":").append(dataSeriesId);
            }
        }

        return sb.toString();
    }

    protected void checkResources()
    {
        // Intentionally left blank.
    }

    private RestorableSupport makeRestorableState(AVList params)
    {
        RestorableSupport rs = RestorableSupport.newRestorableSupport();
        // Creating a new RestorableSupport failed. RestorableSupport logged the problem, so just return null.
        if (rs == null)
            return null;

        for (Map.Entry<String, Object> p : params.getEntries())
        {
            if (p.getValue() instanceof LatLon)
            {
                rs.addStateValueAsDouble(p.getKey() + ".Latitude", ((LatLon) p.getValue()).getLatitude().degrees);
                rs.addStateValueAsDouble(p.getKey() + ".Longitude", ((LatLon) p.getValue()).getLongitude().degrees);
            }
            else if (p.getValue() instanceof Sector)
            {
                rs.addStateValueAsDouble(p.getKey() + ".MinLatitude", ((Sector) p.getValue()).getMinLatitude().degrees);
                rs.addStateValueAsDouble(p.getKey() + ".MaxLatitude", ((Sector) p.getValue()).getMaxLatitude().degrees);
                rs.addStateValueAsDouble(p.getKey() + ".MinLongitude",
                    ((Sector) p.getValue()).getMinLongitude().degrees);
                rs.addStateValueAsDouble(p.getKey() + ".MaxLongitude",
                    ((Sector) p.getValue()).getMaxLongitude().degrees);
            }
            else if (p.getValue() instanceof URLBuilder)
            {
                // Intentionally left blank. URLBuilder will be created from scratch in fromRestorableState().
            }
            else if (p.getKey().equals(RPFGenerator.RPF_FILE_INDEX))
            {
                // Intentionally left blank.
            }
            else
            {
                rs.addStateValueAsString(p.getKey(), p.getValue().toString());
            }
        }

        rs.addStateValueAsBoolean("rpf.LayerEnabled", this.isEnabled());
        rs.addStateValueAsDouble("rpf.Opacity", this.getOpacity());
        rs.addStateValueAsDouble("rpf.MinActiveAltitude", this.getMinActiveAltitude());
        rs.addStateValueAsDouble("rpf.MaxActiveAltitude", this.getMaxActiveAltitude());
        rs.addStateValueAsString("rpf.LayerName", this.getName());
        rs.addStateValueAsBoolean("rpf.UseMipMaps", this.isUseMipMaps());
        rs.addStateValueAsBoolean("rpf.UseTransparentTextures", this.isUseTransparentTextures());

        RestorableSupport.StateObject so = rs.addStateObject("avlist");
        for (Map.Entry<String, Object> p : this.getEntries())
        {
            if (p.getKey().equals(AVKey.CONSTRUCTION_PARAMETERS))
                continue;

            rs.addStateValueAsString(so, p.getKey(), p.getValue().toString());
        }

        return rs;
    }

    public String getRestorableState()
    {
        return this.makeRestorableState(this.creationParams).getStateAsXml();
    }

    public static AVList xmlStateToParams(String stateInXml)
    {
        if (stateInXml == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        RestorableSupport rs;
        try
        {
            rs = RestorableSupport.parse(stateInXml);
        }
        catch (Exception e)
        {
            // Parsing the document specified by stateInXml failed.
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", stateInXml);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message, e);
        }

        AVList params = new AVListImpl();

        String s = rs.getStateValueAsString(RPF_ROOT_PATH);
        if (s != null)
            params.setValue(RPF_ROOT_PATH, s);

        s = rs.getStateValueAsString(RPF_DATA_SERIES_ID);
        if (s != null)
            params.setValue(RPF_DATA_SERIES_ID, s);

        s = rs.getStateValueAsString(AVKey.IMAGE_FORMAT);
        if (s != null)
            params.setValue(AVKey.IMAGE_FORMAT, s);

        s = rs.getStateValueAsString(AVKey.DATA_CACHE_NAME);
        if (s != null)
            params.setValue(AVKey.DATA_CACHE_NAME, s);

        s = rs.getStateValueAsString(AVKey.SERVICE);
        if (s != null)
            params.setValue(AVKey.SERVICE, s);

        s = rs.getStateValueAsString(AVKey.TITLE);
        if (s != null)
            params.setValue(AVKey.TITLE, s);

        s = rs.getStateValueAsString(AVKey.DATASET_NAME);
        if (s != null)
            params.setValue(AVKey.DATASET_NAME, s);

        s = rs.getStateValueAsString(AVKey.FORMAT_SUFFIX);
        if (s != null)
            params.setValue(AVKey.FORMAT_SUFFIX, s);

        s = rs.getStateValueAsString(AVKey.LAYER_NAMES);
        if (s != null)
            params.setValue(AVKey.LAYER_NAMES, s);

        s = rs.getStateValueAsString(AVKey.STYLE_NAMES);
        if (s != null)
            params.setValue(AVKey.STYLE_NAMES, s);

        Integer i = rs.getStateValueAsInteger(AVKey.NUM_EMPTY_LEVELS);
        if (i != null)
            params.setValue(AVKey.NUM_EMPTY_LEVELS, i);

        i = rs.getStateValueAsInteger(AVKey.NUM_LEVELS);
        if (i != null)
            params.setValue(AVKey.NUM_LEVELS, i);

        i = rs.getStateValueAsInteger(AVKey.TILE_WIDTH);
        if (i != null)
            params.setValue(AVKey.TILE_WIDTH, i);

        i = rs.getStateValueAsInteger(AVKey.TILE_HEIGHT);
        if (i != null)
            params.setValue(AVKey.TILE_HEIGHT, i);

        Double lat = rs.getStateValueAsDouble(AVKey.LEVEL_ZERO_TILE_DELTA + ".Latitude");
        Double lon = rs.getStateValueAsDouble(AVKey.LEVEL_ZERO_TILE_DELTA + ".Longitude");
        if (lat != null && lon != null)
            params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, LatLon.fromDegrees(lat, lon));

        Double minLat = rs.getStateValueAsDouble(AVKey.SECTOR + ".MinLatitude");
        Double minLon = rs.getStateValueAsDouble(AVKey.SECTOR + ".MinLongitude");
        Double maxLat = rs.getStateValueAsDouble(AVKey.SECTOR + ".MaxLatitude");
        Double maxLon = rs.getStateValueAsDouble(AVKey.SECTOR + ".MaxLongitude");
        if (minLat != null && minLon != null && maxLat != null && maxLon != null)
            params.setValue(AVKey.SECTOR, Sector.fromDegrees(minLat, maxLat, minLon, maxLon));

        params.setValue(AVKey.TILE_URL_BUILDER, new URLBuilder());

        return params;
    }

    public void restoreState(String stateInXml)
    {
        String message = Logging.getMessage("RestorableSupport.RestoreRequiresConstructor");
        Logging.logger().severe(message);
        throw new UnsupportedOperationException(message);
    }

    private static class URLBuilder implements TileUrlBuilder
    {
        public String URLTemplate = null;

        private URLBuilder()
        {}

        public java.net.URL getURL(Tile tile, String imageFormat) throws java.net.MalformedURLException
        {
            StringBuffer sb;
            if (this.URLTemplate == null)
            {
                sb = new StringBuffer(tile.getLevel().getService());
                sb.append("dataset=");
                sb.append(tile.getLevel().getDataset());
                sb.append("&width=");
                sb.append(tile.getLevel().getTileWidth());
                sb.append("&height=");
                sb.append(tile.getLevel().getTileHeight());

                this.URLTemplate = sb.toString();
            }
            else
            {
                sb = new StringBuffer(this.URLTemplate);
            }

            Sector s = tile.getSector();
            sb.append("&bbox=");
            sb.append(s.getMinLongitude().getDegrees());
            sb.append(",");
            sb.append(s.getMinLatitude().getDegrees());
            sb.append(",");
            sb.append(s.getMaxLongitude().getDegrees());
            sb.append(",");
            sb.append(s.getMaxLatitude().getDegrees());
            sb.append("&"); // terminate the query string

            return new java.net.URL(sb.toString().replace(" ", "%20"));
        }
    }

    protected void forceTextureLoad(TextureTile tile)
    {
        final java.net.URL textureURL = WorldWind.getDataFileStore().findFile(tile.getPath(), true);

        if (textureURL != null)
        {
            this.loadTexture(tile, textureURL);
        }
    }

    protected void requestTexture(DrawContext dc, TextureTile tile)
    {
        Vec4 centroid = tile.getCentroidPoint(dc.getGlobe());
        Vec4 referencePoint = this.getReferencePoint(dc);
        if (referencePoint != null)
            tile.setPriority(centroid.distanceTo3(referencePoint));

        RequestTask task = new RequestTask(tile, this);
        this.getRequestQ().add(task);
    }

    private static class RequestTask extends TileTask
    {
        private final RPFTiledImageLayer layer;

        private RequestTask(TextureTile tile, RPFTiledImageLayer layer)
        {
            super(tile);
            this.layer = layer;
        }

        public void run()
        {
            final TextureTile tile = getTile();

            // TODO: check to ensure load is still needed

            final java.net.URL textureURL = WorldWind.getDataFileStore().findFile(tile.getPath(), false);
            if (textureURL != null)
            {
                if (this.layer.loadTexture(tile, textureURL))
                {
                    layer.getLevels().unmarkResourceAbsent(tile);
                    this.layer.firePropertyChange(AVKey.LAYER, null, this);
                    return;
                }
                else
                {
                    // Assume that something's wrong with the file and delete it.
                    gov.nasa.worldwind.WorldWind.getDataFileStore().removeFile(textureURL);
                    layer.getLevels().markResourceAbsent(tile);
                    String message = Logging.getMessage("generic.DeletedCorruptDataFile", textureURL);
                    Logging.logger().info(message);
                }
            }

            this.layer.downloadTexture(tile);
        }
    }

    private boolean loadTexture(TextureTile tile, java.net.URL textureURL)
    {
        if (WWIO.isFileOutOfDate(textureURL, tile.getLevel().getExpiryTime()))
        {
            // The file has expired. Delete it then request download of newer.
            gov.nasa.worldwind.WorldWind.getDataFileStore().removeFile(textureURL);
            String message = Logging.getMessage("generic.DataFileExpired", textureURL);
            Logging.logger().fine(message);
            return false;
        }

        TextureData textureData;

        synchronized (this.fileLock)
        {
            textureData = readTexture(textureURL, this.isUseMipMaps());
        }

        if (textureData == null)
            return false;

        tile.setTextureData(textureData);
        if (tile.getLevelNumber() != 0 || !this.isRetainLevelZeroTiles())
            this.addTileToCache(tile);

        return true;
    }

    private static TextureData readTexture(java.net.URL url, boolean useMipMaps)
    {
        try
        {
            return TextureIO.newTextureData(url, useMipMaps, null);
        }
        catch (Exception e)
        {
            Logging.logger().log(
                java.util.logging.Level.SEVERE, "layers.TextureLayer.ExceptionAttemptingToReadTextureFile", e);
            return null;
        }
    }

    private void addTileToCache(TextureTile tile)
    {
        TextureTile.getMemoryCache().add(tile.getTileKey(), tile);
    }

    protected void downloadTexture(final TextureTile tile)
    {
        RPFGenerator.RPFServiceInstance service = this.rpfGenerator.getServiceInstance();
        if (service == null)
            return;

        java.net.URL url;
        try
        {
            url = tile.getResourceURL();
        }
        catch (java.net.MalformedURLException e)
        {
            Logging.logger().log(java.util.logging.Level.SEVERE,
                Logging.getMessage("layers.TextureLayer.ExceptionCreatingTextureUrl", tile), e);
            return;
        }

        if (WorldWind.getRetrievalService().isAvailable())
        {
            Retriever retriever = new RPFRetriever(service, url, new DownloadPostProcessor(tile, this));
            // Apply any overridden timeouts.
            Integer srl = AVListImpl.getIntegerValue(this, AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT);
            if (srl != null && srl > 0)
                retriever.setStaleRequestLimit(srl);
            WorldWind.getRetrievalService().runRetriever(retriever, tile.getPriority());
        }
        else
        {
            DownloadTask task = new DownloadTask(service, url, tile, this);
            this.getRequestQ().add(task);
        }
    }

    private static class DownloadPostProcessor implements RetrievalPostProcessor
    {
        private final TextureTile tile;
        private final RPFTiledImageLayer layer;

        public DownloadPostProcessor(TextureTile tile, RPFTiledImageLayer layer)
        {
            this.tile = tile;
            this.layer = layer;
        }

        public ByteBuffer run(Retriever retriever)
        {
            if (retriever == null)
            {
                String msg = Logging.getMessage("nullValue.RetrieverIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }

            try
            {
                if (!retriever.getState().equals(Retriever.RETRIEVER_STATE_SUCCESSFUL))
                    return null;

                ByteBuffer buffer = retriever.getBuffer();

                if (retriever instanceof RPFRetriever)
                {
                    RPFRetriever rpr = (RPFRetriever) retriever;
                    if (rpr.getResponseCode() == RPFRetriever.RESPONSE_CODE_NO_CONTENT)
                    {
                        // Mark tile as missing to avoid excessive attempts
                        this.layer.getLevels().markResourceAbsent(this.tile);
                        return null;
                    }
                    else if (rpr.getResponseCode() != RPFRetriever.RESPONSE_CODE_OK)
                    {
                        // Also mark tile as missing, but for an unknown reason.
                        this.layer.getLevels().markResourceAbsent(this.tile);
                        return null;
                    }
                }

                final File outFile = WorldWind.getDataFileStore().newFile(this.tile.getPath());
                if (outFile == null)
                    return null;

                if (outFile.exists())
                    return buffer;

                if (buffer != null)
                {
                    String contentType = retriever.getContentType();
                    if (contentType == null)
                    {
                        // TODO: logger message
                        return null;
                    }

                    if (contentType.contains("dds"))
                    {
                        this.layer.saveBuffer(buffer, outFile);
                    }
                    else if (outFile.getName().endsWith(".dds"))
                    {
                        // Convert to DDS and save the result.
                        buffer = DDSCompressor.compressImageBuffer(buffer);
                        if (buffer != null)
                            this.layer.saveBuffer(buffer, outFile);
                    }

                    if (buffer != null)
                    {
                        this.layer.firePropertyChange(AVKey.LAYER, null, this.layer);
                    }
                    return buffer;
                }
            }
            catch (ClosedByInterruptException e)
            {
                Logging.logger().log(java.util.logging.Level.FINE,
                    Logging.getMessage("generic.OperationCancelled", "RPF image retrieval"), e);
            }
            catch (java.io.IOException e)
            {
                this.layer.getLevels().markResourceAbsent(this.tile);
                Logging.logger().log(java.util.logging.Level.SEVERE,
                    Logging.getMessage("layers.TextureLayer.ExceptionSavingRetrievedTextureFile", tile.getPath()), e);
            }
            return null;
        }
    }

    private static class DownloadTask extends TileTask
    {
        private final RPFGenerator.RPFServiceInstance service;
        private final java.net.URL url;
        private final RPFTiledImageLayer layer;

        private DownloadTask(RPFGenerator.RPFServiceInstance service, java.net.URL url, TextureTile tile, RPFTiledImageLayer layer)
        {
            super(tile);
            this.service = service;
            this.url = url;
            this.layer = layer;
        }

        public void run()
        {
            final TextureTile tile = getTile();
            try
            {
                ByteBuffer buffer = createImage(this.service, this.url);
                if (buffer != null)
                {
                    final File outFile = WorldWind.getDataFileStore().newFile(tile.getPath());
                    if (outFile != null)
                    {
                        this.layer.saveBuffer(buffer, outFile);
                    }
                }
            }
            catch (Exception e)
            {
                Logging.logger().log(
                    java.util.logging.Level.SEVERE, "layers.TextureLayer.ExceptionAttemptingToCreateTileImage", e);
                this.layer.getLevels().markResourceAbsent(tile);
            }
        }
    }

    private static ByteBuffer createImage(RPFGenerator.RPFServiceInstance service, java.net.URL url) throws java.io.IOException
    {
        ByteBuffer buffer = null;
        BufferedImage bufferedImage = service.serviceRequest(url);
        if (bufferedImage != null)
        {
            buffer = DDSCompressor.compressImage(bufferedImage);
        }

        return buffer;
    }

    private void saveBuffer(ByteBuffer buffer, File outFile) throws java.io.IOException
    {
        synchronized (this.fileLock) // sychronized with read of file in RequestTask.run()
        {
            WWIO.saveBuffer(buffer, outFile);
        }
    }

    private static class TileTask implements Runnable, Comparable<TileTask>
    {
        private final TextureTile tile;

        private TileTask(TextureTile tile)
        {
            this.tile = tile;
        }

        public final TextureTile getTile()
        {
            return this.tile;
        }

        public void run()
        {
        }

        /**
         * @param that the task to compare
         * @return -1 if <code>this</code> less than <code>that</code>, 1 if greater than, 0 if equal
         * @throws IllegalArgumentException if <code>that</code> is null
         */
        public int compareTo(TileTask that)
        {
            if (that == null)
            {
                String msg = Logging.getMessage("nullValue.RequestTaskIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }
            return this.tile.getPriority() == that.tile.getPriority() ? 0 :
                this.tile.getPriority() < that.tile.getPriority() ? -1 : 1;
        }

        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            final TileTask that = (TileTask) o;

            // Don't include layer in comparison so that requests are shared among layers
            return !(tile != null ? !tile.equals(that.tile) : that.tile != null);
        }

        public int hashCode()
        {
            return (tile != null ? tile.hashCode() : 0);
        }

        public String toString()
        {
            return this.tile.getPath();
        }
    }
}