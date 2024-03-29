/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers;

import com.sun.opengl.util.j2d.TextRenderer;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.retrieve.*;
import gov.nasa.worldwind.util.*;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * @author tag
 * @version $Id: TiledImageLayer.java 12778 2009-11-10 01:20:37Z tgaskins $
 */
public abstract class TiledImageLayer extends AbstractLayer
{
    // Infrastructure
    private static final LevelComparer levelComparer = new LevelComparer();
    private final LevelSet levels;
    private ArrayList<TextureTile> topLevels;
    private boolean forceLevelZeroLoads = false;
    private boolean levelZeroLoaded = false;
    private boolean retainLevelZeroTiles = false;
    private String tileCountName;
    private double splitScale = 0.9; // TODO: Make configurable
    private boolean useMipMaps = true;
    private ArrayList<String> supportedImageFormats = new ArrayList<String>();

    // Diagnostic flags
    private boolean showImageTileOutlines = false;
    private boolean drawTileBoundaries = false;
    private boolean useTransparentTextures = false;
    private boolean drawTileIDs = false;
    private boolean drawBoundingVolumes = false;

    // Stuff computed each frame
    private ArrayList<TextureTile> currentTiles = new ArrayList<TextureTile>();
    private TextureTile currentResourceTile;
    private boolean atMaxResolution = false;
    private PriorityBlockingQueue<Runnable> requestQ = new PriorityBlockingQueue<Runnable>(200);

    abstract protected void requestTexture(DrawContext dc, TextureTile tile);

    abstract protected void forceTextureLoad(TextureTile tile);

    abstract protected void checkResources();

