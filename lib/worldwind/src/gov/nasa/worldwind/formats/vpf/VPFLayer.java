/*
Copyright (C) 2001, 2009 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.formats.vpf;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import java.beans.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Renders elements from a VPF database.
 *
 * @author Patrick Murris
 * @version $Id: VPFLayer.java 12503 2009-08-21 06:48:37Z dcollins $
 */
public class VPFLayer extends AbstractLayer
{
    public static final String LIBRARY_CHANGED = "VPFLayer.LibraryChanged";
    public static final String COVERAGE_CHANGED = "VPFLayer.CoverageChanged";

    // Reference
    protected VPFDatabase db;
    protected ArrayList<VPFLibraryRenderable> libraries;

    // Renderables
    protected boolean drawTiles = false;
    protected double drawDistance = 1e6;
    protected int maxTilesToDraw = 4;
    protected ArrayList<VPFSymbol> symbols = new ArrayList<VPFSymbol>();
    protected ArrayList<SurfaceObject> surfaceObjects = new ArrayList<SurfaceObject>();
    protected ArrayList<GeographicText> textObjects = new ArrayList<GeographicText>();
    protected ArrayList<Renderable> renderableObjects = new ArrayList<Renderable>();

    // Renderers
    protected TiledSurfaceObjectRenderer surfaceRenderer = new TiledSurfaceObjectRenderer();
    protected GeographicTextRenderer textRenderer = new GeographicTextRenderer();
    protected VPFSymbolSupport symbolSupport = new VPFSymbolSupport(GeoSymConstants.GEOSYM, "image/png");

    // Threaded requests
    protected Queue<Runnable> requestQ = new PriorityBlockingQueue<Runnable>(4);
    protected Queue<Disposable> disposalQ = new ConcurrentLinkedQueue<Disposable>();

    // --- Inner classes ----------------------------------------------------------------------

    protected static final VPFTileInfo NULL_TILE_INFO = new VPFTileInfo(null);

    protected static class VPFTileInfo
    {
        protected VPFTile tile;
        protected Extent extent;
        protected Object globeKey;

        public VPFTileInfo(VPFTile tile)
        {
            this.tile = tile;
        }

        public VPFTile getTile()
        {
            return this.tile;
        }

        public Extent getExtent(DrawContext dc)
        {
            Object stateKey = dc.getGlobe().getStateKey(dc);
            if (this.extent == null || (stateKey != null && !stateKey.equals(globeKey)))
            {
                this.extent = dc.getGlobe().computeBoundingCylinder(dc.getVerticalExaggeration(),
                    this.tile.getBounds().toSector());
                this.globeKey = stateKey;
            }

            return this.extent;
        }

        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            VPFTileInfo that = (VPFTileInfo) o;

            if (tile != null ? !tile.equals(that.tile) : that.tile != null)
                return false;

            return true;
        }

        public int hashCode()
        {
            int result = tile != null ? tile.hashCode() : 0;
            result = 31 * result + (extent != null ? extent.hashCode() : 0);
            result = 31 * result + (globeKey != null ? globeKey.hashCode() : 0);
            return result;
        }

