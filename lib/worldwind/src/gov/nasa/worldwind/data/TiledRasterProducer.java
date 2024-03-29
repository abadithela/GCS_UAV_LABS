/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.*;

/**
 * @author dcollins
 * @version $Id: TiledRasterProducer.java 8329 2009-01-05 21:29:51Z dcollins $
 */
public abstract class TiledRasterProducer extends AbstractDataStoreProducer
{
    private static final long DEFAULT_TILED_RASTER_PRODUCER_CACHE_SIZE = 300000000L; // ~300 megabytes
    private static final int DEFAULT_TILED_RASTER_PRODUCER_LARGE_DATASET_THRESHOLD = 3000; // 3000 pixels
    private static final int DEFAULT_WRITE_THREAD_POOL_SIZE = 2;
    private static final int DEFAULT_TILE_WIDTH_AND_HEIGHT = 512;
    private static final int DEFAULT_SINGLE_LEVEL_TILE_WIDTH_AND_HEIGHT = 512;
    private static final double DEFAULT_LEVEL_ZERO_TILE_DELTA = 36d;

    // List of source data rasters.
    private java.util.List<DataRaster> dataRasterList = new java.util.ArrayList<DataRaster>();
    // Data raster caching.
    private MemoryCache rasterCache;
    // Concurrent processing helper objects.
    private final java.util.concurrent.ExecutorService tileWriteService;
    private final java.util.concurrent.Semaphore tileWriteSemaphore;
    private final Object fileLock = new Object();
    // Progress counters.
    private int tile;
    private int tileCount;

