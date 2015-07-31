/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.terrain;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.pick.Pickable;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;

import java.awt.*;
import java.util.ArrayList;
import java.nio.DoubleBuffer;

/**
 * @author Tom Gaskins
 * @version $Id: SectorGeometry.java 8686 2009-01-31 04:16:25Z tgaskins $
 */
public interface SectorGeometry extends Renderable, Pickable
{
    Extent getExtent();

    Sector getSector();

    Vec4 getSurfacePoint(Angle latitude, Angle longitude, double metersOffset);

    void renderMultiTexture(DrawContext dc, int numTextureUnits);

    void renderWireframe(DrawContext dc, boolean interior, boolean exterior);

    void renderBoundingVolume(DrawContext dc);

    PickedObject[] pick(DrawContext dc, java.util.List<? extends Point> pickPoints);

    Intersection[] intersect(Line line);

    Intersection[] intersect(double elevation);

    DoubleBuffer makeTextureCoordinates(GeographicTextureCoordinateComputer computer);

    public interface GeographicTextureCoordinateComputer
    {
        double[] compute(Angle latitude, Angle longitude);
    }

    // Extraction of portions of the current tessellation inside given CONVEX regions:
    // The returned "ExtractedShapeDescription" consists of (i) the interior polygons
    // (ArrayList<Vec4[]> interiorPolys) which is the set of tessellation triangles trimmed
    // to the convex region, and (ii) the boundary edges (ArrayList<BoundaryEdge> shapeOutline)
    // which is an unordered set of pairs of vertices comprising the outer boundary of the
    // extracted region. If a boundary edge is a straight line segment, then BoundaryEdge.toMidPoint
    // will be null. Otherwise it is a vector pointing from the midpoint of the two bounding
    // vertices towards the portion of the original convex extraction region. This allows the
    // client to supersample the edge at a resolution higher than that of the current tessellation.
    public class BoundaryEdge
    {
        public Vec4[]  vertices;  // an element of the returned ArrayList
        public int     i1, i2;    // a pair of indices into 'vertices' describing an exterior edge
        public Vec4    toMidPoint;// if the extracted edge is linear (e.g., if the caller was the
                           // 'Plane[] p' variation below), this will be null; if the edge
                           // is nonlinear (e.g., the elliptical cylinder variation below),
                           // this vector will point from the midpoint of (i1,i2) towards
                           // the center of the desired edge.
        public BoundaryEdge(Vec4[] v, int i1, int i2)
        {
            this(v,i1,i2,null);
        }
        public BoundaryEdge(Vec4[] v, int i1, int i2, Vec4 toMid)
        {
            this.vertices = v; this.i1 = i1; this.i2 = i2; this.toMidPoint = toMid;
        }
    }
    public static class ExtractedShapeDescription
    {
        public ArrayList<Vec4[]>         interiorPolys;
        public ArrayList<BoundaryEdge>   shapeOutline;

        public ExtractedShapeDescription(ArrayList<Vec4[]> ip,
            ArrayList<SectorGeometry.BoundaryEdge> so)
        {
            this.interiorPolys = ip; this.shapeOutline = so;
        }
    }

    // Get tessellation pieces inside the intersection of a set of planar half spaces
    ExtractedShapeDescription getIntersectingTessellationPieces(Plane[] p);

    // Get tessellation pieces inside an elliptical cylinder
    ExtractedShapeDescription getIntersectingTessellationPieces(Vec4 Cxyz, Vec4 uHat, Vec4 vHat,
        double uRadius, double vRadius);
}
