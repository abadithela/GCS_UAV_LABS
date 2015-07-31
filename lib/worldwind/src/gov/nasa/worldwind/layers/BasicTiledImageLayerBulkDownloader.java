/*
Copyright (C) 2001, 2009 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.retrieve.*;
import gov.nasa.worldwind.util.*;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Downloads imagery not currently available in the World Wind file cache or a specified file store. The class derives
 * from {@link Thread} and is meant to operate in its own thread.
 * <p/>
 * The sector and resolution associated with the downloader are specified during construction and are final.
 *
 * @author tag
 * @version $Id: BasicTiledImageLayerBulkDownloader.java 12546 2009-09-03 05:36:57Z tgaskins $
 */
public class BasicTiledImageLayerBulkDownloader extends BulkRetrievalThread
{
    protected final static int MAX_TILE_COUNT_PER_REGION = 200;
    protected final static long DEFAULT_AVERAGE_FILE_SIZE = 350000L;

    private final BasicTiledImageLayer layer;
    private final int level;
    private ArrayList<TextureTile> missingTiles;

    /**
     * Constructs a downloader to retrieve imagery not currently available in the World Wind file cache.
     * <p/>
     * The thread returned is not started during construction, the caller must start the thread.
     *
     * @param layer      the layer for which to download imagery.
     * @param sector     the sector to download data for. This value is final.
     * @param resolution the target resolution, provided in radians of latitude per texel. This value is final.
     *
     * @throws IllegalArgumentException if either the layer or sector are null, or the resolution is less than zero.
     */
    public BasicTiledImageLayerBulkDownloader(BasicTiledImageLayer layer, Sector sector, double resolution)
    {
        // Arguments checked in parent constructor
        super(layer, sector, resolution, layer.getDataFileStore());

        this.layer = layer;
        this.level = this.layer.computeLevelForResolution(sector, resolution);
    }

    /**
     * Constructs a downloader to retrieve imagery not currently available in a specified file store.
     * <p/>
     * The thread returned is not started during construction, the caller must start the thread.
     *
     * @param layer      the layer for which to download imagery.
     * @param sector     the sector to download data for. This value is final.
     * @param resolution the target resolution, provided in radians of latitude per texel. This value is final.
     * @param fileStore  the file store in which to place the downloaded elevations.
     *
     * @throws IllegalArgumentException if either the layer, the sector or file store are null, or the resolution is
     *                                  less than zero.
     */
    public BasicTiledImageLayerBulkDownloader(BasicTiledImageLayer layer, Sector sector, double resolution,
        FileStore fileStore)
    {
        // Arguments checked in parent constructor
        super(layer, sector, resolution, fileStore);

        this.layer = layer;
        this.level = this.layer.computeLevelForResolution(sector, resolution);
    }

    public void run()
    {
        try
        {
            // Count missing tiles
            this.progress.setTotalCount(countMissingTiles());
            this.progress.setTotalSize(this.progress.getTotalCount() * estimateAverageTileSize());
            int predictedMissingCount = this.progress.getTotalCount();
            int foundMissingCount = 0;

            // Determine and request missing tiles by level/region
            for (int levelNumber = 0; levelNumber <= this.level; levelNumber++)
            {
                if (this.layer.getLevels().isLevelEmpty(levelNumber))
                    continue;

                Sector[] regions = computeRegions(this.sector, levelNumber, MAX_TILE_COUNT_PER_REGION);

                for (Sector region : regions)
                {
                    // Determine missing tiles
                    this.missingTiles = getMissingTilesInSector(region, levelNumber);
                    foundMissingCount += this.missingTiles.size();

                    // Submit missing tiles requests at intervals
                    while (this.missingTiles.size() > 0)
                    {
                        submitMissingTilesRequests();
                        if (this.missingTiles.size() > 0)
                            Thread.sleep(RETRIEVAL_SERVICE_POLL_DELAY);
                    }
                }
            }
            // Adjust progress now that we know how many tiles have been found missing during the process
            int foundAbsentCount = predictedMissingCount - this.progress.getTotalCount();
            this.progress.setTotalCount(foundMissingCount - foundAbsentCount); // absent tiles are counted off
            this.progress.setTotalSize(this.progress.getTotalCount() * estimateAverageTileSize());
        }
        catch (InterruptedException e)
        {
            String message = Logging.getMessage("generic.BulkRetrievalInterrupted", this.layer.getName());
            Logging.logger().log(java.util.logging.Level.WARNING, message, e);
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("generic.ExceptionDuringBulkRetrieval", this.layer.getName());
            Logging.logger().severe(message);
            throw new RuntimeException(message);
        }
    }