    public TiledRasterProducer(MemoryCache cache, int writeThreadPoolSize)
    {
        if (cache == null)
        {
            String message = Logging.getMessage("nullValue.CacheIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (writeThreadPoolSize < 1)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "writeThreadPoolSize < 1");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.rasterCache = cache;
        this.tileWriteService = this.createDefaultTileWriteService(writeThreadPoolSize);
        this.tileWriteSemaphore = new java.util.concurrent.Semaphore(writeThreadPoolSize, true);
    }

    public TiledRasterProducer()
    {
        this(createDefaultCache(), DEFAULT_WRITE_THREAD_POOL_SIZE);
    }

    // TODO: this describes the file types the producer will read. Make that more clear in the method name.
    public String getDataSourceDescription()
    {
        DataRasterReader[] readers = this.getDataRasterReaders();
        if (readers == null || readers.length < 1)
            return "";

        // Collect all the unique format suffixes available in all readers. If a reader does not publish any
        // format suffixes, then collect it's description.
        java.util.Set<String> suffixSet = new java.util.TreeSet<String>();
        java.util.Set<String> descriptionSet = new java.util.TreeSet<String>();
        for (DataRasterReader reader : readers)
        {
            String description = reader.getDescription();
            String[] names = reader.getSuffixes();

            if (names != null && names.length > 0)
                suffixSet.addAll(java.util.Arrays.asList(names));
            else
                descriptionSet.add(description);
        }

        // Create a string representaiton of the format suffixes (or description if no suffixes are available) for
        // all readers.
        StringBuilder sb = new StringBuilder();
        for (String suffix : suffixSet)
        {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append("*.").append(suffix);
        }
        for (String description : descriptionSet)
        {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(description);
        }
        return sb.toString();
    }

    public void removeProductionState()
    {
        java.io.File installLocation = this.installLocationFor(this.getStoreParameters());

        if (installLocation == null || !installLocation.exists())
        {
            String message = "Install location is null or does not exist";
            Logging.logger().warning(message);
            return;
        }

        try
        {
            WWIO.deleteDirectory(installLocation);
        }
        catch (Exception e)
        {
            String message = "Exception while removing install location";
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
        }
    }

    protected abstract DataRaster createDataRaster(int width, int height, Sector sector, AVList params);

    protected abstract DataRasterReader[] getDataRasterReaders();

    protected abstract DataRasterWriter[] getDataRasterWriters();
    
    protected MemoryCache getCache()
    {
        return this.rasterCache;
    }

    protected java.util.concurrent.ExecutorService getTileWriteService()
    {
        return this.tileWriteService;
    }

    protected java.util.concurrent.Semaphore getTileWriteSemaphore()
    {
        return this.tileWriteSemaphore;
    }

    protected void doStartProduction(AVList parameters) throws Exception
    {
        // Copy production parameters to prevent changes to caller's reference.
        AVList productionParams = parameters.copy();
        this.initProductionParameters(productionParams);

        // Assemble the source data rasters.
        this.assembleDataRasters();

        // Initialize the level set parameters, and create the level set.
        this.initLevelSetParameters(productionParams);
        LevelSet levelSet = new LevelSet(productionParams);
        // Install the each tiles of the LevelSet.
        this.installLevelSet(levelSet, productionParams);

        // Wait for concurrent tasks to complete.
        this.waitForInstallTileTasks();
        // Clear the raster cache.
        this.getCache().clear();

        // Install the data descriptor for this tiled raster set.
        this.installDataDescriptor(productionParams);
    }

    protected String validateProductionParameters(AVList parameters)
    {
        StringBuilder sb = new StringBuilder();

        Object o = parameters.getValue(AVKey.FILE_STORE_LOCATION);
        if (o == null || !(o instanceof String) || ((String) o).length() < 1)
            sb.append((sb.length() > 0 ? ", " : "")).append(Logging.getMessage("term.fileStoreLocation"));

        o = parameters.getValue(AVKey.DATA_CACHE_NAME);
        if (o == null || !(o instanceof String) || ((String) o).length() == 0)
            sb.append((sb.length() > 0 ? ", " : "")).append(Logging.getMessage("term.fileStoreFolder"));

        o = parameters.getValue(AVKey.DATASET_NAME);
        if (o == null || !(o instanceof String) || ((String) o).length() < 1)
            sb.append((sb.length() > 0 ? ", " : "")).append(Logging.getMessage("term.datasetName"));

        if (sb.length() == 0)
            return null;

        return Logging.getMessage("DataStoreProducer.InvalidDataStoreParamters", sb.toString());
    }

    protected java.io.File installLocationFor(AVList params)
    {
        String fileStoreLocation = params.getStringValue(AVKey.FILE_STORE_LOCATION);
        String dataCacheName = params.getStringValue(AVKey.DATA_CACHE_NAME);
        if (fileStoreLocation == null || dataCacheName == null)
            return null;

        String path = appendPathPart(fileStoreLocation, dataCacheName);
        if (path == null || path.length() < 1)
            return null;

        return new java.io.File(path);
    }

    //**************************************************************//
    //********************  LevelSet Assembly  *********************//
    //**************************************************************//

    protected void initProductionParameters(AVList params)
    {
        // Used by subclasses to specify default production parameters.
    }

    protected void initLevelSetParameters(AVList params)
    {
        int largeThreshold = Configuration.getIntegerValue(AVKey.TILED_RASTER_PRODUCER_LARGE_DATASET_THRESHOLD,
            DEFAULT_TILED_RASTER_PRODUCER_LARGE_DATASET_THRESHOLD);
        boolean isDataSetLarge = this.isDataSetLarge(this.dataRasterList, largeThreshold);

        Sector sector = (Sector) params.getValue(AVKey.SECTOR);
        if (sector == null)
        {
            // Compute a sector that bounds the data rasters. Make sure the sector does not exceed the limits of
            // latitude and longitude.
            sector = this.computeBoundingSector(this.dataRasterList);
            if (sector != null)
                sector = sector.intersection(Sector.FULL_SPHERE);
            params.setValue(AVKey.SECTOR, sector);
        }

        Integer tileWidth = (Integer) params.getValue(AVKey.TILE_WIDTH);
        if (tileWidth == null)
        {
            tileWidth = isDataSetLarge ? DEFAULT_TILE_WIDTH_AND_HEIGHT : DEFAULT_SINGLE_LEVEL_TILE_WIDTH_AND_HEIGHT;
            params.setValue(AVKey.TILE_WIDTH, tileWidth);
        }

        Integer tileHeight = (Integer) params.getValue(AVKey.TILE_HEIGHT);
        if (tileHeight == null)
        {
            tileHeight = isDataSetLarge ? DEFAULT_TILE_WIDTH_AND_HEIGHT : DEFAULT_SINGLE_LEVEL_TILE_WIDTH_AND_HEIGHT;
            params.setValue(AVKey.TILE_HEIGHT, tileHeight);
        }

        LatLon rasterTileDelta = this.computeRasterTileDelta(tileWidth, tileHeight, this.dataRasterList);
        LatLon desiredLevelZeroDelta = this.computeDesiredTileDelta(sector);

        Integer numLevels = (Integer) params.getValue(AVKey.NUM_LEVELS);
        if (numLevels == null)
        {
            // If the data set is large, then use compute a number of levels for the full pyramid. Otherwise use a
            // single level.
            numLevels = isDataSetLarge ? this.computeNumLevels(desiredLevelZeroDelta, rasterTileDelta) : 1;
            params.setValue(AVKey.NUM_LEVELS, numLevels);
        }

        Integer numEmptyLevels = (Integer) params.getValue(AVKey.NUM_EMPTY_LEVELS);
        if (numEmptyLevels == null)
        {
            numEmptyLevels = 0;
            params.setValue(AVKey.NUM_EMPTY_LEVELS, numEmptyLevels);
        }

        LatLon levelZeroTileDelta = (LatLon) params.getValue(AVKey.LEVEL_ZERO_TILE_DELTA);
        if (levelZeroTileDelta == null)
        {
            double scale = Math.pow(2d, numLevels - 1);
            levelZeroTileDelta = LatLon.fromDegrees(
                scale * rasterTileDelta.getLatitude().degrees,
                scale * rasterTileDelta.getLongitude().degrees);
            params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, levelZeroTileDelta);
        }

        LatLon tileOrigin = (LatLon) params.getValue(AVKey.TILE_ORIGIN);
        if (tileOrigin == null)
        {
            tileOrigin = new LatLon(sector.getMinLatitude(), sector.getMinLongitude());
            params.setValue(AVKey.TILE_ORIGIN, tileOrigin);
        }

        // If the default or caller-specified values define a level set that does not fit in the limits of latitude
        // and longitude, then we re-define the level set parameters using values known to fit in those limits.
        if (!this.isWithinLatLonLimits(sector, levelZeroTileDelta, tileOrigin))
        {
            String message = "TiledRasterProducer: native tiling is outside lat/lon limits. Falling back to default tiling.";
            Logging.logger().warning(message);

            levelZeroTileDelta = LatLon.fromDegrees(DEFAULT_LEVEL_ZERO_TILE_DELTA, DEFAULT_LEVEL_ZERO_TILE_DELTA);
            params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, levelZeroTileDelta);

            tileOrigin = new LatLon(Angle.NEG90, Angle.NEG180);
            params.setValue(AVKey.TILE_ORIGIN, tileOrigin);
            
            numLevels = this.computeNumLevels(levelZeroTileDelta, rasterTileDelta);
            params.setValue(AVKey.NUM_LEVELS, numLevels);

            int numLevelsNeeded = isDataSetLarge ? this.computeNumLevels(desiredLevelZeroDelta, rasterTileDelta) : 1;
            numEmptyLevels = (numLevels > numLevelsNeeded) ? (numLevels - numLevelsNeeded) : 0;
            params.setValue(AVKey.NUM_EMPTY_LEVELS, numEmptyLevels);
        }
    }

