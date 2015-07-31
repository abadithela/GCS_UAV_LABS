/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render.airspaces;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import java.util.*;

/**
 * @author dcollins
 * @version $Id: AbstractAirspace.java 12522 2009-08-26 22:09:01Z tgaskins $
 */
public abstract class AbstractAirspace extends AVListImpl implements Airspace, Movable
{
    protected static final String ARC_SLICES = "ArcSlices";
    protected static final String DISABLE_TERRAIN_CONFORMANCE = "DisableTerrainConformance";
    protected static final String EXPIRY_TIME = "ExpiryTime";
    protected static final String GEOMETRY_CACHE_NAME = "Airspace Geometry";
    protected static final String GEOMETRY_CACHE_KEY = Geometry.class.getName();
    protected static final String GLOBE_KEY = "GlobeKey";
    protected static final String LENGTH_SLICES = "LengthSlices";
    protected static final String LOOPS = "Loops";
    protected static final String PILLARS = "Pillars";
    protected static final String SLICES = "Slices";
    protected static final String SPLIT_THRESHOLD = "SplitThreshold";    
    protected static final String STACKS = "Stacks";
    protected static final String SUBDIVISIONS = "Subdivisions";
    protected static final String VERTICAL_EXAGGERATION = "VerticalExaggeration";

    private static final long DEFAULT_GEOMETRY_CACHE_SIZE = 16777216L; // 16 megabytes

    private boolean visible = true;
    private AirspaceAttributes attributes;
    private double lowerAltitude = 0.0;
    private double upperAltitude = 1.0;
    private boolean lowerTerrainConforming = false;
    private boolean upperTerrainConforming = false;
    private boolean enableLevelOfDetail = true;
    private Collection<DetailLevel> detailLevels = new TreeSet<DetailLevel>();
    // Geometry computation and rendering support.
    private AirspaceRenderer renderer = new AirspaceRenderer();
    private GeometryBuilder geometryBuilder = new GeometryBuilder();
    // Extent support.
    private Extent extent;
    private boolean extentOutOfDate = true;
    private Object extentGlobeKey;
    // Geometry update support.
    private long expiryTime = -1L;
    private long minExpiryTime = 2000L;
    private long maxExpiryTime = 6000L;
    private static Random rand = new Random();
    // Elevation lookup map.
    private Map<LatLon, Double> elevationMap = new HashMap<LatLon, Double>();

