/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.terrain;

import com.sun.opengl.util.BufferUtil;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.pick.*;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.*;
import java.awt.*;
import java.nio.*;
import java.util.*;
import java.util.List;

/**
 * @author tag
 * @version $Id: RectangularTessellator.java 12775 2009-11-09 18:33:47Z tgaskins $
 */
public class RectangularTessellator extends WWObjectImpl implements Tessellator
{
    protected static class RenderInfo
    {
        private final int density;
        private final Vec4 referenceCenter;
        private final DoubleBuffer vertices;
        private final DoubleBuffer texCoords;
        private final IntBuffer indices;
        private final long time;

        private final Integer bufferIdVertices;
        private final Integer bufferIdIndicies;
        private final Integer bufferIdTexCoords;

        private RenderInfo(DrawContext dc, int density, DoubleBuffer vertices, Integer verticesBuffer, Vec4 refCenter)
        {
            //Fill in the buffers and buffer IDs and store them in hashmaps by density
            processIndices(dc, density);
            processTextureCoordinates(dc, density);

            //Fill in the member variables from the parameters
            this.density = density;
            this.referenceCenter = refCenter;
            this.vertices = vertices;
            this.bufferIdVertices = verticesBuffer;

            //Fill in the remaining variables from the stored buffers and buffer IDs for easier access
            this.indices = indexLists.get(this.density);
            this.bufferIdIndicies = indexBuffers.get(this.density);

            this.texCoords = parameterizations.get(this.density);
            this.bufferIdTexCoords = parameterizationsBuffers.get(this.density);
            this.time = System.currentTimeMillis();
        }

        private long getSizeInBytes()
        {
            // Texture coordinates are shared among all tiles of the same density, so do not count towards size.
            // 8 references, doubles in buffer.
            return 8 * 4 + (this.vertices.limit()) * Double.SIZE;
        }
    }

    private static class RectTile implements SectorGeometry
    {
        private final RectangularTessellator tessellator;
        private final int level;
        private final Sector sector;
        private final int density;
        private final double log10CellSize;
        private Extent extent; // extent of sector in object coordinates
        private RenderInfo ri;

        private int minColorCode = 0;
        private int maxColorCode = 0;

        public RectTile(RectangularTessellator tessellator, Extent extent, int level, int density, Sector sector,
            double cellSize)
        {
            this.tessellator = tessellator;
            this.level = level;
            this.density = density;
            this.sector = sector;
            this.extent = extent;
            this.log10CellSize = Math.log10(cellSize);
        }

        public Sector getSector()
        {
            return this.sector;
        }

        public Extent getExtent()
        {
            return this.extent;
        }

        public void renderMultiTexture(DrawContext dc, int numTextureUnits)
        {
            this.tessellator.renderMultiTexture(dc, this, numTextureUnits);
        }

        public void render(DrawContext dc)
        {
            this.tessellator.render(dc, this);
        }

        public void renderWireframe(DrawContext dc, boolean showTriangles, boolean showTileBoundary)
        {
            this.tessellator.renderWireframe(dc, this, showTriangles, showTileBoundary);
        }

        public void renderBoundingVolume(DrawContext dc)
        {
            this.tessellator.renderBoundingVolume(dc, this);
        }

        public PickedObject[] pick(DrawContext dc, List<? extends Point> pickPoints)
        {
            return this.tessellator.pick(dc, this, pickPoints);
        }

        public void pick(DrawContext dc, Point pickPoint)
        {
            this.tessellator.pick(dc, this, pickPoint);
        }

        public Vec4 getSurfacePoint(Angle latitude, Angle longitude, double metersOffset)
        {
            return this.tessellator.getSurfacePoint(this, latitude, longitude, metersOffset);
        }

        public double getResolution()
        {
            return this.sector.getDeltaLatRadians() / this.density;
        }

        public Intersection[] intersect(Line line)
        {
            return this.tessellator.intersect(this, line);
        }

        public Intersection[] intersect(double elevation)
        {
            return this.tessellator.intersect(this, elevation);
        }

        public DoubleBuffer makeTextureCoordinates(GeographicTextureCoordinateComputer computer)
        {
            return this.tessellator.makeGeographicTexCoords(this, computer);
        }

        public ExtractedShapeDescription getIntersectingTessellationPieces(Plane[] p)
        {
            return this.tessellator.getIntersectingTessellationPieces(this, p);
        }

        public ExtractedShapeDescription getIntersectingTessellationPieces(Vec4 Cxyz,
            Vec4 uHat, Vec4 vHat, double uRadius,
            double vRadius)
        {
            return this.tessellator.getIntersectingTessellationPieces(this, Cxyz,
                uHat, vHat, uRadius, vRadius);
        }
    }

    private static class CacheKey
    {
        private final Sector sector;
        private final int density;
        private final Object globeStateKey;

        public CacheKey(DrawContext dc, Sector sector, int density)
        {
            this.sector = sector;
            this.density = density;
            this.globeStateKey = dc.getGlobe().getStateKey(dc);
        }

        @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
        public boolean equals(Object o)
        {
            if (this == o)
                return true;

            CacheKey cacheKey = (CacheKey) o; // Note: no check of class type equivalence, for performance

            if (density != cacheKey.density)
                return false;
            if (globeStateKey != null ? !globeStateKey.equals(cacheKey.globeStateKey) : cacheKey.globeStateKey != null)
                return false;
            //noinspection RedundantIfStatement
            if (sector != null ? !sector.equals(cacheKey.sector) : cacheKey.sector != null)
                return false;

            return true;
        }

        public int hashCode()
        {
            int result;
            result = (sector != null ? sector.hashCode() : 0);
            result = 31 * result + density;
            result = 31 * result + (globeStateKey != null ? globeStateKey.hashCode() : 0);
            return result;
        }
    }

    // TODO: Make all this configurable
    private static final int DEFAULT_MAX_LEVEL = 20;
    private static final double DEFAULT_LOG10_RESOLUTION_TARGET = 1.3;
    private static final int DEFAULT_NUM_LAT_SUBDIVISIONS = 5;
    private static final int DEFAULT_NUM_LON_SUBDIVISIONS = 10;
    private static final int DEFAULT_DENSITY = 20;
    private static final String CACHE_NAME = "Terrain";
    private static final String CACHE_ID = RectangularTessellator.class.getName();

    // Tri-strip indices and texture coordinates. These depend only on density and can therefore be statically cached.
    private static final HashMap<Integer, DoubleBuffer> parameterizations = new HashMap<Integer, DoubleBuffer>();
    private static final HashMap<Integer, IntBuffer> indexLists = new HashMap<Integer, IntBuffer>();
    private static final HashMap<Integer, ByteBuffer> oddRowColorList = new HashMap<Integer, ByteBuffer>();
    private static final HashMap<Integer, ByteBuffer> evenRowColorList = new HashMap<Integer, ByteBuffer>();

    private static final HashMap<Integer, Integer> parameterizationsBuffers = new HashMap<Integer, Integer>();
    private static final HashMap<Integer, Integer> indexBuffers = new HashMap<Integer, Integer>();
    private static final HashMap<Integer, Integer> oddRowColorBuffers = new HashMap<Integer, Integer>();
    private static final HashMap<Integer, Integer> evenRowColorBuffers = new HashMap<Integer, Integer>();

    private ArrayList<RectTile> topLevels;
    private PickSupport pickSupport = new PickSupport();
    private SectorGeometryList currentTiles = new SectorGeometryList();
    private Frustum currentFrustum;
    private Sector currentCoverage; // union of all tiles selected during call to render()
    private boolean makeTileSkirts = true;
    private int currentLevel;
    private int maxLevel = DEFAULT_MAX_LEVEL;
    private Globe globe;
    private int density = DEFAULT_DENSITY;

    public SectorGeometryList tessellate(DrawContext dc)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (dc.getView() == null)
        {
            String msg = Logging.getMessage("nullValue.ViewIsNull");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        if (!WorldWind.getMemoryCacheSet().containsCache(CACHE_ID))
        {
            long size = Configuration.getLongValue(AVKey.SECTOR_GEOMETRY_CACHE_SIZE, 20000000L);
            MemoryCache cache = new BasicMemoryCache((long) (0.85 * size), size);
            cache.setName(CACHE_NAME);

            //Add a cache listener to delete the vertices VBO when it is removed
            cache.addCacheListener(new MemoryCache.CacheListener()
            {
                public void entryRemoved(Object key, Object clientObject)
                {
                    RenderInfo ri = (RenderInfo) clientObject;

                    if (ri.bufferIdVertices != null)
                    {
                        GLContext glc = GLContext.getCurrent();

                        if (glc == null) // TODO: need to handle this case and release resources at some point
                        {
                            String msg = Logging.getMessage("OGL.CannotDeleteVBO");
                            Logging.logger().severe(msg);
                            return;
                        }

                        //Delete the old buffer
                        int glBuf[] = new int[1];
                        glBuf[0] = ri.bufferIdVertices;
                        glc.getGL().glDeleteBuffers(1, glBuf, 0);
                    }
                }
            });

            WorldWind.getMemoryCacheSet().addCache(CACHE_ID, cache);
        }

        this.maxLevel = Configuration.getIntegerValue(AVKey.RECTANGULAR_TESSELLATOR_MAX_LEVEL, DEFAULT_MAX_LEVEL);

        if (this.topLevels == null)
            this.topLevels = this.createTopLevelTiles(dc);

        this.currentTiles.clear();
        this.currentLevel = 0;
        this.currentCoverage = null;

        this.currentFrustum = dc.getView().getFrustumInModelCoordinates();
        for (RectTile tile : this.topLevels)
        {
            this.selectVisibleTiles(dc, tile);
        }

        this.currentTiles.setSector(this.currentCoverage);

        for (SectorGeometry tile : this.currentTiles)
        {
            this.makeVerts(dc, (RectTile) tile);
        }

        return this.currentTiles;
    }