    protected boolean isDataSetLarge(Iterable<? extends DataRaster> rasters, int largeThreshold)
    {
        Sector sector = this.computeBoundingSector(rasters);
        LatLon pixelSize = this.computeSmallestPixelSize(rasters);
        int sectorWidth = (int) Math.ceil(sector.getDeltaLonDegrees() / pixelSize.getLongitude().degrees);
        int sectorHeight = (int) Math.ceil(sector.getDeltaLatDegrees() / pixelSize.getLatitude().degrees);
        return (sectorWidth >= largeThreshold) || (sectorHeight >= largeThreshold);
    }

    protected boolean isWithinLatLonLimits(Sector sector, LatLon tileDelta, LatLon tileOrigin)
    {
        double minLat = Math.floor((sector.getMinLatitude().degrees - tileOrigin.getLatitude().degrees)
            / tileDelta.getLatitude().degrees);
        minLat = tileOrigin.getLatitude().degrees + minLat * tileDelta.getLatitude().degrees;
        double maxLat = Math.ceil((sector.getMaxLatitude().degrees - tileOrigin.getLatitude().degrees)
            / tileDelta.getLatitude().degrees);
        maxLat = tileOrigin.getLatitude().degrees + maxLat * tileDelta.getLatitude().degrees;
        double minLon = Math.floor((sector.getMinLongitude().degrees - tileOrigin.getLongitude().degrees)
            / tileDelta.getLongitude().degrees);
        minLon = tileOrigin.getLongitude().degrees + minLon * tileDelta.getLongitude().degrees;
        double maxLon = Math.ceil((sector.getMaxLongitude().degrees - tileOrigin.getLongitude().degrees)
            / tileDelta.getLongitude().degrees);
        maxLon = tileOrigin.getLongitude().degrees + maxLon * tileDelta.getLongitude().degrees;
        return Sector.fromDegrees(minLat, maxLat, minLon, maxLon).isWithinLatLonLimits();
    }