    protected int countMissingTiles() throws InterruptedException
    {
        int count = 0;
        for (int levelNumber = 0; levelNumber <= this.level; levelNumber++)
        {
            if (this.layer.getLevels().isLevelEmpty(levelNumber))
                continue;

            count += getMissingTilesInSector(this.sector, levelNumber).size();
        }

        return count;
    }

    protected synchronized void submitMissingTilesRequests() throws InterruptedException
    {
        RetrievalService rs = WorldWind.getRetrievalService();
        int i = 0;
        while (this.missingTiles.size() > i && rs.isAvailable())
        {
            Thread.sleep(1); // generates InterruptedException if thread has been interrupted

            TextureTile tile = this.missingTiles.get(i);

            if (this.layer.getLevels().isResourceAbsent(tile))
            {
                removeAbsentTile(tile);  // tile is absent, count it off.
                continue;
            }

            URL url = this.fileStore.findFile(tile.getPath(), false);
            if (url != null)
            {
                // tile has been retrieved and is local now, count it as retrieved.
                removeRetrievedTile(tile);
                continue;
            }

            this.layer.downloadTexture(tile, new BulkDownloadPostProcessor(tile, this.layer, this.fileStore));
            i++;
        }
    }

    protected class BulkDownloadPostProcessor extends BasicTiledImageLayer.DownloadPostProcessor
    {
        public BulkDownloadPostProcessor(TextureTile tile, BasicTiledImageLayer layer, FileStore fileStore)
        {
            super(tile, layer, fileStore);
        }

        public ByteBuffer run(Retriever retriever)
        {
            ByteBuffer buffer = super.run(retriever);
            if (buffer != null)
                removeRetrievedTile(this.tile);
            return buffer;
        }
    }

    protected synchronized void removeRetrievedTile(TextureTile tile)
    {
        this.missingTiles.remove(tile);
        // Update progress
        this.progress.setCurrentCount(this.progress.getCurrentCount() + 1);
        this.progress.setCurrentSize(this.progress.getCurrentSize() + estimateAverageTileSize());
        this.progress.setLastUpdateTime(System.currentTimeMillis());
    }

    protected synchronized void removeAbsentTile(TextureTile tile)
    {
        this.missingTiles.remove(tile);
        // Decrease progress expected total count and size
        this.progress.setTotalCount(this.progress.getTotalCount() - 1);
        this.progress.setTotalSize(this.progress.getTotalSize() - estimateAverageTileSize());
        this.progress.setLastUpdateTime(System.currentTimeMillis());
    }

    /**
     * Get the estimated size in byte of the missing imagery for the object's {@link Sector}, resolution and file store.
     * Note that the target resolution must be provided in radian latitude per texel - which is the resolution in meter
     * divided by the globe radius.
     *
     * @return the estimated size in byte of the missing imagery.
     */
    protected long getEstimatedMissingDataSize()
    {
        int maxLevel = this.layer.computeLevelForResolution(this.sector, this.resolution);
        // Total expected tiles
        int totCount = 0;
        for (int levelNumber = 0; levelNumber <= maxLevel; levelNumber++)
        {
            if (!this.layer.getLevels().isLevelEmpty(levelNumber))
                totCount += this.layer.countImagesInSector(sector, levelNumber);
        }
        // Sample random small sized sectors at finest level
        Sector[] regions = computeRegions(this.sector, maxLevel, 36); // max 6x6 tiles per region
        int samples = 6;
        int regionMissing = 0;
        int regionCount = 0;
        try
        {
            if (regions.length <= samples)
            {
                regionCount = this.layer.countImagesInSector(this.sector, maxLevel);
                regionMissing = getMissingTilesInSector(this.sector, maxLevel).size();
            }
            else
            {
                Random rand = new Random();
                while (samples > 0)
                {
                    int i = rand.nextInt(regions.length);
                    Sector region = regions[i];
                    if (region != null)
                    {
                        // Count how many tiles are missing inside this region
                        regionCount += this.layer.countImagesInSector(region, maxLevel);
                        regionMissing += getMissingTilesInSector(region, maxLevel).size();
                        regions[i] = null;
                        samples--;
                    }
                }
            }
        }
        catch (InterruptedException e)
        {
            return 0;
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("generic.ExceptionDuringDataSizeEstimate", this.layer.getName());
            Logging.logger().severe(message);
            throw new RuntimeException(message);
        }

        // Extrapolate total missing count
        int totMissing = totCount * regionMissing / regionCount;
        // Get average tile size estimate
        long averageTileSize = estimateAverageTileSize();

        return totMissing * averageTileSize;
    }

