/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package wwj.gui;

import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.examples.ClickAndGoSelectListener;
import gov.nasa.worldwind.examples.LayerPanel;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.WorldMapLayer;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;

/**
 *
 * @author david
 */
public class PanelWorldWind extends JPanel {

    public WorldWindowGLCanvas wwd;
    public StatusBar statusBar;
    public LayerPanel layerPanel;
    private ArrayList<Position> positions = new ArrayList<Position>();
    private final RenderableLayer layer;
    private final Polyline line;
    private boolean initialPosition = false;
    private double alt = 0, lon = 0, lat = 0, alt_1 = 0, lat_1 = 0, lon_1 = 0;
    private double offset = 0;

    public PanelWorldWind(Dimension canvasSize, boolean includeStatusBar) {

        super(new BorderLayout());

        setBackground(Color.white);
        wwd = new WorldWindowGLCanvas();
        wwd.setBackground(Color.white);
        this.wwd.setPreferredSize(canvasSize);        

        Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
        this.wwd.setModel(m);
        wwd.addSelectListener(new ClickAndGoSelectListener(wwd, WorldMapLayer.class));

        this.line = new Polyline();
        this.line.setFollowTerrain(true);
        this.layer = new RenderableLayer();

        this.layer.addRenderable(this.line);
        this.wwd.getModel().getLayers().add(this.layer);

        add(this.wwd, BorderLayout.CENTER);
        LayerPanel layerPanel = new LayerPanel(wwd, null);
        layerPanel.setBackground(Color.white);

        for (int i = 0; i < layerPanel.getComponents().length; i++) {
            layerPanel.getComponent(i).setBackground(Color.white);
        }

        add(layerPanel, BorderLayout.WEST);

        if (includeStatusBar) {
            this.statusBar = new StatusBar();
            statusBar.setBackground(Color.white);
            this.add(statusBar, BorderLayout.PAGE_END);
            this.statusBar.setEventSource(wwd);
        }



    }

    public PanelWorldWind(Dimension canvasSize, boolean includeStatusBar, double _offset) {

        super(new BorderLayout());
        offset = _offset;
        setBackground(Color.white);
        wwd = new WorldWindowGLCanvas();
        wwd.setBackground(Color.white);
        this.wwd.setPreferredSize(canvasSize);

        Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
        this.wwd.setModel(m);
        wwd.addSelectListener(new ClickAndGoSelectListener(wwd, WorldMapLayer.class));

        this.line = new Polyline();
        this.line.setFollowTerrain(true);
        this.layer = new RenderableLayer();

        this.layer.addRenderable(this.line);
        this.wwd.getModel().getLayers().add(this.layer);

        add(this.wwd, BorderLayout.CENTER);
        LayerPanel layerPanel = new LayerPanel(wwd, null);
        layerPanel.setBackground(Color.white);

        for (int i = 0; i < layerPanel.getComponents().length; i++) {
            layerPanel.getComponent(i).setBackground(Color.white);
        }

        add(layerPanel, BorderLayout.WEST);

        if (includeStatusBar) {
            this.statusBar = new StatusBar();
            statusBar.setBackground(Color.white);
            this.add(statusBar, BorderLayout.PAGE_END);
            this.statusBar.setEventSource(wwd);
        }



    }

    public WorldWindowGLCanvas getWwd() {
        return this.wwd;
    }

    public void setInitPosition(double _lat, double _lon, double _alt) {

        lat = _lat;
        lon = _lon;
        alt = _alt;
        Angle lonAngle = Angle.fromDegrees(lon);
        Angle latAngle = Angle.fromDegrees(lat);
        Position targetPos = new Position(latAngle, lonAngle, alt + offset);
        BasicOrbitView view = (BasicOrbitView) getWwd().getView();
        view.addPanToAnimator(targetPos, Angle.ZERO, Angle.ZERO, 1);

    }

    public void addPosition(double _lat, double _lon, double _alt) {

        lat = _lat;
        lon = _lon;
        alt = _alt;

        setInitPosition(lat, lon, alt);

        if (lat != lat_1 || lon != lon_1 || alt != alt_1) {
            Angle lonAngle = Angle.fromDegrees(lon);
            Angle latAngle = Angle.fromDegrees(lat);
            Position curPos = new Position(latAngle, lonAngle, alt + offset);
            this.positions.add(curPos);
            this.line.setPositions(this.positions);
            this.firePropertyChange("LineBuilder.AddPosition", null, curPos);
            this.wwd.redraw();
            //System.out.println("It was drawn");
        }

        lat_1 = lat;
        lon_1 = lon;
        alt_1 = alt;

    }

    void reSizeView(double _offset) {
        offset = _offset;
        setInitPosition(lat, lon, alt);
    }
}