    protected Sector computeBoundingSector(Iterable<? extends DataRaster> rasters)
    {
        Sector sector = null;
        for (DataRaster raster : rasters)
            sector = (sector != null) ? raster.getSector().union(sector) : raster.getSector();
        return sector;
    }

    protected LatLon computeRasterTileDelta(int tileWidth, int tileHeight, Iterable<? extends DataRaster> rasters)
    {
        LatLon pixelSize = this.computeSmallestPixelSize(rasters);
        // Compute the tile size in latitude and longitude, given a raster's sector and dimension, and the tile
        // dimensions. In this computation a pixel is assumed to cover a finite area.
        double latDelta = tileHeight * pixelSize.getLatitude().degrees;
        double lonDelta = tileWidth  * pixelSize.getLongitude().degrees;
        return LatLon.fromDegrees(latDelta, lonDelta);
    }

    protected LatLon computeDesiredTileDelta(Sector sector)
    {
        double levelZeroLat = Math.min(sector.getDeltaLatDegrees(), DEFAULT_LEVEL_ZERO_TILE_DELTA);
        double levelZeroLon = Math.min(sector.getDeltaLonDegrees(), DEFAULT_LEVEL_ZERO_TILE_DELTA);
        return LatLon.fromDegrees(levelZeroLat, levelZeroLon);
    }

    protected LatLon computeRasterPixelSize(DataRaster raster)
    {
        // Compute the raster's pixel dimension in latitude and longitude. In this computation a pixel is assumed to
        // cover a finite area.
        return LatLon.fromDegrees(
            raster.getSector().getDeltaLatDegrees() / raster.getHeight(),
            raster.getSector().getDeltaLonDegrees() / raster.getWidth());
    }

    protected LatLon computeSmallestPixelSize(Iterable<? extends DataRaster> rasters)
    {
        // Find the smallest pixel dimensions in the given rasters.
        double smallestLat = Double.MAX_VALUE;
        double smallestLon = Double.MAX_VALUE;
        for (DataRaster raster : rasters)
        {
            LatLon curSize = this.computeRasterPixelSize(raster);
            if (smallestLat > curSize.getLatitude().degrees)
                smallestLat = curSize.getLatitude().degrees;
            if (smallestLon > curSize.getLongitude().degrees)
                smallestLon = curSize.getLongitude().degrees;
        }
        return LatLon.fromDegrees(smallestLat, smallestLon);
    }

    protected int computeNumLevels(LatLon levelZeroDelta, LatLon lastLevelDelta)
    {
        // Compute the number of levels needed to achieve the given last level tile delta, starting from the given
        // level zero tile delta.
        double numLatLevels = WWMath.logBase2(levelZeroDelta.getLatitude().getDegrees())
            - WWMath.logBase2(lastLevelDelta.getLatitude().getDegrees());
        double numLonLevels = WWMath.logBase2(levelZeroDelta.getLongitude().getDegrees())
            - WWMath.logBase2(lastLevelDelta.getLongitude().getDegrees());

        // Compute the maximum number of levels needed, but limit the number of levels to positive integers greater
        // than or equal to one.
        int numLevels = (int) Math.ceil(Math.max(numLatLevels, numLonLevels));
        if (numLevels < 1)
            numLevels = 1;
        
        return numLevels;

    }

    //**************************************************************//
    //********************  DataRaster Assembly  *******************//
    //**************************************************************//

    protected void assembleDataRasters() throws Exception
    {
        // Exit if the caller has instructed us to stop production.
        if (this.isStopped())
            return;

        for (DataSource source : this.getDataSourceList())
        {
            // Exit if the caller has instructed us to stop production.
            if (this.isStopped())
                break;

            String message = this.validateDataSource(source);
            if (message != null)
            {
                Logging.logger().severe(message);
                throw new java.io.IOException(message);
            }

            this.assembleDataSource(source);
        }
    }

    protected void assembleDataSource(DataSource dataSource) throws Exception
    {
        if (dataSource.getSource() instanceof DataRaster)
        {
            this.dataRasterList.add((DataRaster) dataSource.getSource());
        }
        else
        {
            DataRasterReader reader = ReadableDataRaster.findReaderFor(dataSource, this.getDataRasterReaders());
            this.dataRasterList.add(new ReadableDataRaster(dataSource, reader, this.getCache()));
        }
    }