    private ArrayList<RectTile> createTopLevelTiles(DrawContext dc)
    {
        ArrayList<RectTile> tops =
            new ArrayList<RectTile>(DEFAULT_NUM_LAT_SUBDIVISIONS * DEFAULT_NUM_LON_SUBDIVISIONS);

        this.globe = dc.getGlobe();
        double deltaLat = 180d / DEFAULT_NUM_LAT_SUBDIVISIONS;
        double deltaLon = 360d / DEFAULT_NUM_LON_SUBDIVISIONS;
        Angle lastLat = Angle.NEG90;

        for (int row = 0; row < DEFAULT_NUM_LAT_SUBDIVISIONS; row++)
        {
            Angle lat = lastLat.addDegrees(deltaLat);
            if (lat.getDegrees() + 1d > 90d)
                lat = Angle.POS90;

            Angle lastLon = Angle.NEG180;

            for (int col = 0; col < DEFAULT_NUM_LON_SUBDIVISIONS; col++)
            {
                Angle lon = lastLon.addDegrees(deltaLon);
                if (lon.getDegrees() + 1d > 180d)
                    lon = Angle.POS180;

                Sector tileSector = new Sector(lastLat, lat, lastLon, lon);
                tops.add(this.createTile(dc, tileSector, 0));
                lastLon = lon;
            }
            lastLat = lat;
        }

        return tops;
    }

    private RectTile createTile(DrawContext dc, Sector tileSector, int level)
    {
        Cylinder cylinder = dc.getGlobe().computeBoundingCylinder(dc.getVerticalExaggeration(), tileSector);
        double cellSize = tileSector.getDeltaLatRadians() * dc.getGlobe().getRadius() / this.density;

        return new RectTile(this, cylinder, level, this.density, tileSector, cellSize);
    }

    public boolean isMakeTileSkirts()
    {
        return makeTileSkirts;
    }

    public void setMakeTileSkirts(boolean makeTileSkirts)
    {
        this.makeTileSkirts = makeTileSkirts;
    }

    private void selectVisibleTiles(DrawContext dc, RectTile tile)
    {
        Extent extent = tile.getExtent();
        if (extent != null && !extent.intersects(this.currentFrustum))
            return;

        if (this.currentLevel < this.maxLevel - 1 && this.needToSplit(dc, tile))
        {
            ++this.currentLevel;
            RectTile[] subtiles = this.split(dc, tile);
            for (RectTile child : subtiles)
            {
                this.selectVisibleTiles(dc, child);
            }
            --this.currentLevel;
            return;
        }
        this.currentCoverage = tile.getSector().union(this.currentCoverage);
        this.currentTiles.add(tile);
    }

    private boolean needToSplit(DrawContext dc, RectTile tile)
    {
        Vec4[] corners = tile.sector.computeCornerPoints(dc.getGlobe(), dc.getVerticalExaggeration());
        Vec4 centerPoint = tile.sector.computeCenterPoint(dc.getGlobe(), dc.getVerticalExaggeration());

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

        double logDist = Math.log10(minDistance);
        double target = this.computeTileResolutionTarget(dc, tile);

        boolean useTile = tile.log10CellSize <= (logDist - target);
        return !useTile;
    }

    private double computeTileResolutionTarget(DrawContext dc, RectTile tile)
    {
        // Compute the log10 detail target for the specified tile. Apply the elevation model's detail hint to the
        // default detail target.

        return DEFAULT_LOG10_RESOLUTION_TARGET + dc.getGlobe().getElevationModel().getDetailHint(tile.sector);
    }

    private RectTile[] split(DrawContext dc, RectTile tile)
    {
        Sector[] sectors = tile.sector.subdivide();

        RectTile[] subTiles = new RectTile[4];
        subTiles[0] = this.createTile(dc, sectors[0], tile.level + 1);
        subTiles[1] = this.createTile(dc, sectors[1], tile.level + 1);
        subTiles[2] = this.createTile(dc, sectors[2], tile.level + 1);
        subTiles[3] = this.createTile(dc, sectors[3], tile.level + 1);

        return subTiles;
    }

    private RectangularTessellator.CacheKey createCacheKey(DrawContext dc, RectTile tile)
    {
        return new CacheKey(dc, tile.sector, tile.density);
    }

    private void makeVerts(DrawContext dc, RectTile tile)
    {
        // First see if the vertices have been previously computed and are in the cache. Since the elevation model
        // can change between frames, regenerate and re-cache vertices every second.
        // TODO: Go back to event-generated geometry recomputation.
        MemoryCache cache = WorldWind.getMemoryCache(CACHE_ID);
        CacheKey cacheKey = this.createCacheKey(dc, tile);
        tile.ri = (RenderInfo) cache.getObject(cacheKey);
        if (tile.ri != null && tile.ri.time >= System.currentTimeMillis() - 1000) // Regenerate cache after one second
            return;

        tile.ri = this.buildVerts(dc, tile, this.makeTileSkirts);
        if (tile.ri != null)
        {
            cacheKey = this.createCacheKey(dc, tile);
            cache.add(cacheKey, tile.ri, tile.ri.getSizeInBytes());
        }
    }

    public RenderInfo buildVerts(DrawContext dc, RectTile tile, boolean makeSkirts)
    {
        int density = tile.density;
        int numVertices = (density + 3) * (density + 3);

        DoubleBuffer verts;

        //Re-use the RenderInfo vertices buffer. If it has not been set or the density has changed, create a new buffer
        if (tile.ri == null || tile.ri.vertices == null || density != tile.ri.density)
        {
            verts = BufferUtil.newDoubleBuffer(numVertices * 3);
        }
        else
        {
            verts = tile.ri.vertices;
            verts.rewind();
        }

        ArrayList<LatLon> latlons = this.computeLocations(tile);
        double[] elevations = new double[latlons.size()];
        dc.getGlobe().getElevations(tile.sector, latlons, tile.getResolution(), elevations);

        int iv = 0;
        double verticalExaggeration = dc.getVerticalExaggeration();
        double exaggeratedMinElevation = makeSkirts ? globe.getMinElevation() * verticalExaggeration : 0;

        LatLon centroid = tile.sector.getCentroid();
        Vec4 refCenter = globe.computePointFromPosition(centroid.getLatitude(), centroid.getLongitude(), 0d);

        int ie = 0;
        Iterator<LatLon> latLonIter = latlons.iterator();
        for (int j = 0; j <= density + 2; j++)
        {
            for (int i = 0; i <= density + 2; i++)
            {
                LatLon latlon = latLonIter.next();
                double elevation = verticalExaggeration * elevations[ie++];

                if (j == 0 || j >= tile.density + 2 || i == 0 || i >= tile.density + 2)
                {   // use abs to account for negative elevation.
                    elevation -= exaggeratedMinElevation >= 0 ? exaggeratedMinElevation : -exaggeratedMinElevation;
                }

                Vec4 p = globe.computePointFromPosition(latlon.getLatitude(), latlon.getLongitude(), elevation);
                verts.put(iv++, p.x - refCenter.x).put(iv++, p.y - refCenter.y).put(iv++, p.z - refCenter.z);
            }
        }

        verts.rewind();

        Integer bufferIdVertices = null;

        //Vertex Buffer Objects are supported in versions 1.5 and greater
        if (dc.getGLRuntimeCapabilities().isUseVertexBufferObject())
        {
            GL gl = dc.getGL();

            OGLStackHandler ogsh = new OGLStackHandler();

            try
            {
                ogsh.pushClientAttrib(gl, GL.GL_CLIENT_VERTEX_ARRAY_BIT);

                //Create a new bufferId
                int glBuf[] = new int[1];
                gl.glGenBuffers(1, glBuf, 0);
                bufferIdVertices = glBuf[0];

                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferIdVertices);
                gl.glBufferData(GL.GL_ARRAY_BUFFER, verts.limit() * 8, verts, GL.GL_DYNAMIC_DRAW);
            }
            finally
            {
                ogsh.pop(gl);
            }
        }

