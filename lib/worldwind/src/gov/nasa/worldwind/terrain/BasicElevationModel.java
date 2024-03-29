/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.terrain;

import com.sun.opengl.util.BufferUtil;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.retrieve.*;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.wms.Capabilities;
import org.w3c.dom.*;

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.ClosedByInterruptException;
import java.util.*;

// Implementation notes, not for API doc:
//
// Implements an elevation model based on a quad tree of elevation tiles. Meant to be subclassed by very specific
// classes, e.g. Earth/SRTM. A Descriptor passed in at construction gives the configuration parameters. Eventually
// Descriptor will be replaced by an XML configuration document.
//
// A "tile" corresponds to one tile of the data set, which has a corresponding unique row/column address in the data
// set. An inner class implements Tile. An inner class also implements TileKey, which is used to address the
// corresponding Tile in the memory cache.

// Clients of this class get elevations from it by first getting an Elevations object for a specific Sector, then
// querying that object for the elevation at individual lat/lon positions. The Elevations object captures information
// that is used to compute elevations. See in-line comments for a description.
//
// When an elevation tile is needed but is not in memory, a task is threaded off to find it. If it's in the file cache
// then it's loaded by the task into the memory cache. If it's not in the file cache then a retrieval is initiated by
// the task. The disk is never accessed during a call to getElevations(sector, resolution) because that method is
// likely being called when a frame is being rendered. The details of all this are in-line below.

/**
 * @author Tom Gaskins
 * @version $Id: BasicElevationModel.java 12761 2009-10-31 02:55:32Z tgaskins $
 */
public class BasicElevationModel extends AbstractElevationModel implements BulkRetrievable
{
    private final LevelSet levels;
    private final double minElevation;
    private final double maxElevation;
    private String elevationDataPixelType = AVKey.INT16;
    private String elevationDataByteOrder = AVKey.LITTLE_ENDIAN;
    private double detailHint = 0.0;
    private final Object fileLock = new Object();
    private java.util.concurrent.ConcurrentHashMap<TileKey, ElevationTile> levelZeroTiles =
        new java.util.concurrent.ConcurrentHashMap<TileKey, ElevationTile>();
    private MemoryCache memoryCache;
    private int extremesLevel = -1;
    private BufferWrapper extremes = null;
    private MemoryCache extremesLookupCache;
    private int numExpectedValues = -1;
    // Model resource properties.
    private boolean serviceInitialized = false;
    private AbsentResourceList absentResources = new AbsentResourceList();
    protected static final int SERVICE_CAPABILITIES_RESOURCE_ID = 1;

    public BasicElevationModel(AVList params)
    {
        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ElevationModelConfigParams");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String s = params.getStringValue(AVKey.BYTE_ORDER);
        if (s != null)
            this.setByteOrder(s);

        Double d = (Double) params.getValue(AVKey.DETAIL_HINT);
        if (d != null)
            this.setDetailHint(d);

        s = params.getStringValue(AVKey.DISPLAY_NAME);
        if (s != null)
            this.setName(s);

        d = (Double) params.getValue(AVKey.ELEVATION_MIN);
        this.minElevation = d != null ? d : 0;

        d = (Double) params.getValue(AVKey.ELEVATION_MAX);
        this.maxElevation = d != null ? d : 0;

        Long lo = (Long) params.getValue(AVKey.EXPIRY_TIME);
        if (lo != null)
            params.setValue(AVKey.EXPIRY_TIME, lo);

        d = (Double) params.getValue(AVKey.MISSING_DATA_SIGNAL);
        if (d != null)
            this.setMissingDataSignal(d);

        d = (Double) params.getValue(AVKey.MISSING_DATA_REPLACEMENT);
        if (d != null)
            this.setMissingDataReplacement(d);

        Boolean b = (Boolean) params.getValue(AVKey.NETWORK_RETRIEVAL_ENABLED);
        if (b != null)
            this.setNetworkRetrievalEnabled(b);

        s = params.getStringValue(AVKey.PIXEL_TYPE);
        if (s != null)
            this.setPixelType(s);

        s = params.getStringValue(AVKey.ELEVATION_EXTREMES_FILE);
        if (s != null)
            this.loadExtremeElevations(s);

        // Set some fallback values if not already set.
        setFallbacks(params);

        this.levels = new LevelSet(params);
        this.memoryCache = this.createMemoryCache(ElevationTile.class.getName());

        this.setValue(AVKey.CONSTRUCTION_PARAMETERS, params.copy());

        this.checkResources(); // Check whether resources are present, and perform any necessary initialization.
    }

    public BasicElevationModel(Document dom, AVList params)
    {
        this(dom.getDocumentElement(), params);
    }

    public BasicElevationModel(Element domElement, AVList params)
    {
        this(getParamsFromDocument(domElement, params));
    }

    public BasicElevationModel(String restorableStateInXml)
    {
        this(restorableStateToParams(restorableStateInXml));

        RestorableSupport rs;
        try
        {
            rs = RestorableSupport.parse(restorableStateInXml);
        }
        catch (Exception e)
        {
            // Parsing the document specified by stateInXml failed.
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", restorableStateInXml);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message, e);
        }

        this.doRestoreState(rs, null);
    }

    protected static void setFallbacks(AVList params)
    {
        if (params.getValue(AVKey.TILE_WIDTH) == null)
            params.setValue(AVKey.TILE_WIDTH, 150);

        if (params.getValue(AVKey.TILE_HEIGHT) == null)
            params.setValue(AVKey.TILE_HEIGHT, 150);

        if (params.getValue(AVKey.FORMAT_SUFFIX) == null)
            params.setValue(AVKey.FORMAT_SUFFIX, ".bil");

        if (params.getValue(AVKey.NUM_LEVELS) == null)
            params.setValue(AVKey.NUM_LEVELS, 2);

        if (params.getValue(AVKey.NUM_EMPTY_LEVELS) == null)
            params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
    }

    protected static AVList getParamsFromDocument(Element domElement, AVList params)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
            params = new AVListImpl();

        ElevationModelConfiguration.getElevationModelParams(domElement, params);
        ElevationModelConfiguration.getBasicElevationModelParams(domElement, params);