    protected String validateDataSource(DataSource dataSource)
    {
        if (dataSource == null)
            return Logging.getMessage("nullValue.DataSourceIsNull");

        if (!(dataSource.getSource() instanceof DataRaster))
        {
            DataRasterReader reader = ReadableDataRaster.findReaderFor(dataSource, this.getDataRasterReaders());
            if (reader == null)
                return Logging.getMessage("DataStoreProducer.InvalidDataSource", dataSource);
        }

        return null;
    }

    protected static MemoryCache createDefaultCache()
    {
        long cacheSize = Configuration.getLongValue(AVKey.TILED_RASTER_PRODUCER_CACHE_SIZE,
            DEFAULT_TILED_RASTER_PRODUCER_CACHE_SIZE);
        return new BasicMemoryCache((long) (0.8 * cacheSize), cacheSize);
    }

    //**************************************************************//
    //********************  LevelSet Installation  *****************//
    //**************************************************************//

    protected void installLevelSet(LevelSet levelSet, AVList params) throws java.io.IOException
    {
        // Exit if the caller has instructed us to stop production.
        if (this.isStopped())
            return;

        // Setup the progress parameters.
        this.setProgressParams(levelSet);
        this.startProgress();
        
        Sector sector = levelSet.getSector();
        Level level = levelSet.getFirstLevel();

        Angle dLat = level.getTileDelta().getLatitude();
        Angle dLon = level.getTileDelta().getLongitude();
        Angle latOrigin = levelSet.getTileOrigin().getLatitude();
        Angle lonOrigin = levelSet.getTileOrigin().getLongitude();
        int firstRow = Tile.computeRow(dLat, sector.getMinLatitude(), latOrigin);
        int firstCol = Tile.computeColumn(dLon, sector.getMinLongitude(), lonOrigin);
        int lastRow  = Tile.computeRow(dLat, sector.getMaxLatitude(), latOrigin);
        int lastCol  = Tile.computeColumn(dLon, sector.getMaxLongitude(), lonOrigin);

        Angle p1 = Tile.computeRowLatitude(firstRow, dLat, latOrigin);
        for (int row = firstRow; row <= lastRow; row++)
        {
            // Exit if the caller has instructed us to stop production.
            if (this.isStopped())
                break;

            Angle p2 = p1.add(dLat);
            Angle t1 = Tile.computeColumnLongitude(firstCol, dLon, lonOrigin);
            for (int col = firstCol; col <= lastCol; col++)
            {
                // Exit if the caller has instructed us to stop production.
                if (this.isStopped())
                    break;

                Angle t2 = t1.add(dLon);

                Tile tile = new Tile(new Sector(p1, p2, t1, t2), level, row, col);
                DataRaster tileRaster = this.createTileRaster(levelSet, tile, params);
                // Write the top-level tile raster to disk.
                if (tileRaster != null)
                    this.installTileRasterLater(tile, tileRaster, params);

                t1 = t2;
            }
            p1 = p2;
        }
    }

    protected DataRaster createTileRaster(LevelSet levelSet, Tile tile, AVList params) throws java.io.IOException
    {
        // Exit if the caller has instructed us to stop production.
        if (this.isStopped())
            return null;

        DataRaster tileRaster;

        // If we have reached the final level, then create a tile raster from the original data sources.
        if (levelSet.isFinalLevel(tile.getLevelNumber()))
        {
            tileRaster = this.drawDataSources(levelSet, tile, this.dataRasterList, params);
        }
        // Otherwise, recursively create a tile raster from the next level's tile rasters.
        else
        {
            tileRaster = this.drawDescendants(levelSet, tile, params);
        }

        this.updateProgress();

        return tileRaster;
    }

    protected DataRaster drawDataSources(LevelSet levelSet, Tile tile, Iterable<DataRaster> dataRasters, AVList params)
                                         throws java.io.IOException
    {
        DataRaster tileRaster = null;

        // Find the data sources that intersect this tile and intersect the LevelSet sector.
        java.util.ArrayList<DataRaster> intersectingRasters = new java.util.ArrayList<DataRaster>();
        for (DataRaster raster : dataRasters)
        {
            if (raster.getSector().intersects(tile.getSector()) && raster.getSector().intersects(levelSet.getSector()))
                intersectingRasters.add(raster);
        }

        // If any data sources intersect this tile, and the tile's level is not empty, then we attempt to read those
        // sources and render them into this tile.
        if (!intersectingRasters.isEmpty() && !tile.getLevel().isEmpty())
        {
            // Create the tile raster to render into.
            tileRaster = this.createDataRaster(tile.getLevel().getTileWidth(), tile.getLevel().getTileHeight(),
                tile.getSector(), params);
            // Render each data source raster into the tile raster.
            for (DataRaster raster : intersectingRasters)
                raster.drawOnCanvas(tileRaster);
        }

        // Make the data rasters available for garbage collection.
        intersectingRasters.clear();
        //noinspection UnusedAssignment
        intersectingRasters = null;

        return tileRaster;
    }