        return new RenderInfo(dc, density, verts, bufferIdVertices, refCenter);
    }

    private ArrayList<LatLon> computeLocations(RectTile tile)
    {
        int density = tile.density;
        int numVertices = (density + 3) * (density + 3);

        Angle latMax = tile.sector.getMaxLatitude();
        Angle dLat = tile.sector.getDeltaLat().divide(density);
        Angle lat = tile.sector.getMinLatitude();

        Angle lonMin = tile.sector.getMinLongitude();
        Angle lonMax = tile.sector.getMaxLongitude();
        Angle dLon = tile.sector.getDeltaLon().divide(density);

        ArrayList<LatLon> latlons = new ArrayList<LatLon>(numVertices);
        for (int j = 0; j <= density + 2; j++)
        {
            Angle lon = lonMin;
            for (int i = 0; i <= density + 2; i++)
            {
                latlons.add(new LatLon(lat, lon));

                if (i > density)
                    lon = lonMax;
                else if (i != 0)
                    lon = lon.add(dLon);

                if (lon.degrees < -180)
                    lon = Angle.NEG180;
                else if (lon.degrees > 180)
                    lon = Angle.POS180;
            }

            if (j > density)
                lat = latMax;
            else if (j != 0)
                lat = lat.add(dLat);
        }

        return latlons;
    }

    private void renderMultiTexture(DrawContext dc, RectTile tile, int numTextureUnits)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (numTextureUnits < 1)
        {
            String msg = Logging.getMessage("generic.NumTextureUnitsLessThanOne");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.render(dc, tile, numTextureUnits);
    }

    private void render(DrawContext dc, RectTile tile)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.render(dc, tile, 1);
    }

    private long render(DrawContext dc, RectTile tile, int numTextureUnits)
    {
        if (tile.ri == null)
        {
            String msg = Logging.getMessage("nullValue.RenderInfoIsNull");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        dc.getView().pushReferenceCenter(dc, tile.ri.referenceCenter);

        GL gl = dc.getGL();
        OGLStackHandler ogsh = new OGLStackHandler();

        try
        {
            ogsh.pushClientAttrib(gl, GL.GL_CLIENT_VERTEX_ARRAY_BIT);

            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);

            if (dc.getGLRuntimeCapabilities().isUseVertexBufferObject())
            {
                //Use VBO's
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, tile.ri.bufferIdVertices);
                gl.glVertexPointer(3, GL.GL_DOUBLE, 0, 0);

                for (int i = 0; i < numTextureUnits; i++)
                {
                    gl.glClientActiveTexture(GL.GL_TEXTURE0 + i);
                    gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);

                    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, tile.ri.bufferIdTexCoords);
                    gl.glTexCoordPointer(2, GL.GL_DOUBLE, 0, 0);
                }

                gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, tile.ri.bufferIdIndicies);

                gl.glDrawElements(javax.media.opengl.GL.GL_TRIANGLE_STRIP, tile.ri.indices.limit(),
                    javax.media.opengl.GL.GL_UNSIGNED_INT, 0);
            }
            else
            {
                //Use Vertex Arrays
                gl.glVertexPointer(3, GL.GL_DOUBLE, 0, tile.ri.vertices.rewind());

                for (int i = 0; i < numTextureUnits; i++)
                {
                    gl.glClientActiveTexture(GL.GL_TEXTURE0 + i);
                    gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
                    Object texCoords = dc.getValue(AVKey.TEXTURE_COORDINATES);
                    if (texCoords != null && texCoords instanceof DoubleBuffer)
                        gl.glTexCoordPointer(2, GL.GL_DOUBLE, 0, ((DoubleBuffer) texCoords).rewind());
                    else
                        gl.glTexCoordPointer(2, GL.GL_DOUBLE, 0, tile.ri.texCoords.rewind());
                }

                gl.glDrawElements(javax.media.opengl.GL.GL_TRIANGLE_STRIP, tile.ri.indices.limit(),
                    javax.media.opengl.GL.GL_UNSIGNED_INT, tile.ri.indices.rewind());
            }
        }
        finally
        {
            ogsh.pop(gl);
        }
        dc.getView().popReferenceCenter(dc);

        return tile.ri.indices.limit() - 2; // return number of triangles rendered
    }

    private void renderWireframe(DrawContext dc, RectTile tile, boolean showTriangles, boolean showTileBoundary)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (tile.ri == null)
        {
            String msg = Logging.getMessage("nullValue.RenderInfoIsNull");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        dc.getView().pushReferenceCenter(dc, tile.ri.referenceCenter);

        javax.media.opengl.GL gl = dc.getGL();
        gl.glPushAttrib(
            GL.GL_DEPTH_BUFFER_BIT | GL.GL_POLYGON_BIT | GL.GL_TEXTURE_BIT | GL.GL_ENABLE_BIT | GL.GL_CURRENT_BIT);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);
        gl.glDisable(javax.media.opengl.GL.GL_DEPTH_TEST);
        gl.glEnable(javax.media.opengl.GL.GL_CULL_FACE);
        gl.glCullFace(javax.media.opengl.GL.GL_BACK);
        gl.glDisable(javax.media.opengl.GL.GL_TEXTURE_2D);
        gl.glColor4d(1d, 1d, 1d, 0.2);
        gl.glPolygonMode(javax.media.opengl.GL.GL_FRONT, javax.media.opengl.GL.GL_LINE);

        if (showTriangles)
        {
            OGLStackHandler ogsh = new OGLStackHandler();

            try
            {
                ogsh.pushClientAttrib(gl, GL.GL_CLIENT_VERTEX_ARRAY_BIT);

                gl.glEnableClientState(GL.GL_VERTEX_ARRAY);

                gl.glVertexPointer(3, GL.GL_DOUBLE, 0, tile.ri.vertices.rewind());
                gl.glDrawElements(javax.media.opengl.GL.GL_TRIANGLE_STRIP, tile.ri.indices.limit(),
                    javax.media.opengl.GL.GL_UNSIGNED_INT, tile.ri.indices.rewind());
            }
            finally
            {
                ogsh.pop(gl);
            }
        }

        dc.getView().popReferenceCenter(dc);

        if (showTileBoundary)
            this.renderPatchBoundary(dc, tile, gl);

        gl.glPopAttrib();
    }

    private void renderPatchBoundary(DrawContext dc, RectTile tile, GL gl)
    {
        // TODO: Currently only works if called from renderWireframe because no state is set here.
        // TODO: Draw the boundary using the vertices along the boundary rather than just at the corners.
        gl.glColor4d(1d, 0, 0, 1d);
        Vec4[] corners = tile.sector.computeCornerPoints(dc.getGlobe(), dc.getVerticalExaggeration());

        gl.glBegin(javax.media.opengl.GL.GL_QUADS);
        gl.glVertex3d(corners[0].x, corners[0].y, corners[0].z);
        gl.glVertex3d(corners[1].x, corners[1].y, corners[1].z);
        gl.glVertex3d(corners[2].x, corners[2].y, corners[2].z);
        gl.glVertex3d(corners[3].x, corners[3].y, corners[3].z);
        gl.glEnd();
    }

    private void renderBoundingVolume(DrawContext dc, RectTile tile)
    {
        Extent extent = tile.getExtent();
        if (extent == null)
            return;

        if (extent instanceof Cylinder)
            ((Cylinder) extent).render(dc);
    }

    private PickedObject[] pick(DrawContext dc, RectTile tile, List<? extends Point> pickPoints)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (pickPoints.size() == 0)
            return null;

        if (tile.ri == null)
            return null;

        PickedObject[] pos = new PickedObject[pickPoints.size()];
        this.renderTrianglesWithUniqueColors(dc, tile);
        for (int i = 0; i < pickPoints.size(); i++)
        {
            pos[i] = this.resolvePick(dc, tile, pickPoints.get(i));
        }

        return pos;
    }

    private void pick(DrawContext dc, RectTile tile, Point pickPoint)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (tile.ri == null)
            return;

        renderTrianglesWithUniqueColors(dc, tile);
        PickedObject po = this.resolvePick(dc, tile, pickPoint);
        if (po != null)
            dc.addPickedObject(po);
    }

    private void renderTrianglesWithUniqueColors(DrawContext dc, RectTile tile)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (tile.ri.vertices == null)
            return;

        tile.ri.vertices.rewind();
        tile.ri.indices.rewind();

        javax.media.opengl.GL gl = dc.getGL();

        if (null != tile.ri.referenceCenter)
            dc.getView().pushReferenceCenter(dc, tile.ri.referenceCenter);

        //Fill out the color buffers each frame with unique colors
        int sideSize = density + 2;
        int indexCount = 2 * sideSize * sideSize + 4 * sideSize - 2;
        int trianglesNum = indexCount - 2;
        int numVertices = (density + 3) * (density + 3);
        int verticesSize = numVertices * 3;

        ByteBuffer colorsOdd;
        ByteBuffer colorsEven;

        //Reuse the old color buffers if possible
        if (oddRowColorList.containsKey(density) && evenRowColorList.containsKey(density))
        {
            colorsOdd = oddRowColorList.get(density);
            colorsEven = evenRowColorList.get(density);
        }
        else
        {
            //Otherwise create new buffers
            colorsOdd = BufferUtil.newByteBuffer(verticesSize);
            colorsEven = BufferUtil.newByteBuffer(verticesSize);

            oddRowColorList.put(density, colorsOdd);
            evenRowColorList.put(density, colorsEven);
        }

        tile.minColorCode = dc.getUniquePickColor().getRGB();

        int prevPos = -1;
        int pos;

        for (int i = 0; i < trianglesNum; i++)
        {
            java.awt.Color color = dc.getUniquePickColor();

            //NOTE: Get the indices for the last point for the triangle (i+2).
            // The color of this point is used to fill the entire triangle with flat shading.
            pos = 3 * tile.ri.indices.get(i + 2);

            //Since we are using a single triangle strip for all rows, we need to store the colors in alternate rows.
            // (The same vertices are used in both directions, however, we need different colors for those vertices)
            if (pos > prevPos)
            {
                colorsOdd.position(pos);
                colorsOdd.put((byte) color.getRed()).put((byte) color.getGreen()).put((byte) color.getBlue());
            }
            else if (pos < prevPos)
            {
                colorsEven.position(pos);
                colorsEven.put((byte) color.getRed()).put((byte) color.getGreen()).put((byte) color.getBlue());
            }

            prevPos = pos;
        }

        tile.maxColorCode = dc.getUniquePickColor().getRGB();

        //Rewind the color buffers
        colorsOdd.rewind();
        colorsEven.rewind();

        OGLStackHandler ogsh = new OGLStackHandler();

        try
        {
            ogsh.pushClientAttrib(gl, GL.GL_CLIENT_VERTEX_ARRAY_BIT);

            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL.GL_COLOR_ARRAY);

            if (dc.getGLRuntimeCapabilities().isUseVertexBufferObject())
            {
                int glBuf[] = new int[2];

                //Reuse the color buffers if possible
                if (oddRowColorBuffers.containsKey(density) && evenRowColorBuffers.containsKey(density))
                {
                    glBuf[0] = oddRowColorBuffers.get(density);
                    glBuf[1] = evenRowColorBuffers.get(density);
                }
                else
                {
                    gl.glGenBuffers(2, glBuf, 0);

                    oddRowColorBuffers.put(density, glBuf[0]);
                    evenRowColorBuffers.put(density, glBuf[1]);
                }

                //Store the color data in a VBO
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, glBuf[0]);
                gl.glBufferData(GL.GL_ARRAY_BUFFER, colorsOdd.limit(), colorsOdd, GL.GL_DYNAMIC_DRAW);

                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, glBuf[1]);
                gl.glBufferData(GL.GL_ARRAY_BUFFER, colorsEven.limit(), colorsEven, GL.GL_DYNAMIC_DRAW);
            }

            //Render it

            if (dc.getGLRuntimeCapabilities().isUseVertexBufferObject())
            {
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, tile.ri.bufferIdVertices);
                gl.glVertexPointer(3, GL.GL_DOUBLE, 0, 0);

                int sideWidth = density + 2;
                int trianglesPerRow = sideWidth * 2 + 4;

                gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, tile.ri.bufferIdIndicies);

                //Draw the odd rows
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, oddRowColorBuffers.get(tile.ri.density));
                gl.glColorPointer(3, GL.GL_UNSIGNED_BYTE, 0, 0);

                for (int i = 0; i < sideWidth; i += 2)
                {
                    gl.glDrawElements(javax.media.opengl.GL.GL_TRIANGLE_STRIP, trianglesPerRow,
                        javax.media.opengl.GL.GL_UNSIGNED_INT, trianglesPerRow * i * 4);
                }

                //Draw the even rows
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, evenRowColorBuffers.get(tile.ri.density));
                gl.glColorPointer(3, GL.GL_UNSIGNED_BYTE, 0, 0);

                for (int i = 1; i < sideWidth - 1; i += 2)
                {
                    gl.glDrawElements(javax.media.opengl.GL.GL_TRIANGLE_STRIP, trianglesPerRow,
                        javax.media.opengl.GL.GL_UNSIGNED_INT, trianglesPerRow * i * 4);
                }
            }
            else
            {
                gl.glVertexPointer(3, GL.GL_DOUBLE, 0, tile.ri.vertices.rewind());

                tile.ri.indices.rewind();

                int sideWidth = density + 2;
                int trianglesPerRow = sideWidth * 2 + 4;

                //Draw the odd rows
                gl.glColorPointer(3, GL.GL_UNSIGNED_BYTE, 0, oddRowColorList.get(tile.ri.density).rewind());

                for (int i = 0; i < sideWidth; i += 2)
                {
                    gl.glDrawElements(javax.media.opengl.GL.GL_TRIANGLE_STRIP, trianglesPerRow,
                        javax.media.opengl.GL.GL_UNSIGNED_INT, tile.ri.indices.position(trianglesPerRow * i));
                }

                //Draw the even rows
                gl.glColorPointer(3, GL.GL_UNSIGNED_BYTE, 0, evenRowColorList.get(tile.ri.density).rewind());
                for (int i = 1; i < sideWidth - 1; i += 2)
                {
                    gl.glDrawElements(javax.media.opengl.GL.GL_TRIANGLE_STRIP, trianglesPerRow,
                        javax.media.opengl.GL.GL_UNSIGNED_INT, tile.ri.indices.position(trianglesPerRow * i));
                }
            }
        }
        finally
        {
            ogsh.pop(gl);
        }

        if (null != tile.ri.referenceCenter)
            dc.getView().popReferenceCenter(dc);
    }

    private PickedObject resolvePick(DrawContext dc, RectTile tile, Point pickPoint)
    {
        int colorCode = this.pickSupport.getTopColor(dc, pickPoint);
        if (colorCode < tile.minColorCode || colorCode > tile.maxColorCode)
            return null;

        double EPSILON = (double) 0.00001f;

        int triangleIndex = colorCode - tile.minColorCode - 1;

        if (tile.ri.indices == null || triangleIndex >= (tile.ri.indices.capacity() - 2))
            return null;

        double centerX = tile.ri.referenceCenter.x;
        double centerY = tile.ri.referenceCenter.y;
        double centerZ = tile.ri.referenceCenter.z;

        int[] indices = new int[3];
        tile.ri.indices.position(triangleIndex);
        tile.ri.indices.get(indices);

        double[] coords = new double[3];
        tile.ri.vertices.position(3 * indices[0]);
        tile.ri.vertices.get(coords);
        Vec4 v0 = new Vec4(coords[0] + centerX, coords[1] + centerY, coords[2] + centerZ);

        tile.ri.vertices.position(3 * indices[1]);
        tile.ri.vertices.get(coords);
        Vec4 v1 = new Vec4(coords[0] + centerX, coords[1] + centerY, coords[2] + centerZ);

        tile.ri.vertices.position(3 * indices[2]);
        tile.ri.vertices.get(coords);
        Vec4 v2 = new Vec4(coords[0] + centerX, coords[1] + centerY, coords[2] + centerZ);

        // get triangle edge vectors and plane normal
        Vec4 e1 = v1.subtract3(v0);
        Vec4 e2 = v2.subtract3(v0);
        Vec4 N = e1.cross3(e2);  // if N is 0, the triangle is degenerate, we are not dealing with it

        Line ray = dc.getView().computeRayFromScreenPoint(pickPoint.getX(), pickPoint.getY());

        Vec4 w0 = ray.getOrigin().subtract3(v0);
        double a = -N.dot3(w0);
        double b = N.dot3(ray.getDirection());
        if (java.lang.Math.abs(b) < EPSILON) // ray is parallel to triangle plane
            return null;                    // if a == 0 , ray lies in triangle plane
        double r = a / b;

        Vec4 intersect = ray.getOrigin().add3(ray.getDirection().multiply3(r));
        Position pp = dc.getGlobe().computePositionFromPoint(intersect);

        // Draw the elevation from the elevation model, not the geode.
        double elev = dc.getGlobe().getElevation(pp.getLatitude(), pp.getLongitude());
        elev *= dc.getVerticalExaggeration();
        Position p = new Position(pp.getLatitude(), pp.getLongitude(), elev);

        return new PickedObject(pickPoint, colorCode, p, pp.getLatitude(), pp.getLongitude(), elev, true);
    }

    /**
     * Determines if and where a ray intersects a <code>RectTile</code> geometry.
     *
     * @param tile the <Code>RectTile</code> which geometry is to be tested for intersection.
     * @param line the ray for which an intersection is to be found.
     *
     * @return an array of <code>Intersection</code> sorted by increasing distance from the line origin, or null if no
     *         intersection was found.
     */
    private Intersection[] intersect(RectTile tile, Line line)
    {
        if (line == null)
        {
            String msg = Logging.getMessage("nullValue.LineIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (tile.ri.vertices == null)
            return null;

        // Compute 'vertical' plane perpendicular to the ground, that contains the ray
        Vec4 normalV = line.getDirection().cross3(globe.computeSurfaceNormalAtPoint(line.getOrigin()));
        Plane verticalPlane = new Plane(normalV.x(), normalV.y(), normalV.z(), -line.getOrigin().dot3(normalV));
        if (!tile.getExtent().intersects(verticalPlane))
            return null;

        // Compute 'horizontal' plane perpendicular to the vertical plane, that contains the ray
        Vec4 normalH = line.getDirection().cross3(normalV);
        Plane horizontalPlane = new Plane(normalH.x(), normalH.y(), normalH.z(), -line.getOrigin().dot3(normalH));
        if (!tile.getExtent().intersects(horizontalPlane))
            return null;

        Intersection[] hits;
        ArrayList<Intersection> list = new ArrayList<Intersection>();

        int[] indices = new int[tile.ri.indices.limit()];
        double[] coords = new double[tile.ri.vertices.limit()];
        tile.ri.indices.rewind();
        tile.ri.vertices.rewind();
        tile.ri.indices.get(indices, 0, indices.length);
        tile.ri.vertices.get(coords, 0, coords.length);
        tile.ri.indices.rewind();
        tile.ri.vertices.rewind();

        int trianglesNum = tile.ri.indices.capacity() - 2;
        double centerX = tile.ri.referenceCenter.x;
        double centerY = tile.ri.referenceCenter.y;
        double centerZ = tile.ri.referenceCenter.z;

        // Compute maximum cell size based on tile delta lat, density and globe radius
        double cellSide = tile.getSector().getDeltaLatRadians() * globe.getRadius() / density;
        double maxCellRadius = Math.sqrt(cellSide * cellSide * 2) / 2;   // half cell diagonal

        // Compute maximum elevation difference - assume cylinder as Extent
        double elevationSpan = ((Cylinder) tile.getExtent()).getCylinderHeight();

        // TODO: ignore back facing triangles?
        // Loop through all tile cells - triangle pairs
        int startIndice = (density + 2) * 2 + 6; // skip firts skirt row and a couple degenerate cells
        int endIndice = trianglesNum - startIndice; // ignore last skirt row and a couple degenerate cells
        int k = -1;
        for (int i = startIndice; i < endIndice; i += 2)
        {
            // Skip skirts and degenerate triangle cells - based on indice sequence.
            k = k == density - 1 ? -4 : k + 1; // density x terrain cells interleaved with 4 skirt and degenerate cells.
            if (k < 0)
                continue;

            // Triangle pair diagonal - v1 & v2
            int vIndex = 3 * indices[i + 1];
            Vec4 v1 = new Vec4(
                coords[vIndex++] + centerX,
                coords[vIndex++] + centerY,
                coords[vIndex] + centerZ);

            vIndex = 3 * indices[i + 2];
            Vec4 v2 = new Vec4(
                coords[vIndex++] + centerX,
                coords[vIndex++] + centerY,
                coords[vIndex] + centerZ);

            Vec4 cellCenter = Vec4.mix3(.5, v1, v2);

            // Test cell center distance to vertical plane
            if (Math.abs(verticalPlane.distanceTo(cellCenter)) > maxCellRadius)
                continue;

            // Test cell center distance to horizontal plane
            if (Math.abs(horizontalPlane.distanceTo(cellCenter)) > elevationSpan)
                continue;

            // Prepare to test triangles - get other two vertices v0 & v3
            Vec4 p;
            vIndex = 3 * indices[i];
            Vec4 v0 = new Vec4(
                coords[vIndex++] + centerX,
                coords[vIndex++] + centerY,
                coords[vIndex] + centerZ);

            vIndex = 3 * indices[i + 3];
            Vec4 v3 = new Vec4(
                coords[vIndex++] + centerX,
                coords[vIndex++] + centerY,
                coords[vIndex] + centerZ);

            // Test triangle 1 intersection w ray
            Triangle t = new Triangle(v0, v1, v2);
            if ((p = t.intersect(line)) != null)
            {
                list.add(new Intersection(p, false));
            }

            // Test triangle 2 intersection w ray
            t = new Triangle(v1, v2, v3);
            if ((p = t.intersect(line)) != null)
            {
                list.add(new Intersection(p, false));
            }
        }

        int numHits = list.size();
        if (numHits == 0)
            return null;

        hits = new Intersection[numHits];
        list.toArray(hits);

        final Vec4 origin = line.getOrigin();
        Arrays.sort(hits, new Comparator<Intersection>()
        {
            public int compare(Intersection i1, Intersection i2)
            {
                if (i1 == null && i2 == null)
                    return 0;
                if (i2 == null)
                    return -1;
                if (i1 == null)
                    return 1;

                Vec4 v1 = i1.getIntersectionPoint();
                Vec4 v2 = i2.getIntersectionPoint();
                double d1 = origin.distanceTo3(v1);
                double d2 = origin.distanceTo3(v2);
                return Double.compare(d1, d2);
            }
        });

        return hits;
    }

    /**
     * Determines if and where a <code>RectTile</code> geometry intersects the globe ellipsoid at a given elevation. The
     * returned array of <code>Intersection</code> describes a list of individual segments - two
     * <code>Intersection</code> for each, corresponding to each geometry triangle that intersects the given elevation.
     *
     * @param tile      the <Code>RectTile</code> which geometry is to be tested for intersection.
     * @param elevation the elevation for which intersection points are to be found.
     *
     * @return an array of <code>Intersection</code> pairs or null if no intersection was found.
     */
    private Intersection[] intersect(RectTile tile, double elevation)
    {
        if (tile.ri.vertices == null)
            return null;

        // Check whether the tile includes the intersection elevation - assume cylinder as Extent
        Cylinder cylinder = ((Cylinder) tile.getExtent());
        if (!(globe.isPointAboveElevation(cylinder.getBottomCenter(), elevation)
            ^ globe.isPointAboveElevation(cylinder.getTopCenter(), elevation)))
            return null;

        Intersection[] hits;
        ArrayList<Intersection> list = new ArrayList<Intersection>();

        int[] indices = new int[tile.ri.indices.limit()];
        double[] coords = new double[tile.ri.vertices.limit()];
        tile.ri.indices.rewind();
        tile.ri.vertices.rewind();
        tile.ri.indices.get(indices, 0, indices.length);
        tile.ri.vertices.get(coords, 0, coords.length);
        tile.ri.indices.rewind();
        tile.ri.vertices.rewind();

        int trianglesNum = tile.ri.indices.capacity() - 2;
        double centerX = tile.ri.referenceCenter.x;
        double centerY = tile.ri.referenceCenter.y;
        double centerZ = tile.ri.referenceCenter.z;

        // Loop through all tile cells - triangle pairs
        int startIndice = (density + 2) * 2 + 6; // skip firts skirt row and a couple degenerate cells
        int endIndice = trianglesNum - startIndice; // ignore last skirt row and a couple degenerate cells
        int k = -1;
        for (int i = startIndice; i < endIndice; i += 2)
        {
            // Skip skirts and degenerate triangle cells - based on indice sequence.
            k = k == density - 1 ? -4 : k + 1; // density x terrain cells interleaved with 4 skirt and degenerate cells.
            if (k < 0)
                continue;

            // Get the four cell corners
            int vIndex = 3 * indices[i];
            Vec4 v0 = new Vec4(
                coords[vIndex++] + centerX,
                coords[vIndex++] + centerY,
                coords[vIndex] + centerZ);

            vIndex = 3 * indices[i + 1];
            Vec4 v1 = new Vec4(
                coords[vIndex++] + centerX,
                coords[vIndex++] + centerY,
                coords[vIndex] + centerZ);

            vIndex = 3 * indices[i + 2];
            Vec4 v2 = new Vec4(
                coords[vIndex++] + centerX,
                coords[vIndex++] + centerY,
                coords[vIndex] + centerZ);

            vIndex = 3 * indices[i + 3];
            Vec4 v3 = new Vec4(
                coords[vIndex++] + centerX,
                coords[vIndex++] + centerY,
                coords[vIndex] + centerZ);

            Intersection[] inter;
            // Test triangle 1 intersection
            if ((inter = globe.intersect(new Triangle(v0, v1, v2), elevation)) != null)
            {
                list.add(inter[0]);
                list.add(inter[1]);
            }

            // Test triangle 2 intersection
            if ((inter = globe.intersect(new Triangle(v1, v2, v3), elevation)) != null)
            {
                list.add(inter[0]);
                list.add(inter[1]);
            }
        }

        int numHits = list.size();
        if (numHits == 0)
            return null;

        hits = new Intersection[numHits];
        list.toArray(hits);

        return hits;
    }

    private Vec4 getSurfacePoint(RectTile tile, Angle latitude, Angle longitude, double metersOffset)
    {
        Vec4 result = this.getSurfacePoint(tile, latitude, longitude);
        if (metersOffset != 0 && result != null)
            result = applyOffset(this.globe, result, metersOffset);

        return result;
    }

    /**
     * Offsets <code>point</code> by <code>metersOffset</code> meters.
     *
     * @param globe        the <code>Globe</code> from which to offset
     * @param point        the <code>Vec4</code> to offset
     * @param metersOffset the magnitude of the offset
     *
     * @return <code>point</code> offset along its surface normal as if it were on <code>globe</code>
     */
    private static Vec4 applyOffset(Globe globe, Vec4 point, double metersOffset)
    {
        Vec4 normal = globe.computeSurfaceNormalAtPoint(point);
        point = Vec4.fromLine3(point, metersOffset, normal);
        return point;
    }

    private Vec4 getSurfacePoint(RectTile tile, Angle latitude, Angle longitude)
    {
        if (latitude == null || longitude == null)
        {
            String msg = Logging.getMessage("nullValue.LatLonIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (!tile.sector.contains(latitude, longitude))
        {
            // not on this geometry
            return null;
        }

        if (tile.ri == null)
            return null;

        double lat = latitude.getDegrees();
        double lon = longitude.getDegrees();

        double bottom = tile.sector.getMinLatitude().getDegrees();
        double top = tile.sector.getMaxLatitude().getDegrees();
        double left = tile.sector.getMinLongitude().getDegrees();
        double right = tile.sector.getMaxLongitude().getDegrees();

        double leftDecimal = (lon - left) / (right - left);
        double bottomDecimal = (lat - bottom) / (top - bottom);

        int row = (int) (bottomDecimal * (tile.density));
        int column = (int) (leftDecimal * (tile.density));

        double l = createPosition(column, leftDecimal, tile.ri.density);
        double h = createPosition(row, bottomDecimal, tile.ri.density);

        Vec4 result = interpolate(row, column, l, h, tile.ri);
        result = result.add3(tile.ri.referenceCenter);

        return result;
    }

    /**
     * Computes from a column (or row) number, and a given offset ranged [0,1] corresponding to the distance along the
     * edge of this sector, where between this column and the next column the corresponding position will fall, in the
     * range [0,1].
     *
     * @param start   the number of the column or row to the left, below or on this position
     * @param decimal the distance from the left or bottom of the current sector that this position falls
     * @param density the number of intervals along the sector's side
     *
     * @return a decimal ranged [0,1] representing the position between two columns or rows, rather than between two
     *         edges of the sector
     */
    private static double createPosition(int start, double decimal, int density)
    {
        double l = ((double) start) / (double) density;
        double r = ((double) (start + 1)) / (double) density;

        return (decimal - l) / (r - l);
    }

    /**
     * Calculates a <code>Point</code> that sits at <code>xDec</code> offset from <code>column</code> to <code>column +
     * 1</code> and at <code>yDec</code> offset from <code>row</code> to <code>row + 1</code>. Accounts for the
     * diagonals.
     *
     * @param row    represents the row which corresponds to a <code>yDec</code> value of 0
     * @param column represents the column which corresponds to an <code>xDec</code> value of 0
     * @param xDec   constrained to [0,1]
     * @param yDec   constrained to [0,1]
     * @param ri     the render info holding the vertices, etc.
     *
     * @return a <code>Point</code> geometrically within or on the boundary of the quadrilateral whose bottom left
     *         corner is indexed by (<code>row</code>, <code>column</code>)
     */
    private static Vec4 interpolate(int row, int column, double xDec, double yDec, RenderInfo ri)
    {
        row++;
        column++;

        int numVerticesPerEdge = ri.density + 3;

        int bottomLeft = row * numVerticesPerEdge + column;

        bottomLeft *= 3;

        int numVertsTimesThree = numVerticesPerEdge * 3;

        double[] a = new double[6];
        ri.vertices.position(bottomLeft);
        ri.vertices.get(a);
        Vec4 bL = new Vec4(a[0], a[1], a[2]);
        Vec4 bR = new Vec4(a[3], a[4], a[5]);

        bottomLeft += numVertsTimesThree;

        ri.vertices.position(bottomLeft);
        ri.vertices.get(a);
        Vec4 tL = new Vec4(a[0], a[1], a[2]);
        Vec4 tR = new Vec4(a[3], a[4], a[5]);

        return interpolate(bL, bR, tR, tL, xDec, yDec);
    }

    /**
     * Calculates the point at (xDec, yDec) in the two triangles defined by {bL, bR, tL} and {bR, tR, tL}. If thought of
     * as a quadrilateral, the diagonal runs from tL to bR. Of course, this isn't a quad, it's two triangles.
     *
     * @param bL   the bottom left corner
     * @param bR   the bottom right corner
     * @param tR   the top right corner
     * @param tL   the top left corner
     * @param xDec how far along, [0,1] 0 = left edge, 1 = right edge
     * @param yDec how far along, [0,1] 0 = bottom edge, 1 = top edge
     *
     * @return the point xDec, yDec in the co-ordinate system defined by bL, bR, tR, tL
     */
    private static Vec4 interpolate(Vec4 bL, Vec4 bR, Vec4 tR, Vec4 tL, double xDec, double yDec)
    {
        double pos = xDec + yDec;
        if (pos == 1)
        {
            // on the diagonal - what's more, we don't need to do any "oneMinusT" calculation
            return new Vec4(
                tL.x * yDec + bR.x * xDec,
                tL.y * yDec + bR.y * xDec,
                tL.z * yDec + bR.z * xDec);
        }
        else if (pos > 1)
        {
            // in the "top right" half

            // vectors pointing from top right towards the point we want (can be thought of as "negative" vectors)
            Vec4 horizontalVector = (tL.subtract3(tR)).multiply3(1 - xDec);
            Vec4 verticalVector = (bR.subtract3(tR)).multiply3(1 - yDec);

            return tR.add3(horizontalVector).add3(verticalVector);
        }
        else
        {
            // pos < 1 - in the "bottom left" half

            // vectors pointing from the bottom left towards the point we want
            Vec4 horizontalVector = (bR.subtract3(bL)).multiply3(xDec);
            Vec4 verticalVector = (tL.subtract3(bL)).multiply3(yDec);

            return bL.add3(horizontalVector).add3(verticalVector);
        }
    }

    private static double[] baryCentricCoordsRequireInside(Vec4 pnt, Vec4[] V)
    {
        // if pnt is in the interior of the triangle determined by V, return its
        // barycentric coordinates with respect to V. Otherwise return null.

        // b0:
        final double tol = 1.0e-4;
        double[] b0b1b2 = new double[3];
        double triangleHeight =
            distanceFromLine(V[0], V[1], V[2].subtract3(V[1]));
        double heightFromPoint =
            distanceFromLine(pnt, V[1], V[2].subtract3(V[1]));
        b0b1b2[0] = heightFromPoint / triangleHeight;
        if (Math.abs(b0b1b2[0]) < tol)
            b0b1b2[0] = 0.0;
        else if (Math.abs(1.0 - b0b1b2[0]) < tol)
            b0b1b2[0] = 1.0;
        if (b0b1b2[0] < 0.0 || b0b1b2[0] > 1.0)
            return null;

        // b1:
        triangleHeight = distanceFromLine(V[1], V[0], V[2].subtract3(V[0]));
        heightFromPoint = distanceFromLine(pnt, V[0], V[2].subtract3(V[0]));
        b0b1b2[1] = heightFromPoint / triangleHeight;
        if (Math.abs(b0b1b2[1]) < tol)
            b0b1b2[1] = 0.0;
        else if (Math.abs(1.0 - b0b1b2[1]) < tol)
            b0b1b2[1] = 1.0;
        if (b0b1b2[1] < 0.0 || b0b1b2[1] > 1.0)
            return null;

        // b2:
        b0b1b2[2] = 1.0 - b0b1b2[0] - b0b1b2[1];
        if (Math.abs(b0b1b2[2]) < tol)
            b0b1b2[2] = 0.0;
        else if (Math.abs(1.0 - b0b1b2[2]) < tol)
            b0b1b2[2] = 1.0;
        if (b0b1b2[2] < 0.0)
            return null;
        return b0b1b2;
    }

    private static double distanceFromLine(Vec4 pnt, Vec4 P, Vec4 u)
    {
        // Return distance from pnt to line(P,u)
        // Pythagorean theorem approach: c^2 = a^2 + b^2. The
        // The square of the distance we seek is b^2:
        Vec4 toPoint = pnt.subtract3(P);
        double cSquared = toPoint.dot3(toPoint);
        double aSquared = u.normalize3().dot3(toPoint);
        aSquared *= aSquared;
        double distSquared = cSquared - aSquared;
        if (distSquared < 0.0)
            // must be a tiny number that really ought to be 0.0
            return 0.0;
        return Math.sqrt(distSquared);
    }

    protected DoubleBuffer makeGeographicTexCoords(SectorGeometry sg,
        SectorGeometry.GeographicTextureCoordinateComputer computer)
    {
        if (sg == null)
        {
            String msg = Logging.getMessage("nullValue.SectorGeometryIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (computer == null)
        {
            String msg = Logging.getMessage("nullValue.TextureCoordinateComputerIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        RectTile rt = (RectTile) sg;

        int density = rt.density;
        if (density < 1)
            density = 1;

        int coordCount = (density + 3) * (density + 3);
        DoubleBuffer p = BufferUtil.newDoubleBuffer(2 * coordCount);

        double deltaLat = rt.sector.getDeltaLatRadians() / density;
        double deltaLon = rt.sector.getDeltaLonRadians() / density;
        Angle minLat = rt.sector.getMinLatitude();
        Angle maxLat = rt.sector.getMaxLatitude();
        Angle minLon = rt.sector.getMinLongitude();
        Angle maxLon = rt.sector.getMaxLongitude();

        double[] uv; // for return values from computer

        int k = 2 * (density + 3);
        for (int j = 0; j < density; j++)
        {
            Angle lat = Angle.fromRadians(minLat.radians + j * deltaLat);

            // skirt column; duplicate first column
            uv = computer.compute(lat, minLon);
            p.put(k++, uv[0]).put(k++, uv[1]);

            // interior columns
            for (int i = 0; i < density; i++)
            {
                Angle lon = Angle.fromRadians(minLon.radians + i * deltaLon);
                uv = computer.compute(lat, lon);
                p.put(k++, uv[0]).put(k++, uv[1]);
            }

            // last interior column; force u to 1.
            uv = computer.compute(lat, maxLon);
            p.put(k++, uv[0]).put(k++, uv[1]);

            // skirt column; duplicate previous column
            p.put(k++, uv[0]).put(k++, uv[1]);
        }

        // Last interior row
        uv = computer.compute(maxLat, minLon); // skirt column
        p.put(k++, uv[0]).put(k++, uv[1]);

        for (int i = 0; i < density; i++)
        {
            Angle lon = Angle.fromRadians(minLon.radians + i * deltaLon); // u
            uv = computer.compute(maxLat, lon);
            p.put(k++, uv[0]).put(k++, uv[1]);
        }

        uv = computer.compute(maxLat, maxLon); // last interior column
        p.put(k++, uv[0]).put(k++, uv[1]);
        p.put(k++, uv[0]).put(k++, uv[1]); // skirt column

        // last skirt row
        int kk = k - 2 * (density + 3);
        for (int i = 0; i < density + 3; i++)
        {
            p.put(k++, p.get(kk++));
            p.put(k++, p.get(kk++));
        }

        // first skirt row
        k = 0;
        kk = 2 * (density + 3);
        for (int i = 0; i < density + 3; i++)
        {
            p.put(k++, p.get(kk++));
            p.put(k++, p.get(kk++));
        }

        return p;
    }

    private static void processTextureCoordinates(DrawContext dc, int density)
    {
        if (density < 1)
            density = 1;

        if (dc.getGLRuntimeCapabilities().isUseVertexBufferObject())
        {
            if (parameterizationsBuffers.containsKey(density))
                return;
        }
        else
        {
            if (parameterizations.containsKey(density))
                return;
        }

        // Approximate 1 to avoid shearing off of right and top skirts in SurfaceTileRenderer.
        // TODO: dig into this more: why are the skirts being sheared off?
        final double one = 0.999999;

        int coordCount = (density + 3) * (density + 3);
        DoubleBuffer p = BufferUtil.newDoubleBuffer(2 * coordCount);
        double delta = 1d / density;
        int k = 2 * (density + 3);
        for (int j = 0; j < density; j++)
        {
            double v = j * delta;

            // skirt column; duplicate first column
            p.put(k++, 0d);
            p.put(k++, v);

            // interior columns
            for (int i = 0; i < density; i++)
            {
                p.put(k++, i * delta); // u
                p.put(k++, v);
            }

            // last interior column; force u to 1.
            p.put(k++, one);//1d);
            p.put(k++, v);

            // skirt column; duplicate previous column
            p.put(k++, one);//1d);
            p.put(k++, v);
        }

        // Last interior row
        //noinspection UnnecessaryLocalVariable
        double v = one;//1d;
        p.put(k++, 0d); // skirt column
        p.put(k++, v);

        for (int i = 0; i < density; i++)
        {
            p.put(k++, i * delta); // u
            p.put(k++, v);
        }
        p.put(k++, one);//1d); // last interior column
        p.put(k++, v);

        p.put(k++, one);//1d); // skirt column
        p.put(k++, v);

        // last skirt row
        int kk = k - 2 * (density + 3);
        for (int i = 0; i < density + 3; i++)
        {
            p.put(k++, p.get(kk++));
            p.put(k++, p.get(kk++));
        }

        // first skirt row
        k = 0;
        kk = 2 * (density + 3);
        for (int i = 0; i < density + 3; i++)
        {
            p.put(k++, p.get(kk++));
            p.put(k++, p.get(kk++));
        }

        GL gl = dc.getGL();

        OGLStackHandler ogsh = new OGLStackHandler();

        try
        {
            ogsh.pushClientAttrib(gl, GL.GL_CLIENT_VERTEX_ARRAY_BIT);

            if (dc.getGLRuntimeCapabilities().isUseVertexBufferObject())
            {
                //Create a new VBO for the texture coordinates

                int glBuf[] = new int[1];
                gl.glGenBuffers(1, glBuf, 0);

                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, glBuf[0]);
                gl.glBufferData(GL.GL_ARRAY_BUFFER, p.limit() * 8, p, GL.GL_STATIC_DRAW);

                parameterizationsBuffers.put(density, glBuf[0]);
            }
            else
            {
                //Store the buffer for use with vertex arrays
                parameterizations.put(density, p);
            }
        }
        finally
        {
            ogsh.pop(gl);
        }
    }

    protected static void processIndices(DrawContext dc, int density)
    {
        if (density < 1)
            density = 1;

        // return if we have a pre-computed buffer.
        if (dc.getGLRuntimeCapabilities().isUseVertexBufferObject())
        {
            //Check for both the client and server buffers. We need the client buffer for some calculations.
            if (indexBuffers.containsKey(density) && indexLists.containsKey(density))
                return;
        }
        else
        {
            if (indexLists.containsKey(density))
                return;
        }

        int sideSize = density + 2;

        int indexCount = 2 * sideSize * sideSize + 4 * sideSize - 2;
        java.nio.IntBuffer buffer = BufferUtil.newIntBuffer(indexCount);
        int k = 0;
        for (int i = 0; i < sideSize; i++)
        {
            buffer.put(k);
            if (i > 0)
            {
                buffer.put(++k);
                buffer.put(k);
            }

            if (i % 2 == 0) // even
            {
                buffer.put(++k);
                for (int j = 0; j < sideSize; j++)
                {
                    k += sideSize;
                    buffer.put(k);
                    buffer.put(++k);
                }
            }
            else // odd
            {
                buffer.put(--k);
                for (int j = 0; j < sideSize; j++)
                {
                    k -= sideSize;
                    buffer.put(k);
                    buffer.put(--k);
                }
            }
        }

        buffer.rewind();

        GL gl = dc.getGL();

        OGLStackHandler ogsh = new OGLStackHandler();

        try
        {
            ogsh.pushClientAttrib(gl, GL.GL_CLIENT_VERTEX_ARRAY_BIT);

            if (dc.getGLRuntimeCapabilities().isUseVertexBufferObject())
            {
                //Create a new VBO for the indexes
                int glBuf[] = new int[1];
                gl.glGenBuffers(1, glBuf, 0);

                gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, glBuf[0]);
                gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, buffer.limit() * 4, buffer, GL.GL_STATIC_DRAW);

                indexBuffers.put(density, glBuf[0]);

                //Store the buffer because it is used for calculating intersections etc.
                indexLists.put(density, buffer);
            }
            else
            {
                //Store the buffer for use with vertex arrays
                indexLists.put(density, buffer);
            }
        }
        finally
        {
            ogsh.pop(gl);
        }
    }

    private SectorGeometry.ExtractedShapeDescription getIntersectingTessellationPieces(RectTile tile, Plane[] planes)
    {
        tile.ri.vertices.rewind();
        tile.ri.indices.rewind();

        Vec4 offset = tile.ri.referenceCenter;
        if (offset == null)
            offset = new Vec4(0.0);

        int trianglesNum = tile.ri.indices.capacity() - 2;

        int[] indices = new int[3];
        double[] coords = new double[3];

        SectorGeometry.ExtractedShapeDescription clippedTriangleList = null;
        for (int i = 0; i < trianglesNum; i++)
        {
            tile.ri.indices.position(i);
            tile.ri.indices.get(indices);

            if ((indices[0] == indices[1]) || (indices[0] == indices[2]) ||
                (indices[1] == indices[2]))
                // degenerate triangle
                continue;
            Vec4[] triVerts = new Vec4[3];
            for (int j = 0; j < 3; j++)
            {
                tile.ri.vertices.position(3 * indices[j]);
                tile.ri.vertices.get(coords);
                triVerts[j] = new Vec4(coords[0] + offset.getX(),
                    coords[1] + offset.getY(),
                    coords[2] + offset.getZ(), 1.0);
            }
            clippedTriangleList = addClippedPolygon(triVerts, planes, clippedTriangleList);
        }
        return clippedTriangleList;
    }

    private SectorGeometry.ExtractedShapeDescription addClippedPolygon(Vec4[] triVerts, Plane[] planes,
        SectorGeometry.ExtractedShapeDescription l)
    {
        // Clip the polygon defined by polyVerts to the region defined by the intersection of
        // the negative halfspaces in 'planes'. If there is a non-empty clipped result, then
        // add it to the given list.
        // This routine is (currently) only used to clip triangles in the current tessellation,
        // but it is actually general enough for n-sided polygons. Best results will be
        // obtained if the polygon is convex.

        // ignore triangles on skirts
        if (isSkirt(triVerts))
            return l;

        // We use a multi-pass Sutherland-Hodgman-style clipping algorithm.
        // There is one pass for each clipping plane. We begin by copying the
        // original vertices to local working storage.
        Vec4[] polyVerts = new Vec4[3];
        System.arraycopy(triVerts, 0, polyVerts, 0, 3);

        for (Plane p : planes)
        {
            polyVerts = doSHPass(p, polyVerts);
            if (polyVerts == null)
                // the polygon has been totally clipped away
                return l;
        }
        // some part of the polygon survived. Store it in the list.
        if (l == null)
            l = new SectorGeometry.ExtractedShapeDescription(
                new ArrayList<Vec4[]>(), new ArrayList<SectorGeometry.BoundaryEdge>());
        l.interiorPolys.add(polyVerts);
        addBoundaryEdges(polyVerts, triVerts, l.shapeOutline);

        return l;
    }

    private boolean isSkirt(Vec4[] triVerts)
    {
        Vec4 normal = globe.computeSurfaceNormalAtPoint(triVerts[0]);
        // try to minimize numerical roundoff. The three triangle vertices
        // are going to have coordinates with roughly the same magnitude,
        // so we just sample triVerts[0].
        double maxC = Math.max(Math.abs(triVerts[0].x), Math.abs(triVerts[0].y));
        maxC = Math.max(maxC, Math.abs(triVerts[0].z));
        Vec4 v0 = triVerts[0].divide3(maxC);
        Vec4 u = triVerts[1].divide3(maxC).subtract3(v0);
        Vec4 v = triVerts[triVerts.length - 1].divide3(maxC).subtract3(v0);
        Vec4 w = u.cross3(v).normalize3();
        return (Math.abs(w.dot3(normal)) < 0.0001);
    }

    private Vec4[] doSHPass(Plane p, Vec4[] polyVerts)
    {
        // See comments in addClippedPolygon. Also note that, even if the
        // original polygon is a triangle, the polygon here may have
        // more than three vertices, depending on how it cuts the various
        // planes whose volumetric intersection defines the clipping region.
        ArrayList<Vec4> workingStorage = new ArrayList<Vec4>();
        Vec4 startPnt = polyVerts[0];
        boolean startPntIn = (p.dot(startPnt) <= 0.0);
        for (int i = 1; i <= polyVerts.length; i++)
        {
            if (startPntIn)
                workingStorage.add(startPnt);
            Vec4 endPnt = polyVerts[i % polyVerts.length];
            boolean endPntIn = (p.dot(endPnt) <= 0.0);
            if (startPntIn != endPntIn)
            {
                // compute and store the intersection of this edge with p
                Vec4[] clippedPnts;
                if (startPntIn)
                    clippedPnts = p.clip(startPnt, endPnt);
                else
                    clippedPnts = p.clip(endPnt, startPnt);
                workingStorage.add(clippedPnts[0]);
            }
            // prepare for next edge
            startPnt = endPnt;
            startPntIn = endPntIn;
        }
        if (workingStorage.size() == 0)
            return null;
        Vec4[] verts = new Vec4[workingStorage.size()];
        return workingStorage.toArray(verts);
    }

    private void addBoundaryEdges(Vec4[] polyVerts, Vec4[] triVerts,
        ArrayList<SectorGeometry.BoundaryEdge> beList)
    {
        // each edge of polyVerts not coincident with an edge of the original
        // triangle (triVerts) belongs to the outer boundary.
        for (int i = 0; i < polyVerts.length; i++)
        {
            int j = (i + 1) % polyVerts.length;
            if (!edgeOnTriangle(polyVerts[i], polyVerts[j], triVerts))
                beList.add(new SectorGeometry.BoundaryEdge(polyVerts, i, j));
        }
    }

    private boolean edgeOnTriangle(Vec4 a, Vec4 b, Vec4[] tri)
    {
        final double tol = 1.0e-4;
        double[] coords_a = baryCentricCoordsRequireInside(a, tri);
        double[] coords_b = baryCentricCoordsRequireInside(b, tri);
        if ((coords_a == null) || (coords_b == null))
            // mathematically not possible because 'a' and 'b' are
            // known to be on edges of the triangle 'tri'.
            return true;
        for (int i = 0; i < 3; i++)
        {
            if ((coords_a[i] < tol) && (coords_b[i] < tol))
                // 'a' and 'b' are on the same edge
                return true;
        }
        return false;
    }

    private SectorGeometry.ExtractedShapeDescription getIntersectingTessellationPieces(RectTile tile, Vec4 Cxyz,
        Vec4 uHat, Vec4 vHat,
        double uRadius, double vRadius)
    {
        tile.ri.vertices.rewind();
        tile.ri.indices.rewind();

        Vec4 offset = tile.ri.referenceCenter;
        if (offset == null)
            offset = new Vec4(0.0);

        int trianglesNum = tile.ri.indices.capacity() - 2;

        int[] indices = new int[3];
        double[] coords = new double[3];

        SectorGeometry.ExtractedShapeDescription clippedTriangleList = null;
        for (int i = 0; i < trianglesNum; i++)
        {
            tile.ri.indices.position(i);
            tile.ri.indices.get(indices);

            if ((indices[0] == indices[1]) || (indices[0] == indices[2]) ||
                (indices[1] == indices[2]))
                // degenerate triangle
                continue;
            Vec4[] triVerts = new Vec4[3];
            for (int j = 0; j < 3; j++)
            {
                tile.ri.vertices.position(3 * indices[j]);
                tile.ri.vertices.get(coords);
                triVerts[j] = new Vec4(coords[0] + offset.getX(),
                    coords[1] + offset.getY(),
                    coords[2] + offset.getZ(), 1.0);
            }
            clippedTriangleList = addClippedPolygon(triVerts,
                Cxyz, uHat, vHat, uRadius, vRadius, clippedTriangleList);
        }
        return clippedTriangleList;
    }

    private SectorGeometry.ExtractedShapeDescription addClippedPolygon(Vec4[] polyVerts, Vec4 Cxyz,
        Vec4 uHat, Vec4 vHat, double uRadius,
        double vRadius,
        SectorGeometry.ExtractedShapeDescription l)
    {
        // ignore triangles on skirts
        if (isSkirt(polyVerts))
            return l;

        int i = 0, nInNegHalfspace = 0, locIn = -1, locOut = -1;
        for (Vec4 vtx : polyVerts)
        {
            Vec4 vMinusC = vtx.subtract3(Cxyz);
            double xd = vMinusC.dot3(uHat);
            double yd = vMinusC.dot3(vHat);
            double halfspaceEqn = (xd * xd) / (uRadius * uRadius) + (yd * yd) / (vRadius * vRadius) - 1.0;
            if (halfspaceEqn <= 0.0)
            {
                locIn = i++;
                nInNegHalfspace++;
            }
            else
                locOut = i++;
        }
        SectorGeometry.BoundaryEdge be = new SectorGeometry.BoundaryEdge(null, -1, -1);
        switch (nInNegHalfspace)
        {
            case 0: // check for edge intersections
                polyVerts = checkForEdgeCylinderIntersections(polyVerts, Cxyz, uHat, vHat,
                    uRadius, vRadius);
                break;
            case 1: // compute and return a trimmed triangle
                if (locIn != 0)
                {
                    Vec4 h1 = polyVerts[locIn];
                    polyVerts[locIn] = polyVerts[0];
                    polyVerts[0] = h1;
                }
                polyVerts = computeTrimmedPoly(polyVerts, Cxyz, uHat, vHat, uRadius,
                    vRadius, nInNegHalfspace, be);
                break;
            case 2: // compute and return a trimmed quadrilateral
                if (locOut != 0)
                {
                    Vec4 h2 = polyVerts[locOut];
                    polyVerts[locOut] = polyVerts[0];
                    polyVerts[0] = h2;
                }
                polyVerts = computeTrimmedPoly(polyVerts, Cxyz, uHat, vHat, uRadius,
                    vRadius, nInNegHalfspace, be);
                break;
            case 3: // triangle completely inside cylinder, so store it
                break;
        }
        if (polyVerts == null)
            return l;
        if (l == null)
            l = new SectorGeometry.ExtractedShapeDescription(new ArrayList<Vec4[]>(100),
                new ArrayList<SectorGeometry.BoundaryEdge>(50));
        l.interiorPolys.add(polyVerts);
        if (be.vertices != null)
            l.shapeOutline.add(be);
        return l;
    }

    // TODO: Why is this empty method here?
    private Vec4[] checkForEdgeCylinderIntersections(Vec4[] polyVerts, Vec4 Cxyz,
        Vec4 uHat, Vec4 vHat, double uRadius, double vRadius)
    {
        // no triangle vertices are inside the cylinder; see if there are edge intersections
        // this will only be the case if the cylinder's size is roughly the same as a triangle
        // in the current tessellation and may not be worth the extra computation to check.
        return null;
    }

    private Vec4[] computeTrimmedPoly(Vec4[] polyVerts, Vec4 Cxyz,
        Vec4 uHat, Vec4 vHat, double uRadius, double vRadius, int nInside,
        SectorGeometry.BoundaryEdge be)
    {
        // Either 1 or 2 vertices are inside the ellipse. If exactly 1 is inside, it is in position 0
        // of the array. If exactly 1 is outside, it is in position 0 of the array.
        // We therefore compute the points of intersection between the two edges [0]-[1] and [0]-[2]
        // with the cylinder and return either a triangle or a quadrilateral.
        Vec4 p1 = intersectWithEllCyl(polyVerts[0], polyVerts[1], Cxyz, uHat, vHat, uRadius, vRadius);
        Vec4 p2 = intersectWithEllCyl(polyVerts[0], polyVerts[2], Cxyz, uHat, vHat, uRadius, vRadius);
        Vec4 midP1P2 = p1.multiply3(0.5).add3(p2.multiply3(0.5));
        if (nInside == 1)
        {
            polyVerts[1] = p1;
            polyVerts[2] = p2;
            be.vertices = polyVerts;
            be.i1 = 1;
            be.i2 = 2;
            be.toMidPoint = midP1P2.subtract3(polyVerts[0]);
            return polyVerts;
        }
        Vec4[] ret = new Vec4[4];
        ret[0] = p1;
        ret[1] = polyVerts[1];
        ret[2] = polyVerts[2];
        ret[3] = p2;
        be.vertices = ret;
        be.i1 = 0;
        be.i2 = 3;
        be.toMidPoint = polyVerts[0].subtract3(midP1P2);
        return ret;
    }

    private Vec4 intersectWithEllCyl(Vec4 v0, Vec4 v1, Vec4 Cxyz,
        Vec4 uHat, Vec4 vHat, double uRadius, double vRadius)
    {
        // Entry condition: one of (v0, v1) is inside the elliptical cylinder, and one is
        // outside. We find 0<t<1 such that (1-t)*v0 + t*v1 is on the cylinder. We then return
        // the corresponding point.

        // First project v0 and v1 onto the plane of the ellipse
        Vec4 v0MinusC = v0.subtract3(Cxyz);
        double v0x = v0MinusC.dot3(uHat);
        double v0y = v0MinusC.dot3(vHat);
        Vec4 v1MinusC = v1.subtract3(Cxyz);
        double v1x = v1MinusC.dot3(uHat);
        double v1y = v1MinusC.dot3(vHat);

        // Then compute the coefficients of the quadratic equation describing where
        // the line segment (v0x,v0y)-(v1x,v1y) intersects the ellipse in the plane:
        double v1xMinusV0x = v1x - v0x;
        double v1yMinusV0y = v1y - v0y;
        double uRsquared = uRadius * uRadius;
        double vRsquared = vRadius * vRadius;

        double a = v1xMinusV0x * v1xMinusV0x / uRsquared + v1yMinusV0y * v1yMinusV0y / vRsquared;
        double b = 2.0 * (v0x * v1xMinusV0x / uRsquared + v0y * v1yMinusV0y / vRsquared);
        double c = v0x * v0x / uRsquared + v0y * v0y / vRsquared - 1.0;

        // now solve it
        // if the entry condition is satsfied, the diuscriminant will not be negative...
        double disc = Math.sqrt(b * b - 4.0 * a * c);
        double t = (-b + disc) / (2.0 * a);
        if ((t < 0.0) || (t > 1.0))
            // need the other root
            t = (-b - disc) / (2.0 * a);

        // the desired point is obtained by using the computed t with the original points
        // v0 and v1:
        return v0.multiply3(1.0 - t).add3(v1.multiply3(t));
    }

    // TODO: The following method was brought over from BasicRectangularTessellator and is unchecked.
//    // Compute normals for a strip
//    protected static java.nio.DoubleBuffer getNormals(int density, DoubleBuffer vertices,
//        java.nio.IntBuffer indices, Vec4 referenceCenter)
//    {
//        int numVertices = (density + 3) * (density + 3);
//        int sideSize = density + 2;
//        int numFaces = indices.limit() - 2;
//        double centerX = referenceCenter.x;
//        double centerY = referenceCenter.y;
//        double centerZ = referenceCenter.z;
//        // Create normal buffer
//        java.nio.DoubleBuffer normals = BufferUtil.newDoubleBuffer(numVertices * 3);
//        // Create per vertex normal lists
//        ArrayList<ArrayList<Vec4>> normalLists = new ArrayList<ArrayList<Vec4>>(numVertices);
//        for (int i = 0; i < numVertices; i++)
//            normalLists.set(i, new ArrayList<Vec4>());
//        // Go through all faces in the strip and store normals in lists
//        for (int i = 0; i < numFaces; i++)
//        {
//            int vIndex = 3 * indices.get(i);
//            Vec4 v0 = new Vec4((vertices.get(vIndex++) + centerX),
//                (vertices.get(vIndex++) + centerY),
//                (vertices.get(vIndex) + centerZ));
//
//            vIndex = 3 * indices.get(i + 1);
//            Vec4 v1 = new Vec4((vertices.get(vIndex++) + centerX),
//                (vertices.get(vIndex++) + centerY),
//                (vertices.get(vIndex) + centerZ));
//
//            vIndex = 3 * indices.get(i + 2);
//            Vec4 v2 = new Vec4((vertices.get(vIndex++) + centerX),
//                (vertices.get(vIndex++) + centerY),
//                (vertices.get(vIndex) + centerZ));
//
//            // get triangle edge vectors and plane normal
//            Vec4 e1 = v1.subtract3(v0);
//            Vec4 e2 = v2.subtract3(v0);
//            Vec4 N = e1.cross3(e2).normalize3();  // if N is 0, the triangle is degenerate
//
//            // Store the face's normal for each of the vertices that make up the face.
//            // TODO: Clear up warnings here
//            normalLists.get(indices.get(i)).add(N);
//            normalLists.get(indices.get(i + 1)).add(N);
//            normalLists.get(indices.get(i + 2)).add(N);
//            //System.out.println("Normal: " + N);
//        }
//
//        // Now loop through each vertex, and average out all the normals stored.
//        int idx = 0;
//        for (int i = 0; i < numVertices; i++)
//        {
//            Vec4 normal = Vec4.ZERO;
//            // Sum
//            for (int j = 0; j < normalLists.get(i).size(); ++j)
//                normal = normal.add3(normalLists.get(i).get(j));
//            // Average
//            normal = normal.multiply3(1.0f / normalLists.get(i).size()).normalize3();
//            // Fill normal buffer
//            normals.put(idx++, normal.x);
//            normals.put(idx++, normal.y);
//            normals.put(idx++, normal.z);
//            //System.out.println("Normal: " + normal + " - " + normalLists[i].size());
//            //System.out.println("Normal buffer: " + normals.get(idx - 3) + ", " + normals.get(idx - 2) + ", " + normals.get(idx - 1));
//        }
//
//        return normals;
//    }

    //
    // Exposes aspects of the RectTile.
    //

    public static class RectGeometry
    {
        private RectTile tile;
        private double rowFactor;
        private double colFactor;

        public RectGeometry(RectTile tile)
        {
            this.tile = tile;
            // Precompute as much as possible; computation in this class is a hot spot...
            rowFactor = getNumRows() / tile.sector.getDeltaLatDegrees();
            colFactor = getNumCols() / tile.sector.getDeltaLonDegrees();
        }

        public int getColAtLon(double longitude)
        {
            return (int) Math.floor((longitude - tile.sector.getMinLongitude().degrees) * colFactor);
        }

        public int getRowAtLat(double latitude)
        {
            return (int) Math.floor((latitude - tile.sector.getMinLatitude().degrees) * rowFactor);
        }

        public double getLatAtRow(int row)
        {
            return tile.sector.getMinLatitude().degrees + row / rowFactor;
        }

        public double getLonAtCol(int col)
        {
            return tile.sector.getMinLongitude().degrees + col / colFactor;
        }

        /*
         * Bilinearly interpolate XYZ coords from the grid patch that contains the given lat-lon.
         *
         * Note:  The returned point is clamped along the nearest border if the given lat-lon is outside the
         * region spanned by this tile.
         *
         */
        public double[] getPointAt(double lat, double lon)
        {
            int col = getColAtLon(lon);
            if (col < 0)
            {
                col = 0;
                lon = getMinLongitude();
            }
            else if (col > getNumCols())
            {
                col = getNumCols();
                lon = getMaxLongitude();
            }

            int row = getRowAtLat(lat);
            if (row < 0)
            {
                row = 0;
                lat = getMinLatitude();
            }
            else if (row > getNumRows())
            {
                row = getNumRows();
                lat = getMaxLatitude();
            }

            double[] c0 = new double[3];
            this.tile.ri.vertices.position(getVertexIndex(row, col));
            this.tile.ri.vertices.get(c0);
            double[] c1 = new double[3];
            this.tile.ri.vertices.position(getVertexIndex(row, col + 1));
            this.tile.ri.vertices.get(c1);
            double[] c2 = new double[3];
            this.tile.ri.vertices.position(getVertexIndex(row + 1, col));
            this.tile.ri.vertices.get(c2);
            double[] c3 = new double[3];
            this.tile.ri.vertices.position(getVertexIndex(row + 1, col + 1));
            this.tile.ri.vertices.get(c3);
            double[] refCenter = new double[3];
            this.tile.ri.referenceCenter.toArray3(refCenter, 0);

            // calculate our parameters u and v...
            double minLon = getLonAtCol(col);
            double maxLon = getLonAtCol(col + 1);
            double minLat = getLatAtRow(row);
            double maxLat = getLatAtRow(row + 1);
            double u = (lon - minLon) / (maxLon - minLon);
            double v = (lat - minLat) / (maxLat - minLat);

            double[] ret = new double[3];
            // unroll the loop...this method is a definite hotspot!
            ret[0] = c0[0] * (1. - u) * (1 - v) + c1[0] * (u) * (1. - v) + c2[0] * (1. - u) * (v) + c3[0] * u * v +
                refCenter[0];
            ret[1] = c0[1] * (1. - u) * (1 - v) + c1[1] * (u) * (1. - v) + c2[1] * (1. - u) * (v) + c3[1] * u * v +
                refCenter[1];
            ret[2] = c0[2] * (1. - u) * (1 - v) + c1[2] * (u) * (1. - v) + c2[2] * (1. - u) * (v) + c3[2] * u * v +
                refCenter[2];
            return ret;
        }

        public double getMinLongitude()
        {
            return this.tile.sector.getMinLongitude().degrees;
        }

        public double getMaxLongitude()
        {
            return this.tile.sector.getMaxLongitude().degrees;
        }

        public double getMinLatitude()
        {
            return this.tile.sector.getMinLatitude().degrees;
        }

        public double getMaxLatitude()
        {
            return this.tile.sector.getMaxLatitude().degrees;
        }

        public int getNumRows()
        {
            return this.tile.density;
        }

        public int getNumCols()
        {
            return this.tile.density;
        }

        private int getVertexIndex(int row, int col)
        {
            // The factor of 3 accounts for the 3 doubles that make up each node...
            // The 3 added to density is 2 tile-skirts plus 1 ending column...
            return (this.tile.density + 3) * (row + 1) * 3 + (col + 1) * 3;
        }
    }

    public static RectGeometry getTerrainGeometry(SectorGeometry tile)
    {
        if (tile == null || !(tile instanceof RectTile))
            // TODO: I*N this
            throw new IllegalArgumentException("SectorGeometry instance not of type RectTile");

        return new RectGeometry((RectTile) tile);
    }
}