    protected Sector[] computeRegions(Sector sector, int levelNumber, int maxCount)
    {
        int tileCount = this.layer.countImagesInSector(sector, levelNumber);

        if (tileCount <= maxCount)
            return new Sector[] {sector};

        // Divide sector in regions that will contain no more tiles then allowed
        int div = (int) Math.ceil(Math.sqrt((float) tileCount / maxCount));
        return sector.subdivide(div);
    }

    protected ArrayList<TextureTile> getMissingTilesInSector(Sector sector, int levelNumber)
        throws InterruptedException
    {
        ArrayList<TextureTile> tiles = new ArrayList<TextureTile>();

        TextureTile[][] tileArray = this.layer.getTilesInSector(sector, levelNumber);
        for (TextureTile[] row : tileArray)
        {
            for (TextureTile tile : row)
            {
                Thread.sleep(1); // generates InterruptedException if thread has been interrupted

                if (tile == null)
                    continue;

                if (isTileLocalOrAbsent(tile))
                    continue;  // tile is local or absent

                tiles.add(tile);
            }
        }
        return tiles;
    }

    protected boolean isTileLocalOrAbsent(TextureTile tile)
    {
        if (this.layer.getLevels().isResourceAbsent(tile))
            return true;  // tile is absent

        URL url = this.fileStore.findFile(tile.getPath(), false);

        return url != null && !this.layer.isTextureFileExpired(tile, url, fileStore);
    }

    protected long estimateAverageTileSize()
    {
        Long previouslyComputedSize = (Long) this.layer.getValue(AVKey.AVERAGE_TILE_SIZE);
        if (previouslyComputedSize != null)
            return previouslyComputedSize;

        long size = 0;
        long count = 0;

        // Average cached tile files size in a few directories from first non empty level
        Level targetLevel = this.layer.getLevels().getFirstLevel();
        while (targetLevel.isEmpty() && !targetLevel.equals(this.layer.getLevels().getLastLevel()))
        {
            targetLevel = this.layer.getLevels().getLevel(targetLevel.getLevelNumber() + 1);
        }

        File cacheRoot = new File(this.fileStore.getWriteLocation(), targetLevel.getPath());
        if (cacheRoot.exists())
        {
            File[] rowDirs = cacheRoot.listFiles(new FileFilter()
            {
                public boolean accept(File file)
                {
                    return file.isDirectory();
                }
            });
            for (File dir : rowDirs)
            {
                long averageSize = computeAverageTileSize(dir);
                if (averageSize > 0)
                {
                    size += averageSize;
                    count++;
                }
                if (count >= 2) // average content from up to 2 cache folders
                    break;
            }
        }

        Long averageTileSize = DEFAULT_AVERAGE_FILE_SIZE;
        if (count > 0 && size > 0)
        {
            averageTileSize = size / count;
            this.layer.setValue(AVKey.AVERAGE_TILE_SIZE, averageTileSize);
        }

        return averageTileSize;
    }

    protected long computeAverageTileSize(File dir)
    {
        long size = 0;
        int count = 0;

        File[] files = dir.listFiles();
        for (File file : files)
        {
            try
            {
                FileInputStream fis = new FileInputStream(file);
                size += fis.available();
                fis.close();
                count++;
            }
            catch (IOException e)
            {
                count += 0;
            }
        }

        return count > 0 ? size / count : 0;
    }
}