    protected DataRaster drawDescendants(LevelSet levelSet, Tile tile, AVList params) throws java.io.IOException
    {
        DataRaster tileRaster = null;
        boolean hasDescendants = false;

        // Recursively create sub-tile rasters.
        Tile[] subTiles = this.createSubTiles(tile, levelSet.getLevel(tile.getLevelNumber() + 1));
        DataRaster[] subRasters = new DataRaster[subTiles.length];
        for (int index = 0; index < subTiles.length; index++)
        {
            // If the sub-tile does not intersect the level set, then skip that sub-tile.
            if (subTiles[index].getSector().intersects(levelSet.getSector()))
            {
                // Recursively create the sub-tile raster.
                DataRaster subRaster = this.createTileRaster(levelSet, subTiles[index], params);
                // If creating the sub-tile raster fails, then skip that sub-tile.
                if (subRaster != null)
                {
                    subRasters[index] = subRaster;
                    hasDescendants = true;
                }
            }
        }

        // Exit if the caller has instructed us to stop production.
        if (this.isStopped())
            return null;

        // If any of the sub-tiles successfully created a data raster, then we potentially create this tile's raster,
        // then write the sub-tiles to disk.
        if (hasDescendants)
        {
            // If this tile's level is not empty, then create and render the tile's raster.
            if (!tile.getLevel().isEmpty())
            {
                // Create the tile's raster.
                tileRaster = this.createDataRaster(tile.getLevel().getTileWidth(), tile.getLevel().getTileHeight(),
                    tile.getSector(), params);

                for (int index = 0; index < subTiles.length; index++)
                {
                    if (subRasters[index] != null)
                    {
                        // Render the sub-tile raster to this this tile raster.
                        subRasters[index].drawOnCanvas(tileRaster);
                        // Write the sub-tile raster to disk.
                        this.installTileRasterLater(subTiles[index], subRasters[index], params);
                    }
                }
            }
        }

        // Make the sub-tiles and sub-rasters available for garbage collection.
        for (int index = 0; index < subTiles.length; index++)
        {
            subTiles[index] = null;
            subRasters[index] = null;
        }
        //noinspection UnusedAssignment
        subTiles = null;
        //noinspection UnusedAssignment
        subRasters = null;

        return tileRaster;
    }

    protected Tile[] createSubTiles(Tile tile, Level nextLevel)
    {
        Angle p0 = tile.getSector().getMinLatitude();
        Angle p2 = tile.getSector().getMaxLatitude();
        Angle p1 = Angle.midAngle(p0, p2);

        Angle t0 = tile.getSector().getMinLongitude();
        Angle t2 = tile.getSector().getMaxLongitude();
        Angle t1 = Angle.midAngle(t0, t2);

        int row = tile.getRow();
        int col = tile.getColumn();

        Tile[] subTiles = new Tile[4];
        subTiles[0] = new Tile(new Sector(p0, p1, t0, t1), nextLevel, 2 * row, 2 * col);
        subTiles[1] = new Tile(new Sector(p0, p1, t1, t2), nextLevel, 2 * row, 2 * col + 1);
        subTiles[2] = new Tile(new Sector(p1, p2, t1, t2), nextLevel, 2 * row + 1, 2 * col + 1);
        subTiles[3] = new Tile(new Sector(p1, p2, t0, t1), nextLevel, 2 * row + 1, 2 * col);

        return subTiles;
    }

    //**************************************************************//
    //********************  Tile Installation  *********************//
    //**************************************************************//

    protected java.util.concurrent.ExecutorService createDefaultTileWriteService(int threadPoolSize)
    {
        // TODO: comment

        // Create a fixed thread pool, but provide a callback to release a tile write permit when a task completes.
        return new java.util.concurrent.ThreadPoolExecutor(
            // Fixed size thread pool.
            threadPoolSize, threadPoolSize,
            // This value is irrelevant, as threads only terminated when the executor is shutdown.
            0L, java.util.concurrent.TimeUnit.MILLISECONDS,
            // Provide an unbounded work queue.
            new java.util.concurrent.LinkedBlockingQueue<Runnable>())
        {
            protected void afterExecute(Runnable runnable, Throwable t)
            {
                // Invoke the superclass routine, then release a tile write permit.
                super.afterExecute(runnable, t);
                TiledRasterProducer.this.installTileRasterComplete();
            }
        };
    }