        public String toString()
        {
            return (this.tile != null) ? this.tile.toString() : null;
        }
    }

    protected static class VPFLibraryRenderable
    {
        protected boolean enabled = false;
        protected VPFLayer layer;
        protected VPFLibrary library;
        protected VPFCoverageRenderable referenceCoverage;
        protected ArrayList<VPFCoverageRenderable> coverages;
        protected ArrayList<VPFTileInfo> tiles;

        public VPFLibraryRenderable(VPFLayer layer, VPFLibrary library)
        {
            this.layer = layer;
            this.library = library;
            this.coverages = new ArrayList<VPFCoverageRenderable>();
            this.tiles = new ArrayList<VPFTileInfo>();

            for (VPFCoverage cov : this.library.getCoverages())
            {
                if (cov.getName().equalsIgnoreCase(VPFConstants.LIBRARY_REFERENCE_COVERAGE))
                    this.referenceCoverage = new VPFCoverageRenderable(this.layer, cov);
                else
                    this.coverages.add(new VPFCoverageRenderable(this.layer, cov));
            }

            if (this.library.hasTiledCoverages())
            {
                for (VPFTile tile : this.library.getTiles())
                {
                    this.tiles.add(new VPFTileInfo(tile));
                }
            }

            if (this.referenceCoverage != null)
            {
                this.referenceCoverage.enabled = true;
            }
        }

        public void assembleSymbols(DrawContext dc, double drawDistance, int maxTilesToDraw)
        {
            if (!this.enabled)
                return;

            if (this.referenceCoverage != null)
            {
                this.referenceCoverage.assembleSymbols(dc, null);
            }

            ArrayList<VPFTileInfo> visibleTiles = this.assembleVisibleTiles(dc, drawDistance, maxTilesToDraw);

            for (VPFCoverageRenderable cr : this.coverages)
            {
                cr.assembleSymbols(dc, (cr.coverage.isTiled() ? visibleTiles : null));
            }
        }

        public void drawTiles(DrawContext dc)
        {
            for (VPFTileInfo tile : this.tiles)
            {
                Extent extent = tile.getExtent(dc);
                if (extent instanceof Cylinder)
                    ((Cylinder) extent).render(dc);
            }
        }

        public void setCoverageEnabled(VPFCoverage coverage, boolean enabled)
        {
            VPFCoverageRenderable cr = this.getCoverageRenderable(coverage);
            if (cr != null)
                cr.enabled = enabled;

            this.layer.firePropertyChange(AVKey.LAYER, null, this.layer);
        }

        public VPFCoverageRenderable getCoverageRenderable(VPFCoverage coverage)
        {
            for (VPFCoverageRenderable cr : this.coverages)
            {
                if (cr.coverage.getFilePath().equals(coverage.getFilePath()))
                    return cr;
            }
            return null;
        }

        protected ArrayList<VPFTileInfo> assembleVisibleTiles(DrawContext dc, double drawDistance, int maxTilesToDraw)
        {
            if (!this.library.hasTiledCoverages())
                return null;

            ArrayList<VPFTileInfo> visibleTiles = new ArrayList<VPFTileInfo>();

            Frustum frustum = dc.getView().getFrustumInModelCoordinates();
            Vec4 eyePoint = dc.getView().getEyePoint();

            for (VPFTileInfo tileInfo : this.tiles)
            {
                Extent extent = tileInfo.getExtent(dc);
                double d = extent.getCenter().distanceTo3(eyePoint) - extent.getRadius();

                if (d < drawDistance && frustum.intersects(extent))
                    visibleTiles.add(tileInfo);
            }

            // Trim down list to four closest tiles
            while (visibleTiles.size() > maxTilesToDraw)
            {
                int idx = -1;
                double maxDistance = 0;
                for (int i = 0; i < visibleTiles.size(); i++)
                {
                    Extent extent = visibleTiles.get(i).getExtent(dc);
                    double distance = dc.getView().getEyePoint().distanceTo3(extent.getCenter());
                    if (distance > maxDistance)
                    {
                        maxDistance = distance;
                        idx = i;
                    }
                }
                visibleTiles.remove(idx);
            }

            return visibleTiles;
        }
    }

    protected static class VPFCoverageRenderable
    {
        protected boolean enabled = false;
        protected VPFLayer layer;
        protected VPFCoverage coverage;
        protected Map<VPFTileInfo, VPFTileRenderInfo> tileCache;

        public VPFCoverageRenderable(VPFLayer layer, VPFCoverage coverage)
        {
            this.layer = layer;
            this.coverage = coverage;
            this.tileCache = Collections.synchronizedMap(new BoundedHashMap<VPFTileInfo, VPFTileRenderInfo>(6, true)
            {
                protected boolean removeEldestEntry(Map.Entry<VPFTileInfo, VPFTileRenderInfo> eldest)
                {
                    if (!super.removeEldestEntry(eldest))
                        return false;

                    dispose(eldest.getValue());
                    return true;
                }
            });
        }

        public void assembleSymbols(DrawContext dc, ArrayList<VPFTileInfo> tiles)
        {
            if (!this.enabled)
                return;

            if (tiles == null)
            {
                this.doAssembleSymbols(dc, NULL_TILE_INFO);
                return;
            }

            for (VPFTileInfo tile : tiles)
            {
                this.doAssembleSymbols(dc, tile);
            }
        }

        protected void dispose(VPFTileRenderInfo renderInfo)
        {
            this.layer.disposalQ.add(renderInfo);
        }

        protected void doAssembleSymbols(DrawContext dc, VPFTileInfo tileInfo)
        {
            VPFTileRenderInfo renderInfo = this.tileCache.get(tileInfo);
            if (renderInfo != null && !renderInfo.isExpired(dc))
            {
                this.layer.symbols.addAll(renderInfo.symbols);
            }
            else
            {
                Globe globe = dc.getGlobe();
                this.layer.requestQ.add(new RequestTask(globe, globe.getStateKey(dc), this, tileInfo));
            }
        }
    }

    protected static class VPFTileRenderInfo implements Disposable
    {
        protected Object globeStateKey;
        protected VPFTileInfo tileInfo;
        protected ArrayList<VPFSymbol> symbols;

        public VPFTileRenderInfo(Object globeStateKey, VPFTileInfo tileInfo)
        {
            this.globeStateKey = globeStateKey;
            this.tileInfo = tileInfo;
            this.symbols = new ArrayList<VPFSymbol>();
        }

        public boolean isExpired(DrawContext dc)
        {
            return this.globeStateKey != null && !this.globeStateKey.equals(dc.getGlobe().getStateKey(dc));
        }

        public void dispose()
        {
            for (VPFSymbol s : this.symbols)
            {
                if (s.getMapObject() instanceof Disposable)
                {
                    ((Disposable) s.getMapObject()).dispose();
                }
            }

            this.symbols.clear();
        }
    }

    protected static class RequestTask implements Runnable, Comparable<RequestTask>
    {
        protected Globe globe;
        protected Object globeStateKey;
        protected VPFCoverageRenderable coverageRenderable;
        protected VPFTileInfo tileInfo;

        protected RequestTask(Globe globe, Object globeStateKey, VPFCoverageRenderable coverageRenderable,
            VPFTileInfo tileInfo)
        {
            this.globe = globe;
            this.globeStateKey = globeStateKey;
            this.coverageRenderable = coverageRenderable;
            this.tileInfo = tileInfo;
        }

        public void run()
        {
            VPFTileRenderInfo renderInfo = this.coverageRenderable.layer.loadTileData(this.globe, this.globeStateKey,
                this.coverageRenderable.coverage, this.tileInfo);
            this.coverageRenderable.tileCache.put(this.tileInfo, renderInfo);

            this.coverageRenderable.layer.firePropertyChange(AVKey.LAYER, null, this.coverageRenderable.layer);
        }

        /**
         * @param that the task to compare
         *
         * @return -1 if <code>this</code> less than <code>that</code>, 1 if greater than, 0 if equal
         *
         * @throws IllegalArgumentException if <code>that</code> is null
         */
        public int compareTo(RequestTask that)
        {
            if (that == null)
            {
                String msg = Logging.getMessage("nullValue.RequestTaskIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }

            return 0;
        }

        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            RequestTask that = (RequestTask) o;

            if (coverageRenderable != null ? !coverageRenderable.equals(that.coverageRenderable)
                : that.coverageRenderable != null)
                return false;
            //noinspection RedundantIfStatement
            if (tileInfo != null ? !tileInfo.equals(that.tileInfo) : that.tileInfo != null)
                return false;

            return true;
        }

        public int hashCode()
        {
            int result = coverageRenderable != null ? coverageRenderable.hashCode() : 0;
            result = 31 * result + (tileInfo != null ? tileInfo.hashCode() : 0);
            return result;
        }

        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("coverageRenderable=").append(this.coverageRenderable.coverage.getName());
            sb.append(", tile=").append(this.tileInfo);
            return sb.toString();
        }
    }

    protected VPFTileRenderInfo loadTileData(Globe globe, Object globeStateKey, VPFCoverage coverage,
        VPFTileInfo tileInfo)
    {
        VPFPrimitiveData primitiveData = this.createPrimitiveData(globe, coverage, tileInfo.getTile());
        if (primitiveData == null)
        {
            return null;
        }

        VPFTileRenderInfo renderInfo = new VPFTileRenderInfo(globeStateKey, tileInfo);

        // Create coverage renderables for one tile - if tile is null gets all coverage
        VPFFeatureClass[] array = VPFUtils.readFeatureClasses(coverage, new VPFFeatureTableFilter());
        for (VPFFeatureClass cls : array)
        {
            Collection<? extends VPFSymbol> symbols = this.createFeatureSymbols(globe, cls, tileInfo.getTile(),
                primitiveData);
            if (symbols != null)
            {
                renderInfo.symbols.addAll(symbols);
            }
        }

        return renderInfo;
    }

    protected VPFPrimitiveData createPrimitiveData(Globe globe, VPFCoverage coverage, VPFTile tile)
    {
        VPFPrimitiveDataFactory primitiveDataFactory = new VPFBasicPrimitiveDataFactory(tile);

        return primitiveDataFactory.createPrimitiveData(coverage);
    }

    protected Collection<? extends VPFSymbol> createFeatureSymbols(Globe globe, VPFFeatureClass featureClass,
        VPFTile tile, VPFPrimitiveData primitiveData)
    {
        VPFBasicSymbolFactory symbolFactory = new VPFBasicSymbolFactory(tile, primitiveData);
        symbolFactory.setStyleSupport(this.symbolSupport);
        symbolFactory.setGlobe(globe);

        return featureClass.createFeatureSymbols(symbolFactory);
    }

    protected AtomicLong loadTime = new AtomicLong();

    // --- VPF Layer ----------------------------------------------------------------------

    public VPFLayer()
    {
        this(null);
    }

    public VPFLayer(VPFDatabase db)
    {
        this.setName("VPF Layer");
        this.setPickEnabled(false);
        if (db != null)
            this.setVPFDatabase(db);

        this.textRenderer.setCullTextEnabled(true);
        this.textRenderer.setEffect(AVKey.TEXT_EFFECT_OUTLINE);
    }

    public VPFDatabase getVPFDatabase()
    {
        return this.db;
    }

    public void setVPFDatabase(VPFDatabase db)
    {
        this.db = db;
        this.initialize();

        this.db.addPropertyChangeListener(new PropertyChangeListener()
        {
            public void propertyChange(PropertyChangeEvent event)
            {
                if (event.getPropertyName().equals(LIBRARY_CHANGED))
                {
                    VPFLibrary library = (VPFLibrary) event.getSource();
                    boolean enabled = (Boolean) event.getNewValue();
                    setLibraryEnabled(library, enabled);
                }
                else if (event.getPropertyName().equals(COVERAGE_CHANGED))
                {
                    VPFCoverage coverage = (VPFCoverage) event.getSource();
                    boolean enabled = (Boolean) event.getNewValue();
                    setCoverageEnabled(coverage, enabled);
                }
            }
        });
    }

    protected void initialize()
    {
        this.libraries = new ArrayList<VPFLibraryRenderable>();

        for (VPFLibrary lib : db.getLibraries())
        {
            this.libraries.add(new VPFLibraryRenderable(this, lib));
        }
    }

    public void setCoverageEnabled(VPFCoverage coverage, boolean enabled)
    {
        for (VPFLibraryRenderable lr : this.libraries)
        {
            lr.setCoverageEnabled(coverage, enabled);
        }
    }

    public void doPreRender(DrawContext dc)
    {
        // Assemble renderables lists
        this.assembleRenderables(dc);
        // Handle object disposal.
        this.handleDisposal();
        // Pre render surface objects
        this.surfaceRenderer.preRender(dc);
    }

    public void doRender(DrawContext dc)
    {
        // Render the renderable lists
        this.surfaceRenderer.render(dc);                  // Surface objects
        this.textRenderer.render(dc, this.textObjects);   // Geo text
        for (Renderable r : this.renderableObjects)       // Other renderables
        {
            r.render(dc);
        }

        if (this.drawTiles)
        {
            for (VPFLibraryRenderable lr : this.libraries)
            {
                lr.drawTiles(dc);
            }
        }
    }

    public void setLibraryEnabled(VPFLibrary library, boolean enabled)
    {
        VPFLibraryRenderable lr = this.getLibraryRenderable(library);
        if (lr != null)
            lr.enabled = enabled;

        this.firePropertyChange(AVKey.LAYER, null, this);
    }

    public VPFLibraryRenderable getLibraryRenderable(VPFLibrary library)
    {
        for (VPFLibraryRenderable lr : this.libraries)
        {
            if (lr.library.getFilePath().equals(library.getFilePath()))
                return lr;
        }
        return null;
    }

    public Iterable<VPFSymbol> getActiveSymbols()
    {
        return this.symbols;
    }

    protected void assembleRenderables(DrawContext dc)
    {
        this.symbols.clear();
        this.surfaceObjects.clear();
        this.textObjects.clear();
        this.renderableObjects.clear();

        for (VPFLibraryRenderable lr : this.libraries)
        {
            lr.assembleSymbols(dc, this.drawDistance, this.maxTilesToDraw);
        }

        this.sortSymbols(this.symbols);

        // Dispatch renderable according to its class
        for (VPFSymbol symbol : this.symbols)
        {
            if (symbol.getMapObject() instanceof SurfaceObject)
                this.surfaceObjects.add((SurfaceObject) symbol.getMapObject());
            else if (symbol.getMapObject() instanceof GeographicText)
                this.textObjects.add((GeographicText) symbol.getMapObject());
            else if (symbol.getMapObject() instanceof Renderable)
                this.renderableObjects.add((Renderable) symbol.getMapObject());
        }

        this.surfaceRenderer.setSurfaceObjects(this.surfaceObjects);

        this.sendRequests();
        this.requestQ.clear();
    }

    protected void sortSymbols(List<VPFSymbol> list)
    {
        Collections.sort(list, new VPFSymbolComparator());
    }

    protected void handleDisposal()
    {
        Disposable disposable;
        while ((disposable = this.disposalQ.poll()) != null)
        {
            disposable.dispose();
        }
    }

    protected void sendRequests()
    {
        Runnable task;
        while ((task = this.requestQ.poll()) != null)
        {
            if (!WorldWind.getTaskService().isFull())
            {
                WorldWind.getTaskService().addTask(task);
            }
        }
    }
}
