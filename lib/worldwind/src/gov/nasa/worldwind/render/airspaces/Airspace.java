/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render.airspaces;

import gov.nasa.worldwind.Restorable;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;

import java.util.Collection;

/**
 * @author dcollins
 * @version $Id: Airspace.java 9230 2009-03-06 05:36:26Z dcollins $
 */
public interface Airspace extends Renderable, Restorable, AVList
{
    public static final String DRAW_STYLE_FILL = "Airspace.DrawStyleFill";
    public static final String DRAW_STYLE_OUTLINE = "Airspace.DrawStyleOutline";

    boolean isVisible();

    void setVisible(boolean visible);

    AirspaceAttributes getAttributes();

    void setAttributes(AirspaceAttributes attributes);

    double[] getAltitudes();

    void setAltitudes(double lowerAltitude, double upperAltitude);

    void setAltitude(double altitude);

    boolean[] isTerrainConforming();

    void setTerrainConforming(boolean lowerTerrainConformant, boolean upperTerrainConformant);

    void setTerrainConforming(boolean terrainConformant);

    boolean isEnableLevelOfDetail();

    void setEnableLevelOfDetail(boolean enableLevelOfDetail);

    Iterable<DetailLevel> getDetailLevels();

    void setDetailLevels(Collection<DetailLevel> detailLevels);

    boolean isAirspaceVisible(DrawContext dc);

    Extent getExtent(DrawContext dc);

    void renderGeometry(DrawContext dc, String drawStyle);

    void renderExtent(DrawContext dc);
}