    protected void installTileRasterLater(final Tile tile, final DataRaster tileRaster, final AVList params)
    {
        // TODO: comment
        // Try to aquire a permit from the tile write semaphore.
        this.getTileWriteSemaphore().acquireUninterruptibly();
        // We've aquired the permit, now execute the installTileRaster() routine in a different thread.
        this.getTileWriteService().execute(new Runnable()
        {
            public void run()
            {
                try
                {
                    installTileRaster(tile, tileRaster, params);
                    // Dispose the data raster.
                    if (tileRaster instanceof Disposable)
                        ((Disposable) tileRaster).dispose();
                }
                catch (Throwable t)
                {
                    String message = Logging.getMessage("generic.ExceptionWhileWriting", tile);
                    Logging.logger().log(java.util.logging.Level.SEVERE, message, t);
                }
            }
        });
    }

    protected void installTileRasterComplete()
    {
        // TODO: comment
        this.getTileWriteSemaphore().release();
    }

    protected void waitForInstallTileTasks()
    {
        // TODO: comment
        try
        {
            java.util.concurrent.ExecutorService service = this.getTileWriteService();
            service.shutdown();
            // Block this thread until the executor has completed.
            while (!service.awaitTermination(1000L, java.util.concurrent.TimeUnit.MILLISECONDS))
            {}
        }
        catch (InterruptedException e)
        {
            // TODO: proper logging message
            String message = "Exception while shutting down executor";
            Logging.logger().severe(message);
        }
    }

    protected void installTileRaster(Tile tile, DataRaster tileRaster, AVList params) throws java.io.IOException
    {
        java.io.File installLocation;

        // Compute the install location of the tile.
        Object result = this.installLocationForTile(params, tile);
        if (result instanceof java.io.File)
        {
            installLocation = (java.io.File) result;
        }
        else
        {
            String message = result.toString();
            Logging.logger().severe(message);
            throw new java.io.IOException(message);
        }

        synchronized (this.fileLock)
        {
            java.io.File dir = installLocation.getParentFile();
            if (!dir.exists())
            {
                if (!dir.mkdirs())
                {
                    String message = Logging.getMessage("generic.CannotCreateFile", dir);
                    Logging.logger().warning(message);
                }
            }
        }

        // Write the tile data to the filesystem.
        String formatSuffix = params.getStringValue(AVKey.FORMAT_SUFFIX);
        DataRasterWriter[] writers = this.getDataRasterWriters();

        Object writer = this.findWriterFor(tileRaster, formatSuffix, installLocation, writers);
        if (writer instanceof DataRasterWriter)
        {
            try
            {
                ((DataRasterWriter) writer).write(tileRaster, formatSuffix, installLocation);
            }
            catch (java.io.IOException e)
            {
                String message = Logging.getMessage("generic.ExceptionWhileWriting", installLocation);
                Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            }
        }
    }

    protected Object installLocationForTile(AVList installParams, Tile tile)
    {
        String path = null;

        String s = installParams.getStringValue(AVKey.FILE_STORE_LOCATION);
        if (s != null)
            path = appendPathPart(path, s);

        s = tile.getPath();
        if (s != null)
            path = appendPathPart(path, s);

        if (path == null || path.length() < 1)
            return Logging.getMessage("DataStoreProducer.InvalidTile", tile);

        return new java.io.File(path);
    }

    protected Object findWriterFor(DataRaster raster, String formatSuffix, java.io.File destination,
        DataRasterWriter[] writers)
    {
        for (DataRasterWriter writer : writers)
        {
            if (writer.canWrite(raster, formatSuffix, destination))
                return writer;
        }

        // No writer maching this DataRaster/formatSuffix.
        return Logging.getMessage("DataRaster.CannotWrite", raster, formatSuffix, destination);
    }

    private static String appendPathPart(String firstPart, String secondPart)
    {
        if (secondPart == null || secondPart.length() == 0)
            return firstPart;
        if (firstPart == null || firstPart.length() == 0)
            return secondPart;

        firstPart = WWIO.stripTrailingSeparator(firstPart);
        secondPart = WWIO.stripLeadingSeparator(secondPart);

        return firstPart + System.getProperty("file.separator") + secondPart;
    }

