/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind;

import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.pick.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.terrain.*;

import java.util.*;

/**
 * @author Tom Gaskins
 * @version $Id: SceneController.java 11421 2009-06-03 13:23:25Z tgaskins $
 */
public interface SceneController extends WWObject, Disposable
{
    public Model getModel();

    public void setModel(Model model);

    public View getView();

    public void setView(View view);

    public int repaint();

    void setVerticalExaggeration(double verticalExaggeration);

    double getVerticalExaggeration();

    PickedObjectList getPickedObjectList();

    double getFramesPerSecond();

    double getFrameTime();

    void setPickPoint(java.awt.Point pickPoint);

    java.awt.Point getPickPoint();

    void setTextureCache(TextureCache textureCache);

    Collection<PerformanceStatistic> getPerFrameStatistics();

    void setPerFrameStatisticsKeys(Set<String> keys);

    SectorGeometryList getTerrain();

    DrawContext getDrawContext();

    void reinitialize();

    ScreenCreditController getScreenCreditController();

    void setScreenCreditController(ScreenCreditController screenCreditRenderer);
}