        return params;
    }

    protected MemoryCache getMemoryCache()
    {
        return memoryCache;
    }

    protected MemoryCache createMemoryCache(String cacheName)
    {
        if (WorldWind.getMemoryCacheSet().containsCache(cacheName))
        {
            return WorldWind.getMemoryCache(cacheName);
        }
        else
        {
            long size = Configuration.getLongValue(AVKey.ELEVATION_TILE_CACHE_SIZE, 5000000L);
            MemoryCache mc = new BasicMemoryCache((long) (0.85 * size), size);
            mc.setName("Elevation Tiles");
            WorldWind.getMemoryCacheSet().addCache(cacheName, mc);
            return mc;
        }
    }

    public LevelSet getLevels()
    {
        return this.levels;
    }

    protected Map<TileKey, ElevationTile> getLevelZeroTiles()
    {
        return levelZeroTiles;
    }

    protected int getExtremesLevel()
    {
        return extremesLevel;
    }

    protected BufferWrapper getExtremes()
    {
        return extremes;
    }

    /**
     * Specifies the time of the elevation models's most recent dataset update, beyond which cached data is invalid. If
     * greater than zero, the model ignores and eliminates any in-memory or on-disk cached data older than the time
     * specified, and requests new information from the data source. If zero, the default, the model applies any expiry
     * times associated with its individual levels, but only for on-disk cached data. In-memory cached data is expired
     * only when the expiry time is specified with this method and is greater than zero. This method also overwrites the
     * expiry times of the model's individual levels if the value specified to the method is greater than zero.
     *
     * @param expiryTime the expiry time of any cached data, expressed as a number of milliseconds beyond the epoch. The
     *                   default expiry time is zero.
     *
     * @see System#currentTimeMillis() for a description of milliseconds beyond the epoch.
     */
    public void setExpiryTime(long expiryTime) // Override this method to use intrinsic level-specific expiry times
    {
        super.setExpiryTime(expiryTime);

        if (expiryTime > 0)
            this.levels.setExpiryTime(expiryTime); // remove this in sub-class to use level-specific expiry times
    }

    public double getMaxElevation()
    {
        return this.maxElevation;
    }

    public double getMinElevation()
    {
        return this.minElevation;
    }

    public double getBestResolution(Sector sector)
    {
        if (sector == null)
            return this.levels.getLastLevel().getTexelSize();

        Level level = this.levels.getLastLevel(sector);
        return level != null ? level.getTexelSize() : Double.MAX_VALUE;
    }

    public double getDetailHint(Sector sector)
    {
        return this.detailHint;
    }

    public void setDetailHint(double hint)
    {
        this.detailHint = hint;
    }

    public String getElevationDataPixelType()
    {
        return this.elevationDataPixelType;
    }

    public void setPixelType(String pixelType)
    {
        if (pixelType == null)
        {
            String message = Logging.getMessage("nullValue.PixelTypeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.elevationDataPixelType = pixelType;
    }

    public String getElevationDataByteOrder()
    {
        return this.elevationDataByteOrder;
    }

    public void setByteOrder(String byteOrder)
    {
        if (byteOrder == null)
        {
            String message = Logging.getMessage("nullValue.ByteOrderIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.elevationDataByteOrder = byteOrder;
    }

    public int intersects(Sector sector)
    {
        if (this.levels.getSector().contains(sector))
            return 0;

        return this.levels.getSector().intersects(sector) ? 1 : -1;
    }

    public boolean contains(Angle latitude, Angle longitude)
    {
        if (latitude == null || longitude == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return this.levels.getSector().contains(latitude, longitude);
    }

    //**************************************************************//
    //********************  Elevation Tile Management  *************//
    //**************************************************************//

    // Create the tile corresponding to a specified key.

    protected ElevationTile createTile(TileKey key)
    {
        Level level = this.levels.getLevel(key.getLevelNumber());

        // Compute the tile's SW lat/lon based on its row/col in the level's data set.
        Angle dLat = level.getTileDelta().getLatitude();
        Angle dLon = level.getTileDelta().getLongitude();
        Angle latOrigin = this.levels.getTileOrigin().getLatitude();
        Angle lonOrigin = this.levels.getTileOrigin().getLongitude();

        Angle minLatitude = ElevationTile.computeRowLatitude(key.getRow(), dLat, latOrigin);
        Angle minLongitude = ElevationTile.computeColumnLongitude(key.getColumn(), dLon, lonOrigin);

        Sector tileSector = new Sector(minLatitude, minLatitude.add(dLat), minLongitude, minLongitude.add(dLon));

        return new ElevationTile(tileSector, level, key.getRow(), key.getColumn());
    }

    // Thread off a task to determine whether the file is local or remote and then retrieve it either from the file
    // cache or a remote server.
    protected void requestTile(TileKey key)
    {
        if (WorldWind.getTaskService().isFull())
            return;

        if (this.getLevels().isResourceAbsent(key))
            return;

        RequestTask request = new RequestTask(key, this);
        WorldWind.getTaskService().addTask(request);
    }

    protected static class RequestTask implements Runnable
    {
        protected final BasicElevationModel elevationModel;
        protected final TileKey tileKey;

        protected RequestTask(TileKey tileKey, BasicElevationModel elevationModel)
        {
            this.elevationModel = elevationModel;
            this.tileKey = tileKey;
        }

        public final void run()
        {
            try
            {
                // check to ensure load is still needed
                if (this.elevationModel.areElevationsInMemory(this.tileKey))
                    return;

                ElevationTile tile = this.elevationModel.createTile(this.tileKey);
                final URL url = this.elevationModel.getDataFileStore().findFile(tile.getPath(), false);
                if (url != null && !this.elevationModel.isFileExpired(tile, url,
                    this.elevationModel.getDataFileStore()))
                {
                    if (this.elevationModel.loadElevations(tile, url))
                    {
                        this.elevationModel.levels.unmarkResourceAbsent(tile);
                        this.elevationModel.firePropertyChange(AVKey.ELEVATION_MODEL, null, this);
                        return;
                    }
                    else
                    {
                        // Assume that something's wrong with the file and delete it.
                        this.elevationModel.getDataFileStore().removeFile(url);
                        this.elevationModel.levels.markResourceAbsent(tile);
                        String message = Logging.getMessage("generic.DeletedCorruptDataFile", url);
                        Logging.logger().info(message);
                    }
                }

                this.elevationModel.downloadElevations(tile);
            }
            catch (IOException e)
            {
                String msg = Logging.getMessage("ElevationModel.ExceptionRequestingElevations",
                    this.tileKey.toString());
                Logging.logger().log(java.util.logging.Level.FINE, msg, e);
            }
        }

        public final boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            final RequestTask that = (RequestTask) o;

            //noinspection RedundantIfStatement
            if (this.tileKey != null ? !this.tileKey.equals(that.tileKey) : that.tileKey != null)
                return false;

            return true;
        }

        public final int hashCode()
        {
            return (this.tileKey != null ? this.tileKey.hashCode() : 0);
        }

        public final String toString()
        {
            return this.tileKey.toString();
        }
    }

    protected boolean isFileExpired(Tile tile, java.net.URL fileURL, FileStore fileStore)
    {
        if (!WWIO.isFileOutOfDate(fileURL, tile.getLevel().getExpiryTime()))
            return false;

        // The file has expired. Delete it.
        fileStore.removeFile(fileURL);
        String message = Logging.getMessage("generic.DataFileExpired", fileURL);
        Logging.logger().fine(message);
        return true;
    }

    // Reads a tile's elevations from the file cache and adds the tile to the memory cache.
    protected boolean loadElevations(ElevationTile tile, java.net.URL url) throws IOException
    {
        BufferWrapper elevations = this.readElevations(url);
        if (elevations == null || elevations.length() == 0)
            return false;

        if (this.numExpectedValues > 0 && elevations.length() != this.numExpectedValues)
            return false; // corrupt file

        tile.setElevations(elevations);
        this.addTileToCache(tile, elevations);

        return true;
    }

    protected void addTileToCache(ElevationTile tile, BufferWrapper elevations)
    {
        // Level 0 tiles are held in the model itself; other levels are placed in the memory cache.
        if (tile.getLevelNumber() == 0)
            this.levelZeroTiles.put(tile.getTileKey(), tile);
        else
            this.getMemoryCache().add(tile.getTileKey(), tile, elevations.getSizeInBytes());
    }

    protected boolean areElevationsInMemory(TileKey key)
    {
        // An elevation tile is considered to be in memory if it:
        // * Exists in the memory cache.
        // * Has non-null elevation data.
        // * Has not exipired.
        ElevationTile tile = this.getTileFromMemory(key);
        return (tile != null && tile.getElevations() != null && !tile.isElevationsExpired());
    }

    protected ElevationTile getTileFromMemory(TileKey tileKey)
    {
        if (tileKey.getLevelNumber() == 0)
            return this.levelZeroTiles.get(tileKey);
        else
            return (ElevationTile) this.getMemoryCache().getObject(tileKey);
    }

    // Read elevations from the file cache. Don't be confused by the use of a URL here: it's used so that files can
    // be read using System.getResource(URL), which will draw the data from a jar file in the classpath.
    protected BufferWrapper readElevations(URL url) throws IOException
    {
        try
        {
            ByteBuffer byteBuffer;
            synchronized (this.fileLock)
            {
                byteBuffer = WWIO.readURLContentToBuffer(url);
            }

            // Setup parameters to instruct BufferWrapper on how to interpret the ByteBuffer.
            AVList bufferParams = new AVListImpl();
            bufferParams.setValue(AVKey.DATA_TYPE, this.elevationDataPixelType);
            bufferParams.setValue(AVKey.BYTE_ORDER, this.elevationDataByteOrder);
            return BufferWrapper.wrap(byteBuffer, bufferParams);
        }
        catch (java.io.IOException e)
        {
            Logging.logger().log(java.util.logging.Level.SEVERE,
                "ElevationModel.ExceptionReadingElevationFile", url.toString());
            throw e;
        }
    }

    protected static ByteBuffer convertImageToElevations(ByteBuffer buffer, String contentType) throws IOException
    {
        File tempFile = File.createTempFile("wwj-", WWIO.makeSuffixForMimeType(contentType));
        try
        {
            WWIO.saveBuffer(buffer, tempFile);
            BufferedImage image = ImageIO.read(tempFile);
            ByteBuffer byteBuffer = BufferUtil.newByteBuffer(image.getWidth() * image.getHeight() * 2);
            byteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
            ShortBuffer bilBuffer = byteBuffer.asShortBuffer();

            WritableRaster raster = image.getRaster();
            int[] samples = new int[raster.getWidth() * raster.getHeight()];
            raster.getSamples(0, 0, raster.getWidth(), raster.getHeight(), 0, samples);
            for (int sample : samples)
            {
                bilBuffer.put((short) sample);
            }

            return byteBuffer;
        }
        finally
        {
            if (tempFile != null)
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
        }
    }

    // *** Bulk download ***
    // *** Bulk download ***
    // *** Bulk download ***

    /**
     * Start a new {@link BulkRetrievalThread} that downloads all elevations for a given sector and resolution to the
     * current World Wind file cache, without downloading imagery already in the cache.
     * <p/>
     * This method creates and starts a thread to perform the download. A reference to the thread is returned. To create
     * a downloader that has not been started, construct a {@link BasicElevationModelBulkDownloader}.
     * <p/>
     * Note that the target resolution must be provided in radians of latitude per texel, which is the resolution in
     * meters divided by the globe radius.
     *
     * @param sector     the sector to download data for.
     * @param resolution the target resolution, provided in radians of latitude per texel.
     *
     * @return the {@link BulkRetrievalThread} executing the retrieval.
     *
     * @throws IllegalArgumentException if the sector is null or the resolution is less than  zero.
     * @see BasicElevationModelBulkDownloader
     */
    public BulkRetrievalThread makeLocal(Sector sector, double resolution)
    {
        return this.makeLocal(sector, resolution, this.getDataFileStore());
    }

    /**
     * Start a new {@link BulkRetrievalThread} that downloads all elevations for a given sector and resolution to a
     * specified file store, without downloading imagery already in the file store.
     * <p/>
     * This method creates and starts a thread to perform the download. A reference to the thread is returned. To create
     * a downloader that has not been started, construct a {@link BasicElevationModelBulkDownloader}.
     * <p/>
     * Note that the target resolution must be provided in radians of latitude per texel, which is the resolution in
     * meters divided by the globe radius.
     *
     * @param sector     the sector to download data for.
     * @param resolution the target resolution, provided in radians of latitude per texel.
     * @param fileStore  the file store in which to place the downloaded elevations. If null the current World Wind file
     *                   cache is used.
     *
     * @return the {@link BulkRetrievalThread} executing the retrieval.
     *
     * @throws IllegalArgumentException if  the sector is null or the resolution is less than zero.
     * @see BasicElevationModelBulkDownloader
     */
    public BulkRetrievalThread makeLocal(Sector sector, double resolution, FileStore fileStore)
    {
        // Args checked in downloader constructor
        BasicElevationModelBulkDownloader thread =
            new BasicElevationModelBulkDownloader(this, sector, resolution, fileStore);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /**
     * Get the estimated size in bytes of the elevations not in the World Wind file cache for the given sector and
     * resolution.
     * <p/>
     * Note that the target resolution must be provided in radians of latitude per texel, which is the resolution in
     * meters divided by the globe radius.
     *
     * @param sector     the sector to estimate.
     * @param resolution the target resolution, provided in radians of latitude per texel.
     *
     * @return the estimated size in bytes of the missing elevations.
     *
     * @throws IllegalArgumentException if the sector is null or the resolution is less than zero.
     */
    public long getEstimatedMissingDataSize(Sector sector, double resolution)
    {
        return this.getEstimatedMissingDataSize(sector, resolution, this.getDataFileStore());
    }

    /**
     * Get the estimated size in bytes of the elevations not in a specified file store for the given sector and
     * resolution.
     * <p/>
     * Note that the target resolution must be provided in radians of latitude per texel, which is the resolution in
     * meters divided by the globe radius.
     *
     * @param sector     the sector to estimate.
     * @param resolution the target resolution, provided in radians of latitude per texel.
     * @param fileStore  the file store to examine. If null the current World Wind file cache is used.
     *
     * @return the estimated size in bytes of the missing elevations.
     *
     * @throws IllegalArgumentException if the sector is null or the resolution is less than zero.
     */
    public long getEstimatedMissingDataSize(Sector sector, double resolution, FileStore fileStore)
    {
        // Args checked by downloader constructor
        // Need a downloader to compute the missing data size.
        BasicElevationModelBulkDownloader downloader =
            new BasicElevationModelBulkDownloader(this, sector, resolution, fileStore);

        return downloader.getEstimatedMissingDataSize();
    }

    // *** Tile download ***
    // *** Tile download ***
    // *** Tile download ***

    protected void downloadElevations(final Tile tile)
    {
        downloadElevations(tile, null);
    }

    protected void downloadElevations(final Tile tile, DownloadPostProcessor postProcessor)
    {
        if (!this.isNetworkRetrievalEnabled())
        {
            this.getLevels().markResourceAbsent(tile);
            return;
        }

        if (!WorldWind.getRetrievalService().isAvailable())
            return;

        java.net.URL url = null;
        try
        {
            url = tile.getResourceURL();
            if (WorldWind.getNetworkStatus().isHostUnavailable(url))
            {
                this.getLevels().markResourceAbsent(tile);
                return;
            }
        }
        catch (java.net.MalformedURLException e)
        {
            Logging.logger().log(java.util.logging.Level.SEVERE,
                Logging.getMessage("TiledElevationModel.ExceptionCreatingElevationsUrl", url), e);
            return;
        }

        if (postProcessor == null)
            postProcessor = new DownloadPostProcessor(tile, this);
        URLRetriever retriever = new HTTPRetriever(url, postProcessor);
        if (WorldWind.getRetrievalService().contains(retriever))
            return;

        WorldWind.getRetrievalService().runRetriever(retriever, 0d);
    }

    protected static class DownloadPostProcessor implements RetrievalPostProcessor
    {
        protected Tile tile;
        protected BasicElevationModel elevationModel;
        protected final FileStore fileStore;

        public DownloadPostProcessor(Tile tile, BasicElevationModel elevationModel)
        {
            // don't validate - constructor is only available to classes with private access.
            this.tile = tile;
            this.elevationModel = elevationModel;
            this.fileStore = this.elevationModel.getDataFileStore();
        }

        public DownloadPostProcessor(Tile tile, BasicElevationModel elevationModel, FileStore fileStore)
        {
            // don't validate - constructor is only available to classes with protected access.
            this.tile = tile;
            this.elevationModel = elevationModel;
            this.fileStore = fileStore;
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
                {
                    if (retriever.getState().equals(Retriever.RETRIEVER_STATE_ERROR))
                        this.elevationModel.levels.markResourceAbsent(this.tile);

                    return null;
                }

                if (retriever instanceof HTTPRetriever)
                {
                    HTTPRetriever htr = (HTTPRetriever) retriever;
                    if (htr.getResponseCode() != HttpURLConnection.HTTP_OK)
                    {
                        // Mark tile as missing so avoid excessive attempts
                        this.elevationModel.levels.markResourceAbsent(this.tile);
                        return null;
                    }
                }

                URLRetriever r = (URLRetriever) retriever;
                ByteBuffer buffer = r.getBuffer();

                final File outFile = this.fileStore.newFile(tile.getPath());
                if (outFile == null)
                    return null;

                if (outFile.exists())
                    return buffer;

                if (buffer != null)
                {
                    String contentType = r.getContentType();

                    if (contentType.contains("xml") || contentType.contains("html") || contentType.contains("text"))
                    {
                        this.elevationModel.getLevels().markResourceAbsent(this.tile);

                        StringBuffer sb = new StringBuffer();
                        while (buffer.hasRemaining())
                        {
                            sb.append((char) buffer.get());
                        }
                        // TODO: parse out the message if the content is xml or html.
                        Logging.logger().severe(sb.toString());

                        return null;
                    }

                    else if (contentType.contains("image") && !contentType.equals("image/bil"))
                    {
                        // Convert to .bil and save the result
                        buffer = convertImageToElevations(buffer, contentType);
                    }

                    synchronized (elevationModel.fileLock)
                    {
                        WWIO.saveBuffer(buffer, outFile);
                    }

                    // We've successfully written data to the cache. Check if there's a configuration file for this
                    // layer in the cache, and create one if there isn't.
                    this.elevationModel.writeConfigurationFile(this.fileStore);

                    return buffer;
                }
            }
            catch (ClosedByInterruptException e)
            {
                Logging.logger().log(java.util.logging.Level.FINE,
                    Logging.getMessage("generic.OperationCancelled", "elevations retrieval"), e);
            }
            catch (java.io.IOException e)
            {
                this.elevationModel.getLevels().markResourceAbsent(this.tile);
                Logging.logger().log(java.util.logging.Level.SEVERE,
                    Logging.getMessage("TiledElevationModel.ExceptionSavingRetrievedElevationFile", tile.getPath()), e);
            }
            finally
            {
                this.elevationModel.firePropertyChange(AVKey.ELEVATION_MODEL, null, this);
            }

            return null;
        }
    }

    protected static class Elevations
    {
        protected final BasicElevationModel elevationModel;
        protected java.util.Set<ElevationTile> tiles;
        protected double extremes[] = null;
        protected final double achievedResolution;

        protected Elevations(BasicElevationModel elevationModel, double achievedResolution)
        {
            this.elevationModel = elevationModel;
            this.achievedResolution = achievedResolution;
        }

        public double getElevation(Angle latitude, Angle longitude)
        {
            if (latitude == null || longitude == null)
            {
                String msg = Logging.getMessage("nullValue.AngleIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }

            if (this.tiles == null)
                return this.elevationModel.getMissingDataSignal();

            try
            {
                for (ElevationTile tile : this.tiles)
                {
                    if (tile.getSector().contains(latitude, longitude))
                        return this.elevationModel.lookupElevation(latitude.radians, longitude.radians, tile);
                }

                return this.elevationModel.getMissingDataSignal();
            }
            catch (Exception e)
            {
                // Throwing an exception within what's likely to be the caller's geometry creation loop
                // would be hard to recover from, and a reasonable response to the exception can be done here.
                Logging.logger().log(java.util.logging.Level.SEVERE,
                    Logging.getMessage("BasicElevationModel.ExceptionComputingElevation", latitude, longitude), e);

                return this.elevationModel.getMissingDataSignal();
            }
        }

        public double[] getExtremes(Angle latitude, Angle longitude)
        {
            if (latitude == null || longitude == null)
            {
                String msg = Logging.getMessage("nullValue.AngleIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }

            if (this.extremes != null)
                return this.extremes;

            if (this.tiles == null || tiles.size() == 0)
                return this.elevationModel.getExtremeElevations(latitude, longitude);

            return this.getExtremes();
        }

        public double[] getExtremes()
        {
            if (this.extremes != null)
                return this.extremes;

            if (this.tiles == null || tiles.size() == 0)
                return this.extremes =
                    new double[] {this.elevationModel.getMinElevation(), this.elevationModel.getMaxElevation()};

            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;

            for (ElevationTile tile : this.tiles)
            {
                BufferWrapper elevations = tile.getElevations();

                int len = elevations.length();
                if (len == 0)
                    return null;

                for (int i = 0; i < len; i++)
                {
                    double h = elevations.getDouble(i);

                    if (h == this.elevationModel.getMissingDataSignal())
                        h = this.elevationModel.getMissingDataReplacement();

                    if (h > max)
                        max = h;
                    if (h < min)
                        min = h;
                }
            }

            if (min == Double.MAX_VALUE)
                min = this.elevationModel.getMinElevation();
            if (max == -Double.MAX_VALUE)
                max = this.elevationModel.getMaxElevation();

            return this.extremes = new double[] {min, max};
        }
    }

    public double getUnmappedElevation(Angle latitude, Angle longitude)
    {
        if (latitude == null || longitude == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Level lastLevel = this.levels.getLastLevel(latitude, longitude);
        final TileKey tileKey = new TileKey(latitude, longitude, this.levels, lastLevel.getLevelNumber());
        ElevationTile tile = this.getTileFromMemory(tileKey);

        if (tile == null)
        {
            int fallbackRow = tileKey.getRow();
            int fallbackCol = tileKey.getColumn();
            for (int fallbackLevelNum = tileKey.getLevelNumber() - 1; fallbackLevelNum >= 0; fallbackLevelNum--)
            {
                fallbackRow /= 2;
                fallbackCol /= 2;

                if (this.levels.getLevel(fallbackLevelNum).isEmpty()) // everything lower res is empty
                    return this.getMissingDataSignal();

                TileKey fallbackKey = new TileKey(fallbackLevelNum, fallbackRow, fallbackCol,
                    this.levels.getLevel(fallbackLevelNum).getCacheName());
                tile = this.getTileFromMemory(fallbackKey);
                if (tile != null)
                    break;
            }
        }

        if (tile == null && !this.levels.getFirstLevel().isEmpty())
        {
            Level firstLevel = this.levels.getFirstLevel();
            final TileKey zeroKey = new TileKey(latitude, longitude, this.levels, firstLevel.getLevelNumber());
            this.requestTile(zeroKey);

            return this.getMissingDataSignal();
        }

        // Check tile expiration. Memory-cached tiles are checked for expiration only when an explicit, non-zero expiry
        // time has been set for the elevation model. If none has been set, the expiry times of the model's individual
        // levels are used, but only for tiles in the local file cache, not tiles in memory. This is to avoid incurring
        // the overhead of checking expiration of in-memory tiles, a very rarely used feature.
        if (this.getExpiryTime() > 0 && this.getExpiryTime() < System.currentTimeMillis())
        {
            // Normally getUnmappedElevations() does not request elevation tiles, except for first level tiles. However
            // if the tile is already in memory but has expired, we must issue a request to replace the tile data. This
            // will not fetch new tiles into the cache, but rather will force a refresh of the expired tile's resources
            // in the file cache and the memory cache.
            if (tile != null)
                this.checkElevationExpiration(tile);
        }

        return this.lookupElevation(latitude.radians, longitude.radians, tile);
    }

    public double getElevations(Sector sector, List<? extends LatLon> latlons, double targetResolution, double[] buffer)
    {
        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (latlons == null)
        {
            String msg = Logging.getMessage("nullValue.LatLonListIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (buffer == null)
        {
            String msg = Logging.getMessage("nullValue.ElevationsBufferIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (buffer.length < latlons.size())
        {
            String msg = Logging.getMessage("ElevationModel.ElevationsBufferTooSmall", latlons.size());
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Level targetLevel = this.getTargetLevel(sector, targetResolution);
        if (targetLevel == null)
            return 0;

        Elevations elevations = this.getElevations(sector, this.levels, targetLevel.getLevelNumber());
        if (elevations == null)
            return 0;

        for (int i = 0; i < latlons.size(); i++)
        {
            LatLon ll = latlons.get(i);
            if (ll == null)
                continue;

            double value = elevations.getElevation(ll.getLatitude(), ll.getLongitude());

            // If an elevation at the given location is available, then write that elevation to the destination buffer.
            // Otherwise do nothing.
            if (value != this.getMissingDataSignal())
                buffer[i] = value;
        }

        return elevations.achievedResolution;
    }

    protected Level getTargetLevel(Sector sector, double targetSize)
    {
        Level lastLevel = this.levels.getLastLevel(sector); // finest resolution available
        if (lastLevel == null)
            return null;

        if (lastLevel.getTexelSize() >= targetSize)
            return lastLevel; // can't do any better than this

        for (Level level : this.levels.getLevels())
        {
            if (level.getTexelSize() <= targetSize)
                return !level.isEmpty() ? level : null;

            if (level == lastLevel)
                break;
        }

        return lastLevel;
    }

    protected double lookupElevation(final double latRadians, final double lonRadians, final ElevationTile tile)
    {
        BufferWrapper elevations = tile.getElevations();
        Sector sector = tile.getSector();
        final int tileHeight = tile.getHeight();
        final int tileWidth = tile.getWidth();
        final double sectorDeltaLat = sector.getDeltaLat().radians;
        final double sectorDeltaLon = sector.getDeltaLon().radians;
        final double dLat = sector.getMaxLatitude().radians - latRadians;
        final double dLon = lonRadians - sector.getMinLongitude().radians;
        final double sLat = dLat / sectorDeltaLat;
        final double sLon = dLon / sectorDeltaLon;

        int j = (int) ((tileHeight - 1) * sLat);
        int i = (int) ((tileWidth - 1) * sLon);
        int k = j * tileWidth + i;

        double eLeft = elevations.getDouble(k);
        double eRight = i < (tileWidth - 1) ? elevations.getDouble(k + 1) : eLeft;

        if (this.getMissingDataSignal() == eLeft || this.getMissingDataSignal() == eRight)
            return this.getMissingDataSignal();

        double dw = sectorDeltaLon / (tileWidth - 1);
        double dh = sectorDeltaLat / (tileHeight - 1);
        double ssLon = (dLon - i * dw) / dw;
        double ssLat = (dLat - j * dh) / dh;

        double eTop = eLeft + ssLon * (eRight - eLeft);

        if (j < tileHeight - 1 && i < tileWidth - 1)
        {
            eLeft = elevations.getDouble(k + tileWidth);
            eRight = elevations.getDouble(k + tileWidth + 1);

            if (this.getMissingDataSignal() == eLeft || this.getMissingDataSignal() == eRight)
                return this.getMissingDataSignal();
        }

        double eBot = eLeft + ssLon * (eRight - eLeft);
        return eTop + ssLat * (eBot - eTop);
    }

    public double[] getExtremeElevations(Angle latitude, Angle longitude)
    {
        if (latitude == null || longitude == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (this.extremesLevel < 0 || this.extremes == null)
            return new double[] {this.getMinElevation(), this.getMaxElevation()};

        try
        {
            LatLon delta = this.levels.getLevel(this.extremesLevel).getTileDelta();
            LatLon origin = this.levels.getTileOrigin();
            final int row = ElevationTile.computeRow(delta.getLatitude(), latitude, origin.getLatitude());
            final int col = ElevationTile.computeColumn(delta.getLongitude(), longitude, origin.getLongitude());

            final int nCols = ElevationTile.computeColumn(delta.getLongitude(), Angle.POS180, Angle.NEG180) + 1;

            int index = 2 * (row * nCols + col);
            double min = this.extremes.getDouble(index);
            double max = this.extremes.getDouble(index + 1);

            if (min == this.getMissingDataSignal())
                min = this.getMissingDataReplacement();
            if (max == this.getMissingDataSignal())
                max = this.getMissingDataReplacement();

            return new double[] {min, max};
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("BasicElevationModel.ExceptionDeterminingExtremes",
                new LatLon(latitude, longitude));
            Logging.logger().log(java.util.logging.Level.WARNING, message, e);

            return new double[] {this.getMinElevation(), this.getMaxElevation()};
        }
    }

    public double[] getExtremeElevations(Sector sector)
    {
        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.extremesLevel < 0 || this.extremes == null)
            return new double[] {this.getMinElevation(), this.getMaxElevation()};

        try
        {
            double[] extremes = (double[]) this.getExtremesLookupCache().getObject(sector);
            if (extremes == null)
            {
                extremes = this.computeExtremeElevations(sector);
                long sizeInBytes = (Double.SIZE / 8) * extremes.length;
                this.getExtremesLookupCache().add(sector, extremes, sizeInBytes);
            }

            // Return a defensive copy of the array to prevent the caller from modifying the cache contents.
            return new double[] {extremes[0], extremes[1]};
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("BasicElevationModel.ExceptionDeterminingExtremes", sector);
            Logging.logger().log(java.util.logging.Level.WARNING, message, e);

            return new double[] {this.getMinElevation(), this.getMaxElevation()};
        }
    }

    public void loadExtremeElevations(String extremesFileName)
    {
        if (extremesFileName == null)
        {
            String message = Logging.getMessage("nullValue.ExtremeElevationsFileName");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        InputStream is = null;
        try
        {
            is = this.getClass().getResourceAsStream("/" + extremesFileName);
            if (is == null)
            {
                // Look directly in the file system
                File file = new File(extremesFileName);
                if (file.exists())
                    is = new FileInputStream(file);
                else
                    Logging.logger().log(java.util.logging.Level.WARNING, "BasicElevationModel.UnavailableExtremesFile",
                        extremesFileName);
            }

            if (is == null)
                return;

            // The level the extremes were taken from is encoded as the last element in the file name
            String[] tokens = extremesFileName.substring(0, extremesFileName.lastIndexOf(".")).split("_");
            this.extremesLevel = Integer.parseInt(tokens[tokens.length - 1]);
            if (this.extremesLevel < 0)
            {
                this.extremes = null;
                Logging.logger().log(java.util.logging.Level.WARNING, "BasicElevationModel.UnavailableExtremesLevel",
                    extremesFileName);
                return;
            }

            AVList bufferParams = new AVListImpl();
            bufferParams.setValue(AVKey.DATA_TYPE, AVKey.INT16);
            bufferParams.setValue(AVKey.BYTE_ORDER, AVKey.BIG_ENDIAN); // Extremes are always saved in JVM byte order
            this.extremes = BufferWrapper.wrap(WWIO.readStreamToBuffer(is, true),
                bufferParams); // Read extremes to a direct ByteBuffer.
        }
        catch (FileNotFoundException e)
        {
            Logging.logger().log(java.util.logging.Level.WARNING,
                Logging.getMessage("BasicElevationModel.ExceptionReadingExtremeElevations", extremesFileName), e);
            this.extremes = null;
            this.extremesLevel = -1;
            this.extremesLookupCache = null;
        }
        catch (IOException e)
        {
            Logging.logger().log(java.util.logging.Level.WARNING,
                Logging.getMessage("BasicElevationModel.ExceptionReadingExtremeElevations", extremesFileName), e);
            this.extremes = null;
            this.extremesLevel = -1;
            this.extremesLookupCache = null;
        }
        finally
        {
            WWIO.closeStream(is, extremesFileName);

            // Clear the extreme elevations lookup cache.
            if (this.extremesLookupCache != null)
                this.extremesLookupCache.clear();
        }
    }

    protected double[] computeExtremeElevations(Sector sector)
    {
        LatLon delta = this.levels.getLevel(this.extremesLevel).getTileDelta();
        LatLon origin = this.levels.getTileOrigin();
        final int nwRow = ElevationTile.computeRow(delta.getLatitude(), sector.getMaxLatitude(),
            origin.getLatitude());
        final int nwCol = ElevationTile.computeColumn(delta.getLongitude(), sector.getMinLongitude(),
            origin.getLongitude());
        final int seRow = ElevationTile.computeRow(delta.getLatitude(), sector.getMinLatitude(),
            origin.getLatitude());
        final int seCol = ElevationTile.computeColumn(delta.getLongitude(), sector.getMaxLongitude(),
            origin.getLongitude());

        final int nCols = ElevationTile.computeColumn(delta.getLongitude(), Angle.POS180, Angle.NEG180) + 1;

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        for (int col = nwCol; col <= seCol; col++)
        {
            for (int row = seRow; row <= nwRow; row++)
            {
                int index = 2 * (row * nCols + col);
                double a = this.extremes.getDouble(index);
                double b = this.extremes.getDouble(index + 1);

                if (a == this.getMissingDataSignal())
                    a = this.getMissingDataReplacement();
                if (b == this.getMissingDataSignal())
                    b = this.getMissingDataReplacement();

                if (a > max)
                    max = a;
                if (a < min)
                    min = a;
                if (b > max)
                    max = b;
                if (b < min)
                    min = b;
            }
        }

        // Set to model's limits if for some reason a limit wasn't determined
        if (min == Double.MAX_VALUE)
            min = this.getMinElevation();
        if (max == Double.MAX_VALUE)
            max = this.getMaxElevation();

        return new double[] {min, max};
    }

    /**
     * Returns the memory cache used to cache extreme elevation computations, initializing the cache if it doesn't yet
     * exist. This is an instance level cache: each instance of BasicElevationModel has its own instance of an extreme
     * elevations lookup cache.
     *
     * @return the memory cache associated with the extreme elevations computations.
     */
    protected synchronized MemoryCache getExtremesLookupCache()
    {
        // Note that the extremes lookup cache does not belong to the WorldWind memory cache set, therefore it will not
        // be automatically cleared and disposed when World Wind is shutdown. However, since the extremes lookup cache
        // is a local reference to this elevation model, it will be reclaimed by the JVM garbage collector when this
        // elevation model is reclaimed by the GC.

        if (this.extremesLookupCache == null)
        {
            // Default cache size holds 1250 min/max pairs. This size was experimentally determined to hold enough
            // value lookups to prevent cache thrashing.
            long size = Configuration.getLongValue(AVKey.ELEVATION_EXTREMES_LOOKUP_CACHE_SIZE, 20000L);
            this.extremesLookupCache = new BasicMemoryCache((long) (0.85 * size), size);
        }

        return this.extremesLookupCache;
    }

    protected static class ElevationTile extends gov.nasa.worldwind.util.Tile implements Cacheable
    {
        private BufferWrapper elevations; // the elevations themselves
        private long updateTime = 0;

        protected ElevationTile(Sector sector, Level level, int row, int col)
        {
            super(sector, level, row, col);
        }

        public BufferWrapper getElevations()
        {
            return this.elevations;
        }

        public void setElevations(BufferWrapper elevations)
        {
            this.elevations = elevations;
            this.updateTime = System.currentTimeMillis();
        }

        public boolean isElevationsExpired()
        {
            return this.isElevationsExpired(this.getLevel().getExpiryTime());
        }

        public boolean isElevationsExpired(long expiryTime)
        {
            return this.updateTime > 0 && this.updateTime < expiryTime;
        }
    }

    protected Elevations getElevations(Sector sector, LevelSet levelSet, int targetLevelNumber)
    {
        this.checkResources(); // Check whether resources are present, and perform any necessary initialization.

        // Compute the intersection of the requested sector with the LevelSet's sector.
        // The intersection will be used to determine which Tiles in the LevelSet are in the requested sector.
        sector = sector.intersection(levelSet.getSector());

        Level targetLevel = levelSet.getLevel(targetLevelNumber);
        LatLon delta = targetLevel.getTileDelta();
        LatLon origin = levelSet.getTileOrigin();
        final int nwRow = Tile.computeRow(delta.getLatitude(), sector.getMaxLatitude(), origin.getLatitude());
        final int nwCol = Tile.computeColumn(delta.getLongitude(), sector.getMinLongitude(), origin.getLongitude());
        final int seRow = Tile.computeRow(delta.getLatitude(), sector.getMinLatitude(), origin.getLatitude());
        final int seCol = Tile.computeColumn(delta.getLongitude(), sector.getMaxLongitude(), origin.getLongitude());

        java.util.TreeSet<ElevationTile> tiles = new java.util.TreeSet<ElevationTile>(new Comparator<ElevationTile>()
        {
            public int compare(ElevationTile t1, ElevationTile t2)
            {
                if (t2.getLevelNumber() == t1.getLevelNumber()
                    && t2.getRow() == t1.getRow() && t2.getColumn() == t1.getColumn())
                    return 0;

                // Higher-res levels compare lower than lower-res
                return t1.getLevelNumber() > t2.getLevelNumber() ? -1 : 1;
            }
        });
        ArrayList<TileKey> requested = new ArrayList<TileKey>();

        boolean missingTargetTiles = false;
        boolean missingLevelZeroTiles = false;
        for (int row = seRow; row <= nwRow; row++)
        {
            for (int col = nwCol; col <= seCol; col++)
            {
                TileKey key = new TileKey(targetLevel.getLevelNumber(), row, col, targetLevel.getCacheName());
                ElevationTile tile = this.getTileFromMemory(key);
                if (tile != null)
                {
                    tiles.add(tile);
                    continue;
                }

                missingTargetTiles = true;
                this.requestTile(key);

                // Determine the fallback to use. Simultaneously determine a fallback to request that is
                // the next resolution higher than the fallback chosen, if any. This will progressively
                // refine the display until the desired resolution tile arrives.
                TileKey fallbackToRequest = null;
                TileKey fallbackKey;
                int fallbackRow = row;
                int fallbackCol = col;
                for (int fallbackLevelNum = key.getLevelNumber() - 1; fallbackLevelNum >= 0; fallbackLevelNum--)
                {
                    fallbackRow /= 2;
                    fallbackCol /= 2;
                    fallbackKey = new TileKey(fallbackLevelNum, fallbackRow, fallbackCol,
                        this.levels.getLevel(fallbackLevelNum).getCacheName());

                    tile = this.getTileFromMemory(fallbackKey);
                    if (tile != null)
                    {
                        if (!tiles.contains(tile))
                        {
                            tiles.add(tile);
                        }
                        break;
                    }
                    else
                    {
                        if (fallbackLevelNum == 0)
                            missingLevelZeroTiles = true;
                        fallbackToRequest = fallbackKey; // keep track of lowest level to request
                    }
                }

                if (fallbackToRequest != null)
                {
                    if (!requested.contains(fallbackToRequest))
                    {
                        this.requestTile(fallbackToRequest);
                        requested.add(fallbackToRequest); // keep track to avoid overhead of duplicte requests
                    }
                }
            }
        }

        Elevations elevations;

        if (missingLevelZeroTiles || tiles.isEmpty())
        {
            // Double.MAX_VALUE is a signal for no in-memory tile for a given region of the sector.
            elevations = new Elevations(this, Double.MAX_VALUE);
        }
        else if (missingTargetTiles)
        {
            // Use the level of the the lowest resolution found to denote the resolution of this elevation set.
            // The list of tiles is sorted first by level, so use the level of the list's last entry.
            elevations = new Elevations(this, tiles.last().getLevel().getTexelSize());
        }
        else
        {
            elevations = new Elevations(this, tiles.last().getLevel().getTexelSize());
        }

        elevations.tiles = tiles;

        // Check tile expiration. Memory-cached tiles are checked for expiration only when an explicit, non-zero expiry
        // time has been set for the elevation model. If none has been set, the expiry times of the model's individual
        // levels are used, but only for tiles in the local file cache, not tiles in memory. This is to avoid incurring
        // the overhead of checking expiration of in-memory tiles, a very rarely used feature.
        if (this.getExpiryTime() > 0 && this.getExpiryTime() < System.currentTimeMillis())
            this.checkElevationExpiration(tiles);

        return elevations;
    }

    private void checkElevationExpiration(ElevationTile tile)
    {
        if (tile.isElevationsExpired())
            this.requestTile(tile.getTileKey());
    }

    private void checkElevationExpiration(Iterable<? extends ElevationTile> tiles)
    {
        for (ElevationTile tile : tiles)
        {
            if (tile.isElevationsExpired())
                this.requestTile(tile.getTileKey());
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public ByteBuffer generateExtremeElevations(int levelNumber)
    {
        return null;
//        long waitTime = 1000;
//        long timeout = 10 * 60 * 1000;
//
//        ElevationModel.Elevations elevs;
//        BasicElevationModel em = new EarthElevationModel();
//
//        double delta = 20d / Math.pow(2, levelNumber);
//
//        int numLats = (int) Math.ceil(180 / delta);
//        int numLons = (int) Math.ceil(360 / delta);
//
//        System.out.printf("Building extreme elevations for layer %d, num lats %d, num lons %d\n",
//            levelNumber, numLats, numLons);
//
//        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(2 * 2 * numLats * numLons);
//        ShortBuffer buffer = byteBuffer.asShortBuffer();
//        buffer.rewind();
//
//        Level level = this.levels.getLevel(levelNumber);
//        for (int j = 0; j < numLats; j++)
//        {
//            double lat = -90 + j * delta;
//            for (int i = 0; i < numLons; i++)
//            {
//                double lon = -180 + i * delta;
//                Sector s = Sector.fromDegrees(lat, lat + delta, lon, lon + delta);
//                long startTime = System.currentTimeMillis();
//                while ((elevs = em.getElevations(s, level)) == null)
//                {
//                    try
//                    {
//                        Thread.sleep(waitTime);
//                    }
//                    catch (InterruptedException e)
//                    {
//                        e.printStackTrace();
//                    }
//                    if (System.currentTimeMillis() - startTime >= timeout)
//                        break;
//                }
//
//                if (elevs == null)
//                {
//                    System.out.printf("null elevations for (%f, %f) %s\n", lat, lon, s);
//                    continue;
//                }
//
//
//                double[] extremes = elevs.getExtremes();
//                if (extremes != null)
//                {
//                    System.out.printf("%d:%d, (%f, %f) min = %f, max = %f\n", j, i, lat, lon, extremes[0], extremes[1]);
//                    buffer.put((short) extremes[0]).put((short) extremes[1]);
//                }
//                else
//                    System.out.printf("no extremes for (%f, %f)\n", lat, lon);
//            }
//        }
//
//        return (ByteBuffer) buffer.rewind();
    }
//
//    public final int getTileCount(Sector sector, int resolution)
//    {
//        if (sector == null)
//        {
//            String msg = Logging.getMessage("nullValue.SectorIsNull");
//            Logging.logger().severe(msg);
//            throw new IllegalArgumentException(msg);
//        }
//
//        // Collect all the elevation tiles intersecting the input sector. If a desired tile is not curently
//        // available, choose its next lowest resolution parent that is available.
//        final Level targetLevel = this.levels.getLevel(resolution);
//
//        LatLon delta = this.levels.getLevel(resolution).getTileDelta();
//        final int nwRow = Tile.computeRow(delta.getLatitude(), sector.getMaxLatitude());
//        final int nwCol = Tile.computeColumn(delta.getLongitude(), sector.getMinLongitude());
//        final int seRow = Tile.computeRow(delta.getLatitude(), sector.getMinLatitude());
//        final int seCol = Tile.computeColumn(delta.getLongitude(), sector.getMaxLongitude());
//
//        return (1 + (nwRow - seRow) * (1 + seCol - nwCol));
//    }

    //**************************************************************//
    //********************  Resources  *****************************//
    //**************************************************************//

    protected void checkResources()
    {
        AVList params = (AVList) this.getValue(AVKey.CONSTRUCTION_PARAMETERS);
        if (params == null)
            return;

        this.initializeResources(params);
    }

    protected void initializeResources(AVList params)
    {
        Boolean b = (Boolean) params.getValue(AVKey.RETRIEVE_PROPERTIES_FROM_SERVICE);
        if (b != null && b)
            this.initPropertiesFromService(params);
    }

    protected void initPropertiesFromService(AVList params)
    {
        if (this.serviceInitialized)
            return;

        URL url = DataConfigurationUtils.getOGCGetCapabilitiesURL(params);
        if (url == null)
            return;

        if (this.absentResources.isResourceAbsent(SERVICE_CAPABILITIES_RESOURCE_ID))
            return;

        Capabilities caps = SessionCacheUtils.getOrRetrieveSessionCapabilities(url, WorldWind.getRetrievalService(),
            WorldWind.getSessionCache(), url, this.absentResources, SERVICE_CAPABILITIES_RESOURCE_ID, this,
            AVKey.ELEVATION_MODEL);
        if (caps == null)
            return;

        this.initPropertiesFromCapabilities(caps, params);
        this.serviceInitialized = true;
    }

    protected void initPropertiesFromCapabilities(Capabilities caps, AVList params)
    {
        String[] names = DataConfigurationUtils.getOGCLayerNames(params);
        if (names == null || names.length == 0)
            return;

        Long expiryTime = caps.getLayerLatestLastUpdateTime(caps, names);
        if (expiryTime != null)
            this.setExpiryTime(expiryTime);
    }

    //**************************************************************//
    //********************  Configuration File  ********************//
    //**************************************************************//

    protected void writeConfigurationFile(FileStore fileStore)
    {
        // TODO: configurable max attempts for creating a configuration file.

        try
        {
            AVList configParams = this.getConfigurationParams(null);
            this.writeConfigurationParams(configParams, fileStore);
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("generic.ExceptionAttemptingToWriteConfigurationFile");
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
        }
    }

    protected void writeConfigurationParams(AVList params, FileStore fileStore)
    {
        // Determine what the configuration file name should be based on the configuration parameters. Assume an XML
        // configuration document type, and append the XML file suffix.
        String fileName = DataConfigurationUtils.getDataConfigFilename(params, ".xml");
        if (fileName == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.logger().severe(message);
            throw new WWRuntimeException(message);
        }

        // Check if this component needs to write a configuration file. This happens outside of the synchronized block
        // to improve multithreaded performance for the common case: the configuration file already exists, this just
        // need to check that it's there and return. If the file exists but is expired, do not remove it -  this
        // removes the file inside the synchronized block below.
        if (!this.needsConfigurationFile(fileStore, fileName, params, false))
            return;

        synchronized (this.fileLock)
        {
            // Check again if the component needs to write a configuration file, potentially removing any existing file
            // which has expired. This additional check is necessary because the file could have been created by
            // another thread while we were waiting for the lock.
            if (!this.needsConfigurationFile(fileStore, fileName, params, true))
                return;

            this.doWriteConfigurationParams(fileStore, fileName, params);
        }
    }

    protected void doWriteConfigurationParams(FileStore fileStore, String fileName, AVList params)
    {
        Document doc = this.createConfigurationDocument(params);

        DataConfigurationUtils.saveDataConfigDocument(doc, fileStore, fileName);
    }

    protected boolean needsConfigurationFile(FileStore fileStore, String fileName, AVList params,
        boolean removeIfExpired)
    {
        long expiryTime = this.getExpiryTime();
        if (expiryTime <= 0)
            expiryTime = AVListImpl.getLongValue(params, AVKey.EXPIRY_TIME, 0L);

        return !DataConfigurationUtils.hasDataConfigFile(fileStore, fileName, removeIfExpired, expiryTime);
    }

    protected AVList getConfigurationParams(AVList params)
    {
        if (params == null)
            params = new AVListImpl();

        // Gather all the construction parameters if they are available.
        AVList constructionParams = (AVList) this.getValue(AVKey.CONSTRUCTION_PARAMETERS);
        if (constructionParams != null)
            params.setValues(constructionParams);

        // Gather any missing LevelSet parameters from the LevelSet itself.
        DataConfigurationUtils.getLevelSetParams(this.getLevels(), params);

        // Gather any missing parameters about the elevation data. These values must be available for consumers of the
        // model configuration to property interpret the cached elevation files. While the elevation model assumes
        // default values when these properties are missing, a different system does not know what those default values
        // should be, and thus cannot assume anything about the value of these properties.

        if (params.getValue(AVKey.BYTE_ORDER) == null)
            params.setValue(AVKey.BYTE_ORDER, this.getElevationDataByteOrder());

        if (params.getValue(AVKey.PIXEL_TYPE) == null)
            params.setValue(AVKey.PIXEL_TYPE, this.getElevationDataPixelType());

        if (params.getValue(AVKey.MISSING_DATA_SIGNAL) == null)
            params.setValue(AVKey.MISSING_DATA_SIGNAL, this.getMissingDataSignal());

        return params;
    }

    protected Document createConfigurationDocument(AVList params)
    {
        return ElevationModelConfiguration.createBasicElevationModelDocument(params);
    }

    //**************************************************************//
    //********************  Restorable Support  ********************//
    //**************************************************************//

    public String getRestorableState()
    {
        // We only create a restorable state XML if this elevation model was constructed with an AVList.
        AVList constructionParams = (AVList) this.getValue(AVKey.CONSTRUCTION_PARAMETERS);
        if (constructionParams == null)
            return null;

        RestorableSupport rs = RestorableSupport.newRestorableSupport();
        // Creating a new RestorableSupport failed. RestorableSupport logged the problem, so just return null.
        if (rs == null)
            return null;

        this.doGetRestorableState(rs, null);
        return rs.getStateAsXml();
    }

    public void restoreState(String stateInXml)
    {
        String message = Logging.getMessage("RestorableSupport.RestoreRequiresConstructor");
        Logging.logger().severe(message);
        throw new UnsupportedOperationException(message);
    }

    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        AVList constructionParams = (AVList) this.getValue(AVKey.CONSTRUCTION_PARAMETERS);
        if (constructionParams != null)
        {
            for (Map.Entry<String, Object> avp : constructionParams.getEntries())
            {
                this.doGetRestorableStateForAVPair(avp.getKey(), avp.getValue(), rs, context);
            }
        }

        rs.addStateValueAsString(context, "ElevationModel.Name", this.getName());
        rs.addStateValueAsDouble(context, "ElevationModel.MissingDataFlag", this.getMissingDataSignal());
        rs.addStateValueAsDouble(context, "ElevationModel.MissingDataValue", this.getMissingDataReplacement());
        rs.addStateValueAsBoolean(context, "ElevationModel.NetworkRetrievalEnabled", this.isNetworkRetrievalEnabled());
        rs.addStateValueAsDouble(context, "ElevationModel.MinElevation", this.getMinElevation());
        rs.addStateValueAsDouble(context, "ElevationModel.MaxElevation", this.getMaxElevation());
        rs.addStateValueAsString(context, "BasicElevationModel.DataPixelType", this.getElevationDataPixelType());
        rs.addStateValueAsString(context, "BasicElevationModel.DataByteOrder", this.getElevationDataByteOrder());

        // We'll write the detail hint attribute only when it's a nonzero value.
        if (this.detailHint != 0.0)
            rs.addStateValueAsDouble(context, "BasicElevationModel.DetailHint", this.detailHint);

        RestorableSupport.StateObject so = rs.addStateObject(context, "avlist");
        for (Map.Entry<String, Object> avp : this.getEntries())
        {
            this.doGetRestorableStateForAVPair(avp.getKey(), avp.getValue(), rs, so);
        }
    }

    protected void doGetRestorableStateForAVPair(String key, Object value,
        RestorableSupport rs, RestorableSupport.StateObject context)
    {
        if (value == null)
            return;

        if (key.equals(AVKey.CONSTRUCTION_PARAMETERS))
            return;

        if (value instanceof LatLon)
        {
            rs.addStateValueAsLatLon(context, key, (LatLon) value);
        }
        else if (value instanceof Sector)
        {
            rs.addStateValueAsSector(context, key, (Sector) value);
        }
        else
        {
            rs.addStateValueAsString(context, key, value.toString());
        }
    }

    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        String s = rs.getStateValueAsString(context, "ElevationModel.Name");
        if (s != null)
            this.setName(s);

        Double d = rs.getStateValueAsDouble(context, "ElevationModel.MissingDataFlag");
        if (d != null)
            this.setMissingDataSignal(d);

        d = rs.getStateValueAsDouble(context, "ElevationModel.MissingDataValue");
        if (d != null)
            this.setMissingDataReplacement(d);

        Boolean b = rs.getStateValueAsBoolean(context, "ElevationModel.NetworkRetrievalEnabled");
        if (b != null)
            this.setNetworkRetrievalEnabled(b);

        s = rs.getStateValueAsString(context, "BasicElevationModel.DataPixelType");
        if (s != null)
            this.setPixelType(s);

        s = rs.getStateValueAsString(context, "BasicElevationModel.DataByteOrder");
        if (s != null)
            this.setByteOrder(s);

        d = rs.getStateValueAsDouble(context, "BasicElevationModel.DetailHint");
        if (d != null)
            this.setDetailHint(d);

        // Intentionally omitting "ElevationModel.MinElevation" and "ElevationModel.MaxElevation" because they are final
        // properties only configurable at construction.

        RestorableSupport.StateObject so = rs.getStateObject(context, "avlist");
        if (so != null)
        {
            RestorableSupport.StateObject[] avpairs = rs.getAllStateObjects(so, "");
            if (avpairs != null)
            {
                for (RestorableSupport.StateObject avp : avpairs)
                {
                    if (avp != null)
                        this.doRestoreStateForObject(rs, avp);
                }
            }
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void doRestoreStateForObject(RestorableSupport rs, RestorableSupport.StateObject so)
    {
        if (so == null)
            return;

        this.setValue(so.getName(), so.getValue());
    }

    protected static AVList restorableStateToParams(String stateInXml)
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
        restoreStateForParams(rs, null, params);
        return params;
    }

    protected static void restoreStateForParams(RestorableSupport rs, RestorableSupport.StateObject context,
        AVList params)
    {
        StringBuilder sb = new StringBuilder();

        String s = rs.getStateValueAsString(context, AVKey.DATA_CACHE_NAME);
        if (s != null)
            params.setValue(AVKey.DATA_CACHE_NAME, s);

        s = rs.getStateValueAsString(context, AVKey.SERVICE);
        if (s != null)
            params.setValue(AVKey.SERVICE, s);

        s = rs.getStateValueAsString(context, AVKey.DATASET_NAME);
        if (s != null)
            params.setValue(AVKey.DATASET_NAME, s);

        s = rs.getStateValueAsString(context, AVKey.FORMAT_SUFFIX);
        if (s != null)
            params.setValue(AVKey.FORMAT_SUFFIX, s);

        Integer i = rs.getStateValueAsInteger(context, AVKey.NUM_EMPTY_LEVELS);
        if (i != null)
            params.setValue(AVKey.NUM_EMPTY_LEVELS, i);

        i = rs.getStateValueAsInteger(context, AVKey.NUM_LEVELS);
        if (i != null)
            params.setValue(AVKey.NUM_LEVELS, i);

        i = rs.getStateValueAsInteger(context, AVKey.TILE_WIDTH);
        if (i != null)
            params.setValue(AVKey.TILE_WIDTH, i);

        i = rs.getStateValueAsInteger(context, AVKey.TILE_HEIGHT);
        if (i != null)
            params.setValue(AVKey.TILE_HEIGHT, i);

        Long lo = rs.getStateValueAsLong(context, AVKey.EXPIRY_TIME);
        if (lo != null)
            params.setValue(AVKey.EXPIRY_TIME, lo);

        LatLon ll = rs.getStateValueAsLatLon(context, AVKey.LEVEL_ZERO_TILE_DELTA);
        if (ll != null)
            params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, ll);

        ll = rs.getStateValueAsLatLon(context, AVKey.TILE_ORIGIN);
        if (ll != null)
            params.setValue(AVKey.TILE_ORIGIN, ll);

        Sector sector = rs.getStateValueAsSector(context, AVKey.SECTOR);
        if (sector != null)
            params.setValue(AVKey.SECTOR, sector);

        Double d = rs.getStateValueAsDouble("ElevationModel.MinElevation");
        if (d != null)
        {
            params.setValue(AVKey.ELEVATION_MIN, d);
        }
        else
        {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append("term.minElevation");
        }

        d = rs.getStateValueAsDouble("ElevationModel.MaxElevation");
        if (d != null)
        {
            params.setValue(AVKey.ELEVATION_MAX, d);
        }
        else
        {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append("term.maxElevation");
        }

        if (sb.length() > 0)
        {
            String message = Logging.getMessage("BasicElevationModel.InvalidDescriptorFields", sb.toString());
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
    }
}