    //**************************************************************//
    //********************  DataDescriptor Installation  ***********//
    //**************************************************************//

    protected void installDataDescriptor(AVList params) throws java.io.IOException
    {
        // Exit if the caller has instructed us to stop production.
        if (this.isStopped())
            return;

        DataDescriptor descriptor = new BasicDataDescriptor();
        DataDescriptorWriter writer = new BasicDataDescriptorWriter();

        Object o = params.getValue(AVKey.FILE_STORE_LOCATION);
        if (o != null)
            descriptor.setFileStoreLocation(new java.io.File(o.toString()));

        o = params.getValue(AVKey.DATA_CACHE_NAME);
        if (o != null)
            descriptor.setFileStorePath(o.toString());

        o = params.getValue(AVKey.DATASET_NAME);
        if (o != null)
            descriptor.setName(o.toString());

        o = params.getValue(AVKey.DATA_TYPE);
        if (o != null)
            descriptor.setType(o.toString());

        for (java.util.Map.Entry<String, Object> avp : params.getEntries())
        {
            String key = avp.getKey();

            // Skip key-value pairs that the DataDescriptor specially manages.
            if (key.equals(AVKey.FILE_STORE_LOCATION)
                || key.equals(AVKey.DATA_CACHE_NAME)
                || key.equals(AVKey.DATASET_NAME)
                || key.equals(AVKey.DATA_TYPE))
            {
                continue;
            }

            descriptor.setValue(key, avp.getValue());
        }

        java.io.File installLocation;
        Object result = this.installLocationForDescriptor(descriptor, writer);
        if (result instanceof java.io.File)
        {
            installLocation = (java.io.File) result;
        }
        else
        {
            String message = result.toString();
            Logging.logger().severe(message);
            throw new java.io.IOException(message);
        }

        synchronized (this.fileLock)
        {
            java.io.File dir = installLocation.getParentFile();
            if (!dir.exists())
            {
                if (!dir.mkdirs())
                {
                    String message = Logging.getMessage("generic.CannotCreateFile", dir);
                    Logging.logger().warning(message);
                }
            }
        }

        writer.setDestination(installLocation);
        writer.write(descriptor);

        this.getProductionResultsList().add(descriptor);
    }

    protected Object installLocationForDescriptor(DataDescriptor descriptor, DataDescriptorWriter writer)
    {
        String path = null;

        java.io.File file = descriptor.getFileStoreLocation();
        if (file != null)
            path = appendPathPart(path, file.getPath());

        String s = descriptor.getFileStorePath();
        if (s != null)
            path = appendPathPart(path, s);

        s = "dataDescriptor" + WWIO.makeSuffixForMimeType(writer.getMimeType());
        path = appendPathPart(path, s);

        if (path == null || path.length() < 1)
            return Logging.getMessage("DataStoreProducer.InvalidDataStoreParamters", descriptor);

        return new java.io.File(path);
    }

    //**************************************************************//
    //********************  Progress  ******************************//
    //**************************************************************//

    protected void setProgressParams(LevelSet levelSet)
    {
        Sector sector = levelSet.getSector();

        this.tileCount = 0;
        for (Level level : levelSet.getLevels())
        {
            Angle dLat = level.getTileDelta().getLatitude();
            Angle dLon = level.getTileDelta().getLongitude();
            Angle latOrigin = levelSet.getTileOrigin().getLatitude();
            Angle lonOrigin = levelSet.getTileOrigin().getLongitude();
            int firstRow = Tile.computeRow(dLat, sector.getMinLatitude(), latOrigin);
            int firstCol = Tile.computeColumn(dLon, sector.getMinLongitude(), lonOrigin);
            int lastRow  = Tile.computeRow(dLat, sector.getMaxLatitude(), latOrigin);
            int lastCol  = Tile.computeColumn(dLon, sector.getMaxLongitude(), lonOrigin);
            this.tileCount += (lastRow - firstRow + 1) * (lastCol - firstCol + 1);
        }
    }

    protected void startProgress()
    {
        this.tile = 0;
        this.firePropertyChange(AVKey.PROGRESS, null, 0d);
    }

    protected void updateProgress()
    {
        double oldProgress =   this.tile / (double) this.tileCount;
        double newProgress = ++this.tile / (double) this.tileCount;
        this.firePropertyChange(AVKey.PROGRESS, oldProgress, newProgress);
    }
}