    public AbstractAirspace(AirspaceAttributes attributes)
    {
        if (attributes == null)
        {
            String message = "nullValue.AirspaceAttributesIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.attributes = attributes;

        if (!WorldWind.getMemoryCacheSet().containsCache(GEOMETRY_CACHE_KEY))
        {
            long size = Configuration.getLongValue(AVKey.AIRSPACE_GEOMETRY_CACHE_SIZE, DEFAULT_GEOMETRY_CACHE_SIZE);
            MemoryCache cache = new BasicMemoryCache((long) (0.85 * size), size);
            cache.setName(GEOMETRY_CACHE_NAME);
            WorldWind.getMemoryCacheSet().addCache(GEOMETRY_CACHE_KEY, cache);
        }
    }

    public AbstractAirspace()
    {
        this(new BasicAirspaceAttributes());
    }

    public boolean isVisible()
    {
        return this.visible;
    }

    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    public AirspaceAttributes getAttributes()
    {
        return this.attributes;
    }

    public void setAttributes(AirspaceAttributes attributes)
    {
        if (attributes == null)
        {
            String message = "nullValue.AirspaceAttributesIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.attributes = attributes;
    }

    /**
     * Returns the altitude limits.
     *
     * @return an array of two values indicating the lower and upper altitude limits, respectively.
     */
    public double[] getAltitudes()
    {
        double[] array = new double[2];
        array[0] = this.lowerAltitude;
        array[1] = this.upperAltitude;
        return array;
    }

    protected double[] getAltitudes(double verticalExaggeration)
    {
        double[] array = this.getAltitudes();
        array[0] = array[0] * verticalExaggeration;
        array[1] = array[1] * verticalExaggeration;
        return array;
    }

    /**
     * Set the upper and lower altitude limits.
     *
     * @param lowerAltitude the lower altitude limit, in meters relative to mean sea level
     * @param upperAltitude the upper altitude limit, in meters relative to mean sea level
     */
    public void setAltitudes(double lowerAltitude, double upperAltitude)
    {
        this.lowerAltitude = lowerAltitude;
        this.upperAltitude = upperAltitude;
        this.setExtentOutOfDate();
    }

    public void setAltitude(double altitude)
    {
        this.setAltitudes(altitude, altitude);
    }

    /**
     * Returns the value of the terrain-conforming attribute.
     *
     * @return the value of the terrain-conforming attribute. A valud of true indicates the object is
     *         terrain-conforming, a value of false indicates that it's not.
     */
    public boolean[] isTerrainConforming()
    {
        boolean[] array = new boolean[2];
        array[0] = this.lowerTerrainConforming;
        array[1] = this.upperTerrainConforming;
        return array;
    }

    /**
     * Sets the value of the terrain-conforming attribute.
     *
     * @param lowerTerrainConformant the value of the lower altitude terrain-conforming attribute.
     *                               A value of true indicates the object's lower altitude is terrain-conforming,
     *                               a value of false indicates that it's not.
     * @param upperTerrainConformant the value of the upper altitude terrain-conforming attribute.
     *                               A value of true indicates the object's upper altitude is terrain-conforming,
     *                               a value of false indicates that it's not.
     */
    public void setTerrainConforming(boolean lowerTerrainConformant, boolean upperTerrainConformant)
    {
        this.lowerTerrainConforming = lowerTerrainConformant;
        this.upperTerrainConforming = upperTerrainConformant;
        this.setExtentOutOfDate();
    }

    public boolean isAirspaceCollapsed()
    {
        return this.lowerAltitude == this.upperAltitude && this.lowerTerrainConforming == this.upperTerrainConforming;
    }

    public void setTerrainConforming(boolean terrainConformant)
    {
        this.setTerrainConforming(terrainConformant, terrainConformant);
    }

    public boolean isEnableLevelOfDetail()
    {
        return this.enableLevelOfDetail;
    }

    public void setEnableLevelOfDetail(boolean enableLevelOfDetail)
    {
        this.enableLevelOfDetail = enableLevelOfDetail;
    }

    public Iterable<DetailLevel> getDetailLevels()
    {
        return this.detailLevels;
    }

    public void setDetailLevels(Collection<DetailLevel> detailLevels)
    {
        this.detailLevels.clear();
        this.addDetailLevels(detailLevels);
    }

    protected void addDetailLevels(Collection<DetailLevel> newDetailLevels)
    {
        if (newDetailLevels != null)
            for (DetailLevel level : newDetailLevels)
                if (level != null)
                    this.detailLevels.add(level);
    }

    public boolean isAirspaceVisible(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getView() == null)
        {
            String message = "nullValue.DrawingContextViewIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Extent extent = this.getExtent(dc);
        return extent != null && extent.intersects(dc.getView().getFrustumInModelCoordinates());
    }

    public Extent getExtent(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getGlobe() == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Object globeKey = dc.getGlobe().getStateKey(dc);

        if (this.extentOutOfDate
         || this.extent == null
         || this.extentGlobeKey == null
         || !this.extentGlobeKey.equals(globeKey))
        {
            this.extent = this.doComputeExtent(dc);
            this.extentOutOfDate = false;
            this.extentGlobeKey = globeKey;
        }

        return this.extent;
    }

    protected boolean isExtentOutOfDate()
    {
        return this.extentOutOfDate;
    }

    protected void setExtentOutOfDate()
    {
        this.extentOutOfDate = true;
    }

    public AirspaceRenderer getRenderer()
    {
        return this.renderer;
    }

    protected void setRenderer(AirspaceRenderer renderer)
    {
        if (renderer == null)
        {
            String message = "nullValue.AirspaceRendererIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.renderer = renderer;
    }

    public void render(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!this.isVisible())
            return;

        if (!this.isAirspaceVisible(dc))
            return;

        this.doRender(dc);
    }

    public void renderGeometry(DrawContext dc, String drawStyle)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (drawStyle == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.doRenderGeometry(dc, drawStyle);
    }

    public void renderExtent(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        this.doRenderExtent(dc);
    }

    public void move(Position position)
    {
        if (position == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.moveTo(this.getReferencePosition().add(position));
    }

    public void moveTo(Position position)
    {
        if (position == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Position oldRef = this.getReferencePosition();
        //noinspection UnnecessaryLocalVariable
        Position newRef = position;
        this.doMoveTo(oldRef, newRef);
    }

    protected void doMoveTo(Position oldRef, Position newRef)
    {
        if (oldRef == null)
        {
            String message = "nullValue.OldRefIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (newRef == null)
        {
            String message = "nullValue.NewRefIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        
        double[] altitudes = this.getAltitudes();
        double elevDelta = newRef.getElevation() - oldRef.getElevation();
        this.setAltitudes(altitudes[0] + elevDelta, altitudes[1] + elevDelta);
    }

    protected Position computeReferencePosition(List<? extends LatLon> locations, double[] altitudes)
    {
        if (locations == null)
        {
            String message = "nullValue.LocationsIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (altitudes == null)
        {
            String message = "nullValue.AltitudesIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }


        int count = locations.size();
        if (count == 0)
            return null;

        LatLon ll;
        if (count < 3)
            ll = locations.get(0);
        else
            ll = locations.get(count / 2);

        return new Position(ll, altitudes[0]);
    }

    //**************************************************************//
    //********************  Geometry Rendering  ********************//
    //**************************************************************//

    // TODO: utility method for transforming list of LatLons into equivalent list comparable of crossing the dateline
    // (a) for computing a bounding sector, then bounding cylinder
    // (b) for computing tessellations of the list as 2D points
    // These lists of LatLons (Polygon) need to be capable of passing over
    // (a) the dateline
    // (b) either pole

    protected abstract Extent doComputeExtent(DrawContext dc);

    protected abstract void doRenderGeometry(DrawContext dc, String drawStyle);

    protected GeometryBuilder getGeometryBuilder()
    {
        return this.geometryBuilder;
    }

    protected void setGeometryBuilder(GeometryBuilder gb)
    {
        if (gb == null)
        {
            String message = "nullValue.GeometryBuilderIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.geometryBuilder = gb;
    }

    protected void doRender(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        AirspaceRenderer renderer = this.getRenderer();
        renderer.renderNow(dc, Arrays.asList(this));
    }

    protected void doRenderExtent(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Extent extent = this.getExtent(dc);
        if (extent != null && extent instanceof Renderable)
            ((Renderable) extent).render(dc);
    }

    protected DetailLevel computeDetailLevel(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Iterable<DetailLevel> detailLevels = this.getDetailLevels();
        if (detailLevels == null)
            return null;

        Iterator<DetailLevel> iter = detailLevels.iterator();
        if (!iter.hasNext())
            return null;

        // Find the first detail level that meets rendering criteria.
        DetailLevel level = iter.next();
        while (iter.hasNext() && !level.meetsCriteria(dc, this))
            level = iter.next();

        return level;
    }

    protected Cylinder computeBoundingCylinder(DrawContext dc, Iterable<? extends LatLon> locations)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getGlobe() == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (locations == null)
        {
            String message = "nullValue.LocationsIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Globe globe = dc.getGlobe();
        double verticalExaggeration = dc.getVerticalExaggeration();
        double[] altitudes = this.getAltitudes();
        boolean[] terrainConformant = this.isTerrainConforming();

        // Get the points corresponding to the given locations at the lower and upper altitudes.
        ArrayList<Vec4> points = new ArrayList<Vec4>();
        for (int a = 0; a < 2; a++)
            for (LatLon ll : locations)
                points.add(globe.computePointFromPosition(ll.getLatitude(), ll.getLongitude(),
                    verticalExaggeration * altitudes[a]));
        if (points.size() == 0)
            return null;

        // Compute the average point.
        Vec4 centerPoint = null;
        for (Vec4 vec : points)
            centerPoint = (centerPoint == null) ? vec : vec.add3(centerPoint);
        if (centerPoint == null)
            return null;

        centerPoint = centerPoint.divide3(points.size());

        // Get the center point and the lower and upper altitudes.
        LatLon centerLocation = globe.computePositionFromPoint(centerPoint);
        points.add(globe.computePointFromPosition(centerLocation.getLatitude(), centerLocation.getLongitude(),
            verticalExaggeration * altitudes[0]));
        points.add(globe.computePointFromPosition(centerLocation.getLatitude(), centerLocation.getLongitude(),
            verticalExaggeration * altitudes[1]));

        // Compute the surface normal at the center point. This will be the axis or up direction of the extent.
        Vec4 axis = globe.computeSurfaceNormalAtPoint(centerPoint);

        // Compute the extrema parallel and perpendicular projections of each point on the axis.
        double min_parallel = 0.0;
        double max_parallel = 0.0;
        double max_perp = 0.0;
        for (Vec4 vec : points)
        {
            Vec4 p = vec.subtract3(centerPoint);
            double parallel_proj = p.dot3(axis);
            Vec4 parallel = axis.multiply3(parallel_proj);
            Vec4 perpendicular = p.subtract3(parallel);
            double perpendicular_proj = perpendicular.getLength3();

            if (parallel_proj < min_parallel)
                min_parallel = parallel_proj;
            else if (parallel_proj > max_parallel)
                max_parallel = parallel_proj;

            if (perpendicular_proj > max_perp)
                max_perp = perpendicular_proj;
        }

        // If terrain conformance is enabled, add the minimum or maximum elevations around the locations to the
        // extrema parallel projections.
        if (terrainConformant[0] || terrainConformant[1])
        {
            double min_elev, max_elev;
            if (LatLon.locationsCrossDateLine(locations))
            {
                Sector[] splitSector = Sector.splitBoundingSectors(locations);
                double[] a = globe.getMinAndMaxElevations(splitSector[0]);
                double[] b = globe.getMinAndMaxElevations(splitSector[1]);
                min_elev = Math.min(a[0], b[0]); // Take the smallest min elevation.
                max_elev = Math.max(a[1], b[1]); // Take the largest max elevation.
            }
            else
            {
                Sector sector = Sector.boundingSector(locations);
                double[] a = globe.getMinAndMaxElevations(sector);
                min_elev = a[0];
                max_elev = a[1];
            }

            if (terrainConformant[0] && min_elev < 0.0)
                min_parallel += verticalExaggeration * min_elev;
            if (terrainConformant[1] && max_elev > 0.0)
                max_parallel += verticalExaggeration * max_elev;
        }

        // The bottom and top of the cylinder are the extrema parallel projections from the center point along the axis.
        Vec4 bottomPoint = axis.multiply3(min_parallel).add3(centerPoint);
        Vec4 topPoint    = axis.multiply3(max_parallel).add3(centerPoint);
        // The radius of the cylinder is the extrama perpendicular projection.
        double radius = max_perp;

        return new Cylinder(bottomPoint, topPoint, radius);
    }

    protected Extent computeBoundingExtent(DrawContext dc, Iterable<? extends Airspace> airspaces)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (airspaces == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Vec4 center = null;
        double radius = 0;
        int count = 0;

        // Add the center point of all airspace extents. This is the first step in computing the mean center point.
        for (Airspace airspace : airspaces)
        {
            Extent extent = airspace.getExtent(dc);
            if (extent != null)
            {
                center = (center != null) ? extent.getCenter().add3(center) : extent.getCenter();
                count++;
            }
        }

        // If there's no mean center point, then we cannot compute an enclosing extent, so just return null.
        if (center == null)
        {
            return null;
        }

        // Divide by the number of contributing extents to compute the mean center point.
        center = center.divide3(count);

        // Compute the maximum distance from the mean center point to the outermost point on an airspace extent. This
        // will be the radius of the enclosing extent.
        for (Airspace airspace : airspaces)
        {
            Extent extent = airspace.getExtent(dc);
            if (extent != null)
            {
                double distance = extent.getCenter().distanceTo3(center) + extent.getRadius();
                if (radius < distance)
                    radius = distance;
            }
        }

        return new Sphere(center, radius);
    }

    protected MemoryCache getGeometryCache()
    {
        return WorldWind.getMemoryCache(GEOMETRY_CACHE_KEY);
    }

    protected boolean isExpired(DrawContext dc, Geometry geom)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getGlobe() == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (geom == null)
        {
            String message = "nullValue.AirspaceGeometryIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Object o = geom.getValue(EXPIRY_TIME);
        if (o != null && o instanceof Long)
            if (dc.getFrameTimeStamp() > (Long) o)
                return true;

        o = geom.getValue(GLOBE_KEY);
        if (o != null)
            if (!dc.getGlobe().getStateKey(dc).equals(o))
                return true;

        return false;
    }

    protected void updateExpiryCriteria(DrawContext dc, Geometry geom)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getGlobe() == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        long expiryTime = this.getExpiryTime();
        geom.setValue(EXPIRY_TIME, (expiryTime >= 0L) ? expiryTime : null);
        geom.setValue(GLOBE_KEY, dc.getGlobe().getStateKey(dc));
    }

    protected long getExpiryTime()
    {
        return this.expiryTime;
    }

    protected void setExpiryTime(long timeMillis)
    {
        this.expiryTime = timeMillis;
    }

    protected long[] getExpiryRange()
    {
        long[] array = new long[2];
        array[0] = this.minExpiryTime;
        array[1] = this.maxExpiryTime;
        return array;
    }

    protected void setExpiryRange(long minTimeMillis, long maxTimeMillis)
    {
        this.minExpiryTime = minTimeMillis;
        this.maxExpiryTime = maxTimeMillis;
    }

    protected long nextExpiryTime(DrawContext dc, boolean[] terrainConformance)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        long expiryTime;
        if (terrainConformance[0] || terrainConformance[1])
        {
            long time = nextLong(this.minExpiryTime, this.maxExpiryTime);
            expiryTime = dc.getFrameTimeStamp() + time;
        }
        else
        {
            expiryTime = -1L;
        }
        return expiryTime;
    }

    private static long nextLong(long lo, long hi)
    {
        long n = hi - lo + 1;
        long i = rand.nextLong() % n;
        return lo + ((i < 0 )? -i : i);
    }

    protected void clearElevationMap()
    {
        this.elevationMap.clear();
    }

    public Vec4 computePointFromPosition(DrawContext dc, Angle latitude, Angle longitude, double elevation,
        boolean terrainConformant)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getGlobe() == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double newElevation = elevation;
        
        if (terrainConformant)
        {
            newElevation += this.computeElevationAt(dc, latitude, longitude);
        }

        return dc.getGlobe().computePointFromPosition(latitude, longitude, newElevation);
    }

    protected double computeElevationAt(DrawContext dc, Angle latitude, Angle longitude)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getGlobe() == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Globe globe;
        LatLon latlon;
        Vec4 surfacePoint;
        Position surfacePos;
        Double elevation;

        latlon = new LatLon(latitude, longitude);
        elevation = this.elevationMap.get(latlon);

        if (elevation == null)
        {
            globe = dc.getGlobe();
            elevation = 0.0;

            surfacePoint = dc.getPointOnGlobe(latitude, longitude);
            if (surfacePoint != null)
            {
                surfacePos = globe.computePositionFromPoint(surfacePoint);
                elevation += surfacePos.getElevation();
            }
            else
            {
                elevation += dc.getVerticalExaggeration() * globe.getElevation(latitude, longitude);
            }

            this.elevationMap.put(latlon, elevation);
        }

        return elevation;
    }

    //**************************************************************//
    //******************** END Geometry Rendering  *****************//
    //**************************************************************//

    public String getRestorableState()
    {
        RestorableSupport rs = RestorableSupport.newRestorableSupport();
        this.doGetRestorableState(rs, null);

        return rs.getStateAsXml();
    }

    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        // Method is invoked by subclasses to have superclass add its state and only its state
        this.doMyGetRestorableState(rs, context);
    }

    private void doMyGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        rs.addStateValueAsBoolean(context, "visible", this.isVisible());
        rs.addStateValueAsDouble(context, "lowerAltitude", this.getAltitudes()[0]);
        rs.addStateValueAsDouble(context, "upperAltitude", this.getAltitudes()[1]);
        rs.addStateValueAsBoolean(context, "lowerTerrainConforming", this.isTerrainConforming()[0]);
        rs.addStateValueAsBoolean(context, "upperTerrainConforming", this.isTerrainConforming()[1]);

        this.attributes.getRestorableState(rs, rs.addStateObject(context, "attributes"));
    }

    public void restoreState(String stateInXml)
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

        this.doRestoreState(rs, null);
    }

    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        // Method is invoked by subclasses to have superclass add its state and only its state
        this.doMyRestoreState(rs, context);
    }

    private void doMyRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        Boolean booleanState = rs.getStateValueAsBoolean(context, "visible");
        if (booleanState != null)
            this.setVisible(booleanState);

        Double lo = rs.getStateValueAsDouble(context, "lowerAltitude");
        if (lo == null)
            lo = this.getAltitudes()[0];

        Double hi = rs.getStateValueAsDouble(context, "upperAltitude");
        if (hi == null)
            hi = this.getAltitudes()[1];

        this.setAltitudes(lo, hi);

        Boolean loConform = rs.getStateValueAsBoolean(context, "lowerTerrainConforming");
        if (loConform == null)
            loConform = this.isTerrainConforming()[0];

        Boolean hiConform = rs.getStateValueAsBoolean(context, "upperTerrainConforming");
        if (hiConform == null)
            hiConform = this.isTerrainConforming()[1];

        this.setTerrainConforming(loConform, hiConform);

        RestorableSupport.StateObject so = rs.getStateObject(context, "attributes");
        if (so != null)
            this.getAttributes().restoreState(rs, so);
    }
}