    public TiledImageLayer(LevelSet levelSet)
    {
        if (levelSet == null)
        {
            String message = Logging.getMessage("nullValue.LevelSetIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.levels = new LevelSet(levelSet); // the caller's levelSet may change internally, so we copy it.

//        this.createTopLevelTiles();

        this.setPickEnabled(false); // textures are assumed to be terrain unless specifically indicated otherwise.
        this.tileCountName = this.getName() + " Tiles";

        this.checkResources(); // Check whether resources are present, and perform any necessary initialization.
    }

    @Override
    public void setName(String name)
    {
        super.setName(name);
        this.tileCountName = this.getName() + " Tiles";
    }

    public boolean isUseTransparentTextures()
    {
        return this.useTransparentTextures;
    }

    public void setUseTransparentTextures(boolean useTransparentTextures)
    {
        this.useTransparentTextures = useTransparentTextures;
    }

    public boolean isForceLevelZeroLoads()
    {
        return this.forceLevelZeroLoads;
    }

    public void setForceLevelZeroLoads(boolean forceLevelZeroLoads)
    {
        this.forceLevelZeroLoads = forceLevelZeroLoads;
    }

    public boolean isRetainLevelZeroTiles()
    {
        return retainLevelZeroTiles;
    }

    public void setRetainLevelZeroTiles(boolean retainLevelZeroTiles)
    {
        this.retainLevelZeroTiles = retainLevelZeroTiles;
    }

    public boolean isDrawTileIDs()
    {
        return drawTileIDs;
    }

    public void setDrawTileIDs(boolean drawTileIDs)
    {
        this.drawTileIDs = drawTileIDs;
    }

    public boolean isDrawTileBoundaries()
    {
        return drawTileBoundaries;
    }

    public void setDrawTileBoundaries(boolean drawTileBoundaries)
    {
        this.drawTileBoundaries = drawTileBoundaries;
    }

    public boolean isShowImageTileOutlines()
    {
        return showImageTileOutlines;
    }

    public void setShowImageTileOutlines(boolean showImageTileOutlines)
    {
        this.showImageTileOutlines = showImageTileOutlines;
    }

    public boolean isDrawBoundingVolumes()
    {
        return drawBoundingVolumes;
    }

    public void setDrawBoundingVolumes(boolean drawBoundingVolumes)
    {
        this.drawBoundingVolumes = drawBoundingVolumes;
    }

    protected LevelSet getLevels()
    {
        return levels;
    }

    protected void setSplitScale(double splitScale)
    {
        this.splitScale = splitScale;
    }

    protected PriorityBlockingQueue<Runnable> getRequestQ()
    {
        return requestQ;
    }

    public boolean isMultiResolution()
    {
        return this.getLevels() != null && this.getLevels().getNumLevels() > 1;
    }

    public boolean isAtMaxResolution()
    {
        return this.atMaxResolution;
    }

    public boolean isUseMipMaps()
    {
        return useMipMaps;
    }

    public void setUseMipMaps(boolean useMipMaps)
    {
        this.useMipMaps = useMipMaps;
    }

    /**
     * Specifies the time of the layer's most recent dataset update, beyond which cached data is invalid. If greater
     * than zero, the layer ignores and eliminates any in-memory or on-disk cached data older than the time specified,
     * and requests new information from the data source. If zero, the default, the layer applies any expiry times
     * associated with its individual levels, but only for on-disk cached data. In-memory cached data is expired only
     * when the expiry time is specified with this method and is greater than zero. This method also overwrites the
     * expiry times of the layer's individual levels if the value specified to the method is greater than zero.
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

    public ArrayList<TextureTile> getTopLevels()
    {
        if (this.topLevels == null)
            this.createTopLevelTiles();
        
        return topLevels;
    }

    private void createTopLevelTiles()
    {
        Sector sector = this.levels.getSector();

        Level level = levels.getFirstLevel();
        Angle dLat = level.getTileDelta().getLatitude();
        Angle dLon = level.getTileDelta().getLongitude();
        Angle latOrigin = this.levels.getTileOrigin().getLatitude();
        Angle lonOrigin = this.levels.getTileOrigin().getLongitude();

        // Determine the row and column offset from the common World Wind global tiling origin.
        int firstRow = Tile.computeRow(dLat, sector.getMinLatitude(), latOrigin);
        int firstCol = Tile.computeColumn(dLon, sector.getMinLongitude(), lonOrigin);
        int lastRow = Tile.computeRow(dLat, sector.getMaxLatitude(), latOrigin);
        int lastCol = Tile.computeColumn(dLon, sector.getMaxLongitude(), lonOrigin);

        int nLatTiles = lastRow - firstRow + 1;
        int nLonTiles = lastCol - firstCol + 1;

        this.topLevels = new ArrayList<TextureTile>(nLatTiles * nLonTiles);

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

                this.topLevels.add(new TextureTile(new Sector(p1, p2, t1, t2), level, row, col));
                t1 = t2;
            }
            p1 = p2;
        }
    }

    private void loadAllTopLevelTextures(DrawContext dc)
    {
        for (TextureTile tile : this.getTopLevels())
        {
            if (!tile.isTextureInMemory(dc.getTextureCache()))
                this.forceTextureLoad(tile);
        }

        this.levelZeroLoaded = true;
    }

    // ============== Tile Assembly ======================= //
    // ============== Tile Assembly ======================= //
    // ============== Tile Assembly ======================= //

    private void assembleTiles(DrawContext dc)
    {
        this.currentTiles.clear();

        for (TextureTile tile : this.getTopLevels())
        {
            if (this.isTileVisible(dc, tile))
            {
                this.currentResourceTile = null;
                this.addTileOrDescendants(dc, tile);
            }
        }
    }

    private void addTileOrDescendants(DrawContext dc, TextureTile tile)
    {
        if (this.meetsRenderCriteria(dc, tile))
        {
            this.addTile(dc, tile);
            return;
        }

        // The incoming tile does not meet the rendering criteria, so it must be subdivided and those
        // subdivisions tested against the criteria.

        // All tiles that meet the selection criteria are drawn, but some of those tiles will not have
        // textures associated with them either because their texture isn't loaded yet or because they
        // are finer grain than the layer has textures for. In these cases the tiles use the texture of
        // the closest ancestor that has a texture loaded. This ancestor is called the currentResourceTile.
        // A texture transform is applied during rendering to align the sector's texture coordinates with the
        // appropriate region of the ancestor's texture.

        TextureTile ancestorResource = null;

        try
        {
            // TODO: Revise this to reflect that the parent layer is only requested while the algorithm continues
            // to search for the layer matching the criteria.
            // At this point the tile does not meet the render criteria but it may have its texture in memory.
            // If so, register this tile as the resource tile. If not, then this tile will be the next level
            // below a tile with texture in memory. So to provide progressive resolution increase, add this tile
            // to the draw list. That will cause the tile to be drawn using its parent tile's texture, and it will
            // cause it's texture to be requested. At some future call to this method the tile's texture will be in
            // memory, it will not meet the render criteria, but will serve as the parent to a tile that goes
            // through this same process as this method recurses. The result of all this is that a tile isn't rendered
            // with its own texture unless all its parents have their textures loaded. In addition to causing
            // progressive resolution increase, this ensures that the parents are available as the user zooms out, and
            // therefore the layer remains visible until the user is zoomed out to the point the layer is no longer
            // active.
            if (tile.isTextureInMemory(dc.getTextureCache()) || tile.getLevelNumber() == 0)
            {
                ancestorResource = this.currentResourceTile;
                this.currentResourceTile = tile;
            }
            else if (!tile.getLevel().isEmpty())
            {
//                this.addTile(dc, tile);
//                return;

                // Issue a request for the parent before descending to the children.
//                if (tile.getLevelNumber() < this.levels.getNumLevels())
//                {
//                    // Request only tiles with data associated at this level
//                    if (!this.levels.isResourceAbsent(tile))
//                        this.requestTexture(dc, tile);
//                }
            }

            TextureTile[] subTiles = tile.createSubTiles(this.levels.getLevel(tile.getLevelNumber() + 1));
            for (TextureTile child : subTiles)
            {
                if (this.isTileVisible(dc, child))
                    this.addTileOrDescendants(dc, child);
            }
        }
        finally
        {
            if (ancestorResource != null) // Pop this tile as the currentResource ancestor
                this.currentResourceTile = ancestorResource;
        }
    }

    private void addTile(DrawContext dc, TextureTile tile)
    {
        tile.setFallbackTile(null);

        if (tile.isTextureInMemory(dc.getTextureCache()))
        {
            this.addTileToCurrent(tile);
            return;
        }

        // Level 0 loads may be forced
        if (tile.getLevelNumber() == 0 && this.forceLevelZeroLoads && !tile.isTextureInMemory(dc.getTextureCache()))
        {
            this.forceTextureLoad(tile);
            if (tile.isTextureInMemory(dc.getTextureCache()))
            {
                this.addTileToCurrent(tile);
                return;
            }
        }

        // Tile's texture isn't available, so request it
        if (tile.getLevelNumber() < this.levels.getNumLevels())
        {
            // Request only tiles with data associated at this level
            if (!this.levels.isResourceAbsent(tile))
                this.requestTexture(dc, tile);
        }

        // Set up to use the currentResource tile's texture
        if (this.currentResourceTile != null)
        {
            if (this.currentResourceTile.getLevelNumber() == 0 && this.forceLevelZeroLoads &&
                !this.currentResourceTile.isTextureInMemory(dc.getTextureCache()) &&
                !this.currentResourceTile.isTextureInMemory(dc.getTextureCache()))
                this.forceTextureLoad(this.currentResourceTile);

            if (this.currentResourceTile.isTextureInMemory(dc.getTextureCache()))
            {
                tile.setFallbackTile(currentResourceTile);
                this.addTileToCurrent(tile);
            }
        }
    }

    private void addTileToCurrent(TextureTile tile)
    {
        this.currentTiles.add(tile);
    }

    private boolean isTileVisible(DrawContext dc, TextureTile tile)
    {
//        if (!(tile.getExtent(dc).intersects(dc.getView().getFrustumInModelCoordinates())
//            && (dc.getVisibleSector() == null || dc.getVisibleSector().intersects(tile.getSector()))))
//            return false;
//
//        Position eyePos = dc.getView().getEyePosition();
//        LatLon centroid = tile.getSector().getCentroid();
//        Angle d = LatLon.greatCircleDistance(eyePos, centroid);
//        if ((!tile.getLevelName().equals("0")) && d.compareTo(tile.getSector().getDeltaLat().multiply(2.5)) == 1)
//            return false;
//
//        return true;
//
        return tile.getExtent(dc).intersects(dc.getView().getFrustumInModelCoordinates()) &&
            (dc.getVisibleSector() == null || dc.getVisibleSector().intersects(tile.getSector()));
    }
//
//    private boolean meetsRenderCriteria2(DrawContext dc, TextureTile tile)
//    {
//        if (this.levels.isFinalLevel(tile.getLevelNumber()))
//            return true;
//
//        Sector sector = tile.getSector();
//        Vec4[] corners = sector.computeCornerPoints(dc.getGlobe());
//        Vec4 centerPoint = sector.computeCenterPoint(dc.getGlobe());
//
//        View view = dc.getView();
//        double d1 = view.getEyePoint().distanceTo3(corners[0]);
//        double d2 = view.getEyePoint().distanceTo3(corners[1]);
//        double d3 = view.getEyePoint().distanceTo3(corners[2]);
//        double d4 = view.getEyePoint().distanceTo3(corners[3]);
//        double d5 = view.getEyePoint().distanceTo3(centerPoint);
//
//        double minDistance = d1;
//        if (d2 < minDistance)
//            minDistance = d2;
//        if (d3 < minDistance)
//            minDistance = d3;
//        if (d4 < minDistance)
//            minDistance = d4;
//        if (d5 < minDistance)
//            minDistance = d5;
//
//        double r = 0;
//        if (minDistance == d1)
//            r = corners[0].getLength3();
//        if (minDistance == d2)
//            r = corners[1].getLength3();
//        if (minDistance == d3)
//            r = corners[2].getLength3();
//        if (minDistance == d4)
//            r = corners[3].getLength3();
//        if (minDistance == d5)
//            r = centerPoint.getLength3();
//
//        double texelSize = tile.getLevel().getTexelSize(r);
//        double pixelSize = dc.getView().computePixelSizeAtDistance(minDistance);
//
//        return 2 * pixelSize >= texelSize;
//    }

    private boolean meetsRenderCriteria(DrawContext dc, TextureTile tile)
    {
        return this.levels.isFinalLevel(tile.getLevelNumber()) || !needToSplit(dc, tile.getSector());
    }

    private boolean needToSplit(DrawContext dc, Sector sector)
    {
        Vec4[] corners = sector.computeCornerPoints(dc.getGlobe(), dc.getVerticalExaggeration());
        Vec4 centerPoint = sector.computeCenterPoint(dc.getGlobe(), dc.getVerticalExaggeration());

        View view = dc.getView();
        double d1 = view.getEyePoint().distanceTo3(corners[0]);
        double d2 = view.getEyePoint().distanceTo3(corners[1]);
        double d3 = view.getEyePoint().distanceTo3(corners[2]);
        double d4 = view.getEyePoint().distanceTo3(corners[3]);
        double d5 = view.getEyePoint().distanceTo3(centerPoint);

        double minDistance = d1;
        if (d2 < minDistance)
            minDistance = d2;
        if (d3 < minDistance)
            minDistance = d3;
        if (d4 < minDistance)
            minDistance = d4;
        if (d5 < minDistance)
            minDistance = d5;

        double cellSize = (Math.PI * sector.getDeltaLatRadians() * dc.getGlobe().getRadius()) / 20; // TODO

        return !(Math.log10(cellSize) <= (Math.log10(minDistance) - this.splitScale));
    }

    private boolean atMaxLevel(DrawContext dc)
    {
        Position vpc = dc.getViewportCenterPosition();
        if (dc.getView() == null || this.getLevels() == null || vpc == null)
            return false;

        if (!this.getLevels().getSector().contains(vpc.getLatitude(), vpc.getLongitude()))
            return true;

        Level nextToLast = this.getLevels().getNextToLastLevel();
        if (nextToLast == null)
            return true;

        Sector centerSector = nextToLast.computeSectorForPosition(vpc.getLatitude(), vpc.getLongitude(),
            this.levels.getTileOrigin());
        return this.needToSplit(dc, centerSector);
    }

    // ============== Rendering ======================= //
    // ============== Rendering ======================= //
    // ============== Rendering ======================= //

    @Override
    public void render(DrawContext dc)
    {
        this.atMaxResolution = this.atMaxLevel(dc);
        super.render(dc);
    }

    @Override
    protected final void doRender(DrawContext dc)
    {
        if (this.forceLevelZeroLoads && !this.levelZeroLoaded)
            this.loadAllTopLevelTextures(dc);
        if (dc.getSurfaceGeometry() == null || dc.getSurfaceGeometry().size() < 1)
            return;

        dc.getGeographicSurfaceTileRenderer().setShowImageTileOutlines(this.showImageTileOutlines);

        draw(dc);
    }

    private void draw(DrawContext dc)
    {
        this.checkResources(); // Check whether resources are present, and perform any necessary initialization.

        this.assembleTiles(dc); // Determine the tiles to draw.

        if (this.currentTiles.size() >= 1)
        {
            if (this.getScreenCredit() != null)
            {
                dc.addScreenCredit(this.getScreenCredit());
            }

            TextureTile[] sortedTiles = new TextureTile[this.currentTiles.size()];
            sortedTiles = this.currentTiles.toArray(sortedTiles);
            Arrays.sort(sortedTiles, levelComparer);

            GL gl = dc.getGL();

            if (this.isUseTransparentTextures() || this.getOpacity() < 1)
            {
                gl.glPushAttrib(GL.GL_COLOR_BUFFER_BIT | GL.GL_POLYGON_BIT | GL.GL_CURRENT_BIT);
                this.setBlendingFunction(dc);
            }
            else
            {
                gl.glPushAttrib(GL.GL_COLOR_BUFFER_BIT | GL.GL_POLYGON_BIT);
            }

            gl.glPolygonMode(GL.GL_FRONT, GL.GL_FILL);
            gl.glEnable(GL.GL_CULL_FACE);
            gl.glCullFace(GL.GL_BACK);

            dc.setPerFrameStatistic(PerformanceStatistic.IMAGE_TILE_COUNT, this.tileCountName,
                this.currentTiles.size());
            dc.getGeographicSurfaceTileRenderer().renderTiles(dc, this.currentTiles);

            gl.glPopAttrib();

            if (this.drawTileIDs)
                this.drawTileIDs(dc, this.currentTiles);

            if (this.drawBoundingVolumes)
                this.drawBoundingVolumes(dc, this.currentTiles);

            // Check texture expiration. Memory-cached textures are checked for expiration only when an explicit,
            // non-zero expiry time has been set for the layer. If none has been set, the expiry times of the layer's
            // individual levels are used, but only for images in the local file cache, not textures in memory. This is
            // to avoid incurring the overhead of checking expiration of in-memory textures, a very rarely used feature.
            if (this.getExpiryTime() > 0 && this.getExpiryTime() < System.currentTimeMillis())
                this.checkTextureExpiration(dc, this.currentTiles);

            this.currentTiles.clear();
        }

        this.sendRequests();
        this.requestQ.clear();
    }

    private void checkTextureExpiration(DrawContext dc, List<TextureTile> tiles)
    {
        for (TextureTile tile : tiles)
        {
            if (tile.isTextureExpired())
                this.requestTexture(dc, tile);
        }
    }

    protected void setBlendingFunction(DrawContext dc)
    {
        // Set up a premultiplied-alpha blending function. Any texture read by JOGL will have alpha-premultiplied color
        // components, as will any DDS file created by World Wind or the World Wind WMS. We'll also set up the base
        // color as a premultiplied color, so that any incoming premultiplied color will be properly combined with the
        // base color.

        GL gl = dc.getGL();

        double alpha = this.getOpacity();
        gl.glColor4d(alpha, alpha, alpha, alpha);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void sendRequests()
    {
        Runnable task = this.requestQ.poll();
        while (task != null)
        {
            if (!WorldWind.getTaskService().isFull())
            {
                WorldWind.getTaskService().addTask(task);
            }
            task = this.requestQ.poll();
        }
    }

    public boolean isLayerInView(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (dc.getView() == null)
        {
            String message = Logging.getMessage("layers.AbstractLayer.NoViewSpecifiedInDrawingContext");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        return !(dc.getVisibleSector() != null && !this.levels.getSector().intersects(dc.getVisibleSector()));
    }

    private Vec4 computeReferencePoint(DrawContext dc)
    {
        if (dc.getViewportCenterPosition() != null)
            return dc.getGlobe().computePointFromPosition(dc.getViewportCenterPosition());

        java.awt.geom.Rectangle2D viewport = dc.getView().getViewport();
        int x = (int) viewport.getWidth() / 2;
        for (int y = (int) (0.5 * viewport.getHeight()); y >= 0; y--)
        {
            Position pos = dc.getView().computePositionFromScreenPoint(x, y);
            if (pos == null)
                continue;

            return dc.getGlobe().computePointFromPosition(pos.getLatitude(), pos.getLongitude(), 0d);
        }

        return null;
    }

    protected Vec4 getReferencePoint(DrawContext dc)
    {
        return this.computeReferencePoint(dc);
    }

    private static class LevelComparer implements Comparator<TextureTile>
    {
        public int compare(TextureTile ta, TextureTile tb)
        {
            int la = ta.getFallbackTile() == null ? ta.getLevelNumber() : ta.getFallbackTile().getLevelNumber();
            int lb = tb.getFallbackTile() == null ? tb.getLevelNumber() : tb.getFallbackTile().getLevelNumber();

            return la < lb ? -1 : la == lb ? 0 : 1;
        }
    }

    private void drawTileIDs(DrawContext dc, ArrayList<TextureTile> tiles)
    {
        java.awt.Rectangle viewport = dc.getView().getViewport();
        TextRenderer textRenderer = OGLTextRenderer.getOrCreateTextRenderer(dc.getTextRendererCache(),
            java.awt.Font.decode("Arial-Plain-13"));

        dc.getGL().glDisable(GL.GL_DEPTH_TEST);
        dc.getGL().glDisable(GL.GL_BLEND);
        dc.getGL().glDisable(GL.GL_TEXTURE_2D);

        textRenderer.beginRendering(viewport.width, viewport.height);
        textRenderer.setColor(java.awt.Color.YELLOW);
        for (TextureTile tile : tiles)
        {
            String tileLabel = tile.getLabel();

            if (tile.getFallbackTile() != null)
                tileLabel += "/" + tile.getFallbackTile().getLabel();

            LatLon ll = tile.getSector().getCentroid();
            Vec4 pt = dc.getGlobe().computePointFromPosition(ll.getLatitude(), ll.getLongitude(),
                dc.getGlobe().getElevation(ll.getLatitude(), ll.getLongitude()));
            pt = dc.getView().project(pt);
            textRenderer.draw(tileLabel, (int) pt.x, (int) pt.y);
        }
        textRenderer.setColor(java.awt.Color.WHITE);
        textRenderer.endRendering();
    }

    private void drawBoundingVolumes(DrawContext dc, ArrayList<TextureTile> tiles)
    {
        float[] previousColor = new float[4];
        dc.getGL().glGetFloatv(GL.GL_CURRENT_COLOR, previousColor, 0);
        dc.getGL().glColor3d(0, 1, 0);

        for (TextureTile tile : tiles)
        {
            ((Cylinder) tile.getExtent(dc)).render(dc);
        }

        Cylinder c =
            dc.getGlobe().computeBoundingCylinder(dc.getVerticalExaggeration(), this.levels.getSector());
        dc.getGL().glColor3d(1, 1, 0);
        c.render(dc);

        dc.getGL().glColor4fv(previousColor, 0);
    }

    // ============== Image Composition ======================= //
    // ============== Image Composition ======================= //
    // ============== Image Composition ======================= //

    public ArrayList<String> getAvailableImageFormats()
    {
        return new ArrayList<String>(this.supportedImageFormats);
    }

    public boolean isImageFormatAvailable(String imageFormat)
    {
        return imageFormat != null && this.supportedImageFormats.contains(imageFormat);
    }

    public String getDefaultImageFormat()
    {
        return this.supportedImageFormats.size() > 0 ? this.supportedImageFormats.get(0) : null;
    }

    protected void setAvailableImageFormats(String[] formats)
    {
        this.supportedImageFormats.clear();

        if (formats != null)
            this.supportedImageFormats.addAll(Arrays.asList(formats));
    }

    protected BufferedImage requestImage(TextureTile tile, String mimeType)
        throws URISyntaxException, InterruptedIOException, MalformedURLException
    {
        String pathBase = tile.getPathBase();
        String suffix = WWIO.makeSuffixForMimeType(mimeType);
        String path = pathBase + suffix;
        File f = new File(path);
        URL url;
        if (f.isAbsolute() && f.exists())
            url = f.toURI().toURL();
        else
            url = this.getDataFileStore().findFile(path, false);

        if (url == null) // image is not local
            return null;

        if (WWIO.isFileOutOfDate(url, tile.getLevel().getExpiryTime()))
        {
            // The file has expired. Delete it.
            this.getDataFileStore().removeFile(url);
            String message = Logging.getMessage("generic.DataFileExpired", url);
            Logging.logger().fine(message);
        }
        else
        {
            try
            {
                File imageFile = new File(url.toURI());
                BufferedImage image = ImageIO.read(imageFile);
                if (image == null)
                {
                    String message = Logging.getMessage("generic.ImageReadFailed", imageFile);
                    throw new RuntimeException(message);
                }

                this.levels.unmarkResourceAbsent(tile);
                return image;
            }
            catch (InterruptedIOException e)
            {
                throw e;
            }
            catch (IOException e)
            {
                // Assume that something's wrong with the file and delete it.
                this.getDataFileStore().removeFile(url);
                this.levels.markResourceAbsent(tile);
                String message = Logging.getMessage("generic.DeletedCorruptDataFile", url);
                Logging.logger().info(message);
            }
        }

        return null;
    }

    protected void downloadImage(final TextureTile tile, String mimeType, int timeout) throws Exception
    {
        final URL resourceURL = tile.getResourceURL(mimeType); // TODO: check for null
        Retriever retriever;

        String protocol = resourceURL.getProtocol();

        if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol))
        {
            retriever = new HTTPRetriever(resourceURL, new HttpRetrievalPostProcessor(tile));
        }
        else
        {
            String message = Logging.getMessage("layers.TextureLayer.UnknownRetrievalProtocol", resourceURL);
            throw new RuntimeException(message);
        }

        Logging.logger().log(java.util.logging.Level.FINE, "Retrieving " + resourceURL.toString());
        retriever.setConnectTimeout(10000);
        retriever.setReadTimeout(timeout);
        retriever.call();
    }

    public int computeLevelForResolution(Sector sector, double resolution)
    {
        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        // Find the first level exceeding the desired resolution
        double texelSize;
        Level targetLevel = this.levels.getLastLevel();
        for (int i = 0; i < this.getLevels().getLastLevel().getLevelNumber(); i++)
        {
            if (this.levels.isLevelEmpty(i))
                continue;

            texelSize = this.levels.getLevel(i).getTexelSize();
            if (texelSize > resolution)
                continue;

            targetLevel = this.levels.getLevel(i);
            break;
        }

        // Choose the level closest to the resolution desired
        if (targetLevel.getLevelNumber() != 0 && !this.levels.isLevelEmpty(targetLevel.getLevelNumber() - 1))
        {
            Level nextLowerLevel = this.levels.getLevel(targetLevel.getLevelNumber() - 1);
            double dless = Math.abs(nextLowerLevel.getTexelSize() - resolution);
            double dmore = Math.abs(targetLevel.getTexelSize() - resolution);
            if (dless < dmore)
                targetLevel = nextLowerLevel;
//            if (targetLevel == nextLowerLevel)
//                System.out.printf("REDUCED LEVEL to %d, ts = %f, ts next = %f\n", targetLevel.getLevelNumber(),
//                    targetLevel.getTexelSize(), this.levels.getLevel(targetLevel.getLevelNumber() + 1).getTexelSize());
        }

        Logging.logger().fine(Logging.getMessage("layers.TiledImageLayer.LevelSelection",
            targetLevel.getLevelNumber(), Double.toString(targetLevel.getTexelSize())));
        return targetLevel.getLevelNumber();
    }
//
//    private static class ComposeImageTextureTile extends TextureTile
//    {
//        private int width;
//        private int height;
//        private String path;
//
//        public ComposeImageTextureTile(Sector sector, String mimeType, Level level, int width, int height)
//            throws IOException
//        {
//            super(sector, level, 0, 0);
//
//            this.width = width;
//            this.height = height;
//            this.path = File.createTempFile("LandPrintImage", WWIO.makeSuffixForMimeType(mimeType)).getPath();
//        }
//
//        public int getWidth()
//        {
//            return this.width;
//        }
//
//        public int getHeight()
//        {
//            return this.height;
//        }
//
//        @Override
//        public String getPath()
//        {
//            return this.path;
//        }
//    }
//
//    public BufferedImage composeImageForSector2(Sector sector, int canvasWidth, int canvasHeight, double aspectRatio,
//        int levelNumber, String mimeType, boolean abortOnError, BufferedImage image) throws Exception
//    {
//        if (sector == null)
//        {
//            String message = Logging.getMessage("nullValue.SectorIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }
//
//        TextureTile tile = new ComposeImageTextureTile(sector, mimeType, this.levels.getLastLevel(), canvasWidth, canvasHeight);
//
//        if (image == null)
//            image = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
//
//        BufferedImage tileImage;
//        try
//        {
//            tileImage = this.getImage(tile, mimeType);
//            Thread.sleep(1); // generates InterruptedException if thread has been interupted
//            ImageUtil.mergeImage(sector, tile.getSector(), aspectRatio, tileImage, image);
//            this.firePropertyChange(AVKey.PROGRESS, 0d, 1d);
//        }
//        catch (InterruptedException e)
//        {
//            throw e;
//        }
//        catch (InterruptedIOException e)
//        {
//            throw e;
//        }
//        catch (Exception e)
//        {
//            if (abortOnError)
//                throw e;
//
//            String message = Logging.getMessage("generic.ExceptionWhileRequestingImage", tile.getPath());
//            Logging.logger().log(java.util.logging.Level.WARNING, message, e);
//        }
//
//        return image;
//    }

    /**
     * Create an image for the portion of this layer lying within a specified sector. The image is created at a
     * specified aspect ratio within a canvas of a specified size.
     *
     * @param sector       the sector of interest.
     * @param canvasWidth  the width of the canvas.
     * @param canvasHeight the height of the canvas.
     * @param aspectRatio  the aspect ratio, width/height, of the window. If the aspect ratio is greater or equal to
     *                     one, the full width of the canvas is used for the image; the height used is proportional to
     *                     the inverse of the aspect ratio. If the aspect ratio is less than one, the full height of the
     *                     canvas is used, and the width used is proportional to the aspect ratio.
     * @param levelNumber  the target level of the tiled image layer.
     * @param mimeType     the type of image to create, e.g., "png" and "jpg".
     * @param abortOnError indicates whether to stop assembling the image if an error occurs. If false, processing
     *                     continues until all portions of the layer that intersect the specified sector have been added
     *                     to the image. Portions for which an error occurs will be blank.
     * @param image        if non-null, a {@link BufferedImage} in which to place the image. If null, a new buffered
     *                     image is created. The image must be the width and height specified in the
     *                     <code>canvasWidth</code> and <code>canvasHeight</code> arguments.
     * @param timeout      The amount of time to allow for reading the image from the server.
     *
     * @return image        the assembelled image, of size indicated by the <code>canvasWidth</code> and
     *         <code>canvasHeight</code>. If the specified aspect ratio is one, all pixels contain values. If the aspect
     *         ratio is greater than one, a full-width segment along the top of the canvas is blank. If the aspect ratio
     *         is less than one, a full-height segment along the right side of the canvase is blank. If the
     *         <code>image</code> argument was non-null, that buffered image is returned.
     *
     * @throws IllegalArgumentException if <code>sector</code> is null.
     * @see ImageUtil#mergeImage ;
     */
    public BufferedImage composeImageForSector(Sector sector, int canvasWidth, int canvasHeight, double aspectRatio,
        int levelNumber, String mimeType, boolean abortOnError, BufferedImage image, int timeout) throws Exception
    {
        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (levelNumber < 0)
        {
            levelNumber = this.levels.getLastLevel().getLevelNumber();
        }
        else if (levelNumber > this.levels.getLastLevel().getLevelNumber())
        {
            Logging.logger().warning(Logging.getMessage("generic.LevelRequestedGreaterThanMaxLevel",
                levelNumber, this.levels.getLastLevel().getLevelNumber()));
            levelNumber = this.levels.getLastLevel().getLevelNumber();
        }

        int numTiles = 0;
        TextureTile[][] tiles = this.getTilesInSector(sector, levelNumber);
        for (TextureTile[] row : tiles)
        {
            numTiles += row.length;
        }

        if (tiles.length == 0 || tiles[0].length == 0)
        {
            Logging.logger().severe(Logging.getMessage("layers.TiledImageLayer.NoImagesAvailable"));
            return null;
        }

        if (image == null)
            image = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);

        double tileCount = 0;
        for (TextureTile[] row : tiles)
        {
            for (TextureTile tile : row)
            {
                if (tile == null)
                    continue;

                BufferedImage tileImage;
                try
                {
                    tileImage = this.getImage(tile, mimeType, timeout);
                    Thread.sleep(1); // generates InterruptedException if thread has been interupted
                    ImageUtil.mergeImage(sector, tile.getSector(), aspectRatio, tileImage, image);
                    this.firePropertyChange(AVKey.PROGRESS, tileCount / numTiles, ++tileCount / numTiles);
                }
                catch (InterruptedException e)
                {
                    throw e;
                }
                catch (InterruptedIOException e)
                {
                    throw e;
                }
                catch (Exception e)
                {
                    if (abortOnError)
                        throw e;

                    String message = Logging.getMessage("generic.ExceptionWhileRequestingImage", tile.getPath());
                    Logging.logger().log(java.util.logging.Level.WARNING, message, e);
                }
            }
        }

        return image;
    }

    public int countImagesInSector(Sector sector)
    {
        int count = 0;
        for (int i = 0; i <= this.getLevels().getLastLevel().getLevelNumber(); i++)
        {
            if (!this.levels.isLevelEmpty(i))
                count += countImagesInSector(sector, i);
        }
        return count;
    }

    public int countImagesInSector(Sector sector, int levelNumber)
    {
        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Level targetLevel = this.levels.getLastLevel();
        if (levelNumber >= 0)
        {
            for (int i = levelNumber; i < this.getLevels().getLastLevel().getLevelNumber(); i++)
            {
                if (this.levels.isLevelEmpty(i))
                    continue;

                targetLevel = this.levels.getLevel(i);
                break;
            }
        }

        // Collect all the tiles intersecting the input sector.
        LatLon delta = targetLevel.getTileDelta();
        LatLon origin = this.levels.getTileOrigin();
        final int nwRow = Tile.computeRow(delta.getLatitude(), sector.getMaxLatitude(), origin.getLatitude());
        final int nwCol = Tile.computeColumn(delta.getLongitude(), sector.getMinLongitude(), origin.getLongitude());
        final int seRow = Tile.computeRow(delta.getLatitude(), sector.getMinLatitude(), origin.getLatitude());
        final int seCol = Tile.computeColumn(delta.getLongitude(), sector.getMaxLongitude(), origin.getLongitude());

        int numRows = nwRow - seRow + 1;
        int numCols = seCol - nwCol + 1;

        return numRows * numCols;
    }

    public TextureTile[][] getTilesInSector(Sector sector, int levelNumber)
    {
        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Level targetLevel = this.levels.getLastLevel();
        if (levelNumber >= 0)
        {
            for (int i = levelNumber; i < this.getLevels().getLastLevel().getLevelNumber(); i++)
            {
                if (this.levels.isLevelEmpty(i))
                    continue;

                targetLevel = this.levels.getLevel(i);
                break;
            }
        }

        // Collect all the tiles intersecting the input sector.
        LatLon delta = targetLevel.getTileDelta();
        LatLon origin = this.levels.getTileOrigin();
        final int nwRow = Tile.computeRow(delta.getLatitude(), sector.getMaxLatitude(), origin.getLatitude());
        final int nwCol = Tile.computeColumn(delta.getLongitude(), sector.getMinLongitude(), origin.getLongitude());
        final int seRow = Tile.computeRow(delta.getLatitude(), sector.getMinLatitude(), origin.getLatitude());
        final int seCol = Tile.computeColumn(delta.getLongitude(), sector.getMaxLongitude(), origin.getLongitude());

        int numRows = nwRow - seRow + 1;
        int numCols = seCol - nwCol + 1;
        TextureTile[][] sectorTiles = new TextureTile[numRows][numCols];

        for (int row = nwRow; row >= seRow; row--)
        {
            for (int col = nwCol; col <= seCol; col++)
            {
                TileKey key = new TileKey(targetLevel.getLevelNumber(), row, col, targetLevel.getCacheName());
                Sector tileSector = this.levels.computeSectorForKey(key);
                sectorTiles[nwRow - row][col - nwCol] = new TextureTile(tileSector, targetLevel, row, col);
            }
        }

        return sectorTiles;
    }

    private BufferedImage getImage(TextureTile tile, String mimeType, int timeout) throws Exception
    {
        // Read the image from disk.
        BufferedImage image = this.requestImage(tile, mimeType);
        Thread.sleep(1); // generates InterruptedException if thread has been interupted
        if (image != null)
            return image;

        // Retrieve it from the net since it's not on disk.
        this.downloadImage(tile, mimeType, timeout);

        // Try to read from disk again after retrieving it from the net.
        image = this.requestImage(tile, mimeType);
        Thread.sleep(1); // generates InterruptedException if thread has been interupted
        if (image == null)
        {
            String message =
                Logging.getMessage("layers.TiledImageLayer.ImageUnavailable", tile.getPath());
            throw new RuntimeException(message);
        }

        return image;
    }

    protected class HttpRetrievalPostProcessor implements RetrievalPostProcessor
    {
        protected TextureTile tile;

        public HttpRetrievalPostProcessor(TextureTile tile)
        {
            this.tile = tile;
        }

        public ByteBuffer run(Retriever retriever)
        {
            if (!retriever.getState().equals(Retriever.RETRIEVER_STATE_SUCCESSFUL))
                return null;

            HTTPRetriever htr = (HTTPRetriever) retriever;
            if (htr.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT)
            {
                // Mark tile as missing to avoid excessive attempts
                TiledImageLayer.this.levels.markResourceAbsent(tile);
                return null;
            }

            if (htr.getResponseCode() != HttpURLConnection.HTTP_OK)
                return null;

            URLRetriever r = (URLRetriever) retriever;
            ByteBuffer buffer = r.getBuffer();

            String suffix = WWIO.makeSuffixForMimeType(htr.getContentType());
            if (suffix == null)
            {
                return null; // TODO: log error
            }

            String path = tile.getPathBase();
            path += suffix;

            File f = new File(path);
            final File outFile = f.isAbsolute() ? f : getDataFileStore().newFile(path);
            if (outFile == null)
                return null;

            if (outFile.getPath().contains(WWIO.DELETE_ON_EXIT_PREFIX))
                outFile.deleteOnExit();

            try
            {
                WWIO.saveBuffer(buffer, outFile);
                return buffer;
            }
            catch (ClosedByInterruptException e)
            {
                Logging.logger().log(java.util.logging.Level.FINE,
                    Logging.getMessage("generic.OperationCancelled", "image retrieval"), e);
            }
            catch (IOException e)
            {
                e.printStackTrace(); // TODO: log error
            }

            return null;
        }
    }
}
