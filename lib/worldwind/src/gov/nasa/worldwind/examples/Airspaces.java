/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.render.airspaces.*;
import gov.nasa.worldwind.render.airspaces.Box;
import gov.nasa.worldwind.render.airspaces.Polygon;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.view.orbit.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: Airspaces.java 12530 2009-08-29 17:55:54Z jterhorst $
 */
public class Airspaces extends ApplicationTemplate
{
    public static final String ACTION_COMMAND_ANTIALIAS = "gov.nasa.worldwind.avkey.ActionCommandAntialias";
    public static final String ACTION_COMMAND_DEPTH_OFFSET = "gov.nasa.worldwind.avkey.ActionCommandDepthOffset";
    public static final String ACTION_COMMAND_DRAW_EXTENT = "gov.nasa.worldwind.avkey.ActionCommandDrawExtent";
    public static final String ACTION_COMMAND_DRAW_WIREFRAME = "gov.nasa.worldwind.avkey.ActionCommandDrawWireframe";
    public static final String ACTION_COMMAND_LOAD_DEMO_AIRSPACES = "ActionCommandLoadDemoAirspaces";
    public static final String ACTION_COMMAND_LOAD_INTERSECTING_AIRSPACES = "ActionCommandLoadIntersectingAirspaces";
    public static final String ACTION_COMMAND_ZOOM_TO_DEMO_AIRSPACES = "ActionCommandZoomToDemoAirspaces";
    public static final String ACTION_COMMAND_SAVE_AIRSPACES = "ActionCommandSaveAirspaces";
    public static final String ACTION_COMMAND_READ_AIRSPACES = "ActionCommandReadAirspaces";
    public static final String ACTION_COMMAND_VERTICAL_EXAGGERATION = "ActionCommandVerticalExaggeration";
    public static final String AIRSPACE_LAYER_NAME = "Airspaces";
    public static final String ANNOTATION_LAYER_NAME = "Annotations";
    public static final String DESCRIPTION = "Description";

    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        private AirspacesController controller;
        private LayerPanel layerPanel;

        public AppFrame()
        {
            // We add our own LayerPanel, but keep the StatusBar from ApplicationTemplate.
            super(true, false, false);
            this.controller = new AirspacesController(this.getWwd());
            this.controller.frame = this;
            this.makeComponents();

            //((FlatGlobe) this.getWwd().getModel().getGlobe()).setProjection(FlatGlobe.PROJECTION_MODIFIED_SINUSOIDAL);

            this.controller.actionPerformed(new ActionEvent(this, 0, ACTION_COMMAND_LOAD_DEMO_AIRSPACES));
            this.getLayerPanel().update(this.getWwd());

            this.pack();
        }

        public LayerPanel getLayerPanel()
        {
            return this.layerPanel;
        }

        private void makeComponents()
        {
            this.getWwd().setPreferredSize(new Dimension(1024, 768));

            JPanel panel = new JPanel(new BorderLayout());
            {
                panel.setBorder(new EmptyBorder(10, 0, 10, 0));

                JPanel controlPanel = new JPanel(new BorderLayout(0, 10));
                controlPanel.setBorder(new EmptyBorder(20, 10, 20, 10));

                JPanel btnPanel = new JPanel(new GridLayout(5, 1, 0, 5));
                {
                    JButton btn = new JButton("Load Demo Airspaces");
                    btn.setActionCommand(ACTION_COMMAND_LOAD_DEMO_AIRSPACES);
                    btn.addActionListener(this.controller);
                    btnPanel.add(btn);

                    btn = new JButton("Load Intersecting Airspaces");
                    btn.setActionCommand(ACTION_COMMAND_LOAD_INTERSECTING_AIRSPACES);
                    btn.addActionListener(this.controller);
                    btnPanel.add(btn);

                    btn = new JButton("Zoom to Demo Airspaces");
                    btn.setActionCommand(ACTION_COMMAND_ZOOM_TO_DEMO_AIRSPACES);
                    btn.addActionListener(this.controller);
                    btnPanel.add(btn);

                    btn = new JButton("Save Airspaces");
                    btn.setActionCommand(ACTION_COMMAND_SAVE_AIRSPACES);
                    btn.addActionListener(this.controller);
                    btnPanel.add(btn);
                    controller.saveButton = btn;

                    btn = new JButton("Read Airspaces");
                    btn.setActionCommand(ACTION_COMMAND_READ_AIRSPACES);
                    btn.addActionListener(this.controller);
                    btnPanel.add(btn);
                    controller.readButton = btn;
                }
                controlPanel.add(btnPanel, BorderLayout.NORTH);

                JComponent box = javax.swing.Box.createVerticalBox();
                {
                    JCheckBox cb = new JCheckBox("Antialias", this.controller.airspaceLayer.isEnableAntialiasing());
                    cb.setActionCommand(ACTION_COMMAND_ANTIALIAS);
                    cb.addActionListener(this.controller);
                    box.add(cb);

                    cb = new JCheckBox("Fix Z-Fighting", this.controller.airspaceLayer.isEnableDepthOffset());
                    cb.setActionCommand(ACTION_COMMAND_DEPTH_OFFSET);
                    cb.addActionListener(this.controller);
                    box.add(cb);

                    cb = new JCheckBox("Show Wireframe", this.controller.airspaceLayer.isDrawWireframe());
                    cb.setActionCommand(ACTION_COMMAND_DRAW_WIREFRAME);
                    cb.addActionListener(this.controller);
                    box.add(cb);

                    cb = new JCheckBox("Show Bounds", this.controller.airspaceLayer.isDrawExtents());
                    cb.setActionCommand(ACTION_COMMAND_DRAW_EXTENT);
                    cb.addActionListener(this.controller);
                    box.add(cb);
                }
                controlPanel.add(box, BorderLayout.CENTER);

                JPanel vePanel = new JPanel(new BorderLayout(0, 5));
                {
                    JLabel label = new JLabel("Vertical Exaggeration");
                    vePanel.add(label, BorderLayout.NORTH);

                    int MIN_VE = 1;
                    int MAX_VE = 8;
                    int curVe = (int) this.getWwd().getSceneController().getVerticalExaggeration();
                    curVe = curVe < MIN_VE ? MIN_VE : (curVe > MAX_VE ? MAX_VE : curVe);
                    JSlider slider = new JSlider(MIN_VE, MAX_VE, curVe);
                    slider.setMajorTickSpacing(1);
                    slider.setPaintTicks(true);
                    slider.setSnapToTicks(true);
                    Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
                    labelTable.put(1, new JLabel("1x"));
                    labelTable.put(2, new JLabel("2x"));
                    labelTable.put(4, new JLabel("4x"));
                    labelTable.put(8, new JLabel("8x"));
                    slider.setLabelTable(labelTable);
                    slider.setPaintLabels(true);
                    slider.addChangeListener(new ChangeListener()
                    {
                        public void stateChanged(ChangeEvent e)
                        {
                            double ve = ((JSlider) e.getSource()).getValue();
                            ActionEvent ae = new ActionEvent(ve, 0, ACTION_COMMAND_VERTICAL_EXAGGERATION);
                            controller.actionPerformed(ae);
                        }
                    });
                    vePanel.add(slider, BorderLayout.SOUTH);
                }
                controlPanel.add(vePanel, BorderLayout.SOUTH);

                panel.add(controlPanel, BorderLayout.SOUTH);

                this.layerPanel = new LayerPanel(this.getWwd(), null);
                panel.add(this.layerPanel, BorderLayout.CENTER);
            }
            getContentPane().add(panel, BorderLayout.WEST);
        }
    }

    public static class AirspacesController implements ActionListener
    {
        // AWT/Swing stuff.
        private JFileChooser fileChooser;
        private JButton readButton;
        private JButton saveButton;
        private Airspaces.AppFrame frame;
        // World Wind stuff.
        private WorldWindowGLCanvas wwd;
        private AirspaceLayer airspaceLayer;
        private AnnotationLayer annotationLayer;
        private Airspace lastHighlit;
        private Airspace lastToolTip;
        private AirspaceAttributes lastAttrs;
        private Annotation lastAnnotation;
        private BasicDragger dragger;

        public AirspacesController(WorldWindowGLCanvas wwd)
        {
            this.wwd = wwd;

            // Construct a layer that will hold the airspaces and annotations.
            this.airspaceLayer = new AirspaceLayer();
            this.annotationLayer = new AnnotationLayer();
            this.airspaceLayer.setName(AIRSPACE_LAYER_NAME);
            this.annotationLayer.setName(ANNOTATION_LAYER_NAME);
            insertBeforePlacenames(this.wwd, this.airspaceLayer);
            insertBeforePlacenames(this.wwd, this.annotationLayer);

            this.initializeSelectionMonitoring();
        }

        public void actionPerformed(ActionEvent e)
        {
            if (ACTION_COMMAND_LOAD_DEMO_AIRSPACES.equalsIgnoreCase(e.getActionCommand()))
            {
                this.airspaceLayer.removeAllAirspaces();
                this.doLoadDemoAirspaces();
            }
            else if (ACTION_COMMAND_LOAD_INTERSECTING_AIRSPACES.equalsIgnoreCase(e.getActionCommand()))
            {
                this.airspaceLayer.removeAllAirspaces();
                this.doLoadIntersectingAirspaces();
            }
            else if (ACTION_COMMAND_ZOOM_TO_DEMO_AIRSPACES.equalsIgnoreCase(e.getActionCommand()))
            {
                this.doZoomToAirspaces();
            }
            else if (ACTION_COMMAND_SAVE_AIRSPACES.equalsIgnoreCase(e.getActionCommand()))
            {
                this.doSaveAirspaces();
            }
            else if (ACTION_COMMAND_READ_AIRSPACES.equalsIgnoreCase(e.getActionCommand()))
            {
                this.doReadAirspaces();
            }
            else if (ACTION_COMMAND_VERTICAL_EXAGGERATION.equalsIgnoreCase(e.getActionCommand()))
            {
                Double ve = (Double) e.getSource();
                this.doSetVerticalExaggeration(ve);
                this.wwd.redraw();
            }
            else if (ACTION_COMMAND_ANTIALIAS.equalsIgnoreCase(e.getActionCommand()))
            {
                JCheckBox cb = (JCheckBox) e.getSource();
                this.airspaceLayer.setEnableAntialiasing(cb.isSelected());
                this.wwd.redraw();
            }
            else if (ACTION_COMMAND_DEPTH_OFFSET.equalsIgnoreCase(e.getActionCommand()))
            {
                JCheckBox cb = (JCheckBox) e.getSource();
                this.airspaceLayer.setEnableDepthOffset(cb.isSelected());
                this.wwd.redraw();
            }
            else if (ACTION_COMMAND_DRAW_WIREFRAME.equalsIgnoreCase(e.getActionCommand()))
            {
                JCheckBox cb = (JCheckBox) e.getSource();
                this.airspaceLayer.setDrawWireframe(cb.isSelected());
                this.wwd.redraw();
            }
            else if (ACTION_COMMAND_DRAW_EXTENT.equalsIgnoreCase(e.getActionCommand()))
            {
                JCheckBox cb = (JCheckBox) e.getSource();
                this.airspaceLayer.setDrawExtents(cb.isSelected());
                this.wwd.redraw();
            }
        }

        public void setAirspaces(Collection<Airspace> airspaces)
        {
            this.airspaceLayer.removeAllAirspaces();
            if (airspaces != null)
            {
                for (Airspace a : airspaces)
                {
                    if (a != null)
                    {
                        this.airspaceLayer.addAirspace(a);
                    }
                }
            }
        }

        public void initializeSelectionMonitoring()
        {
            this.dragger = new BasicDragger(this.wwd);
            this.wwd.addSelectListener(new SelectListener()
            {
                public void selected(SelectEvent event)
                {
                    if (lastHighlit != null
                        && (event.getTopObject() == null || !event.getTopObject().equals(lastHighlit)))
                    {
                        lastHighlit.setAttributes(lastAttrs);
                        lastHighlit = null;
                    }

                    if (lastToolTip != null
                        && (event.getTopObject() == null || !event.getTopObject().equals(lastHighlit)))
                    {
                        annotationLayer.removeAnnotation(lastAnnotation);
                        lastToolTip = null;
                    }

                    // Have rollover events highlight the rolled-over object.
                    if (event.getEventAction().equals(SelectEvent.ROLLOVER) && !dragger.isDragging())
                    {
                        if (event.getTopObject() != null && event.getTopPickedObject().getParentLayer() != null)
                        {
                            if (event.getTopPickedObject().getParentLayer() == airspaceLayer)
                            {
                                AirspacesController.this.highlight(event.getTopObject());
                                AirspacesController.this.wwd.redraw();
                            }
                        }
                    }
                    // Have hover events popup an annotation about the hovered-over object.
                    else if (event.getEventAction().equals(SelectEvent.HOVER) && !dragger.isDragging())
                    {
                        if (event.getTopObject() != null && event.getTopPickedObject().getParentLayer() != null)
                        {
                            if (event.getTopPickedObject().getParentLayer() == airspaceLayer)
                            {
                                AirspacesController.this.popupToolTip(event.getTopObject(), event);
                                AirspacesController.this.wwd.redraw();
                            }
                        }
                    }
                    // Have drag events drag the selected object.
                    else if (event.getEventAction().equals(SelectEvent.DRAG_END)
                          || event.getEventAction().equals(SelectEvent.DRAG))
                    {
                        // Delegate dragging computations to a dragger.
                        dragger.selected(event);

                        // We missed any roll-over events while dragging, so highlight any under the cursor now,
                        // or de-highlight the dragged shape if it's no longer under the cursor.
                        if (event.getEventAction().equals(SelectEvent.DRAG_END))
                        {
                            PickedObjectList pol = AirspacesController.this.wwd.getObjectsAtCurrentPosition();
                            if (pol != null)
                            {
                                AirspacesController.this.highlight(pol.getTopObject());
                                AirspacesController.this.wwd.repaint();
                            }
                        }
                    }
                }
            });
        }

        private void highlight(Object o)
        {
            if (lastHighlit == o)
                return; // same thing selected

            if (lastHighlit == null && o instanceof Airspace)
            {
                lastHighlit = (Airspace) o;
                lastAttrs = lastHighlit.getAttributes();
                BasicAirspaceAttributes highliteAttrs = new BasicAirspaceAttributes(lastAttrs);
                highliteAttrs.setMaterial(Material.WHITE);
                lastHighlit.setAttributes(highliteAttrs);
            }
        }

        private void popupToolTip(Object o, SelectEvent e)
        {
            if (lastToolTip == o)
                return;

            if (lastToolTip == null && o instanceof Airspace)
            {
                lastToolTip = (Airspace) o;
                lastAnnotation = this.createToolTip((Airspace) o, e);
                annotationLayer.addAnnotation(lastAnnotation);
            }
        }

        private Annotation createToolTip(Airspace a, SelectEvent e)
        {
            Object o = a.getValue(DESCRIPTION);
            if (o == null)
                o = a.getClass().getName();

            java.awt.Point point = e.getPickPoint();

            Position pos = wwd.getView().computePositionFromScreenPoint(point.x, point.y);
            if (pos != null)
            {
                double[] altitudes = a.getAltitudes();
                pos = new Position(pos.getLatitude(), pos.getLongitude(), altitudes[1]);
                return new GlobeAnnotation(o.toString(), pos);
            }
            else
            {
                return new ScreenAnnotation(o.toString(), point);
            }
        }

        private void setupDefaultMaterial(Airspace a, Color color)
        {
            Color outlineColor = makeBrighter(color);

            a.getAttributes().setDrawOutline(true);
            a.getAttributes().setMaterial(new Material(color));
            a.getAttributes().setOutlineMaterial(new Material(outlineColor));
            a.getAttributes().setOpacity(0.8);
            a.getAttributes().setOutlineOpacity(0.9);
            a.getAttributes().setOutlineWidth(3.0);
        }

        private static Color makeBrighter(Color color)
        {
            float[] hsbComponents = new float[3];
            Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsbComponents);
            float hue = hsbComponents[0];
            float saturation = hsbComponents[1];
            float brightness = hsbComponents[2];

            saturation /= 3f;
            brightness *= 3f;

            if (saturation < 0f)
                saturation = 0f;

            if (brightness > 1f)
                brightness = 1f;

            int rgbInt = Color.HSBtoRGB(hue, saturation, brightness);
            
            return new Color(rgbInt);
        }

        public void doLoadDemoAirspaces()
        {
            ArrayList<Airspace> airspaces = new ArrayList<Airspace>();

            // Cylinder.
            CappedCylinder cyl = new CappedCylinder();
            cyl.setCenter(LatLon.fromDegrees(47.7477, -123.6372));
            cyl.setRadius(30000.0);
            cyl.setAltitudes(5000.0, 10000.0);
            cyl.setTerrainConforming(true, true);
            cyl.setValue(DESCRIPTION, "30,000m Radius Cylinder. Top & bottom terrain conformance.");
            this.setupDefaultMaterial(cyl, Color.BLUE);
            airspaces.add(cyl);

            // Continent-sized cylinder.
            cyl = new CappedCylinder();
            cyl.setCenter(LatLon.fromDegrees(0.0, 0.0));
            cyl.setRadius(3000000.0);
            cyl.setAltitudes(100000.0, 500000.0);
            cyl.setTerrainConforming(true, false);
            cyl.setValue(DESCRIPTION, "3,000,000m Cylinder.");
            this.setupDefaultMaterial(cyl, Color.RED);
            airspaces.add(cyl);

            // Radarc
            PartialCappedCylinder partCyl = new PartialCappedCylinder();
            partCyl.setCenter(LatLon.fromDegrees(46.7477, -123.6372));
            partCyl.setAltitudes(5000.0, 10000.0);
            partCyl.setTerrainConforming(false, true);
            // To render a Radarc,
            // (1) Specify inner radius and outer radius.
            // (2) Specify start and stop azimuth.
            partCyl.setRadii(15000.0, 30000.0);
            partCyl.setAzimuths(Angle.fromDegrees(0.0), Angle.fromDegrees(90.0));
            partCyl.setValue(DESCRIPTION, "Partial Cylinder from 0 to 90 degrees.");
            this.setupDefaultMaterial(partCyl, Color.DARK_GRAY);
            airspaces.add(partCyl);

            // Radarc
            partCyl = new PartialCappedCylinder();
            partCyl.setCenter(LatLon.fromDegrees(46.7477, -122.6372));
            partCyl.setAltitudes(5000.0, 10000.0);
            partCyl.setTerrainConforming(true, true);
            // To render a Radarc,
            // (1) Specify inner radius and outer radius.
            // (2) Specify start and stop azimuth.
            partCyl.setRadii(15000.0, 30000.0);
            partCyl.setAzimuths(Angle.fromDegrees(90.0), Angle.fromDegrees(0.0));
            partCyl.setValue(DESCRIPTION, "Partial Cylinder from 90 to 0 degrees.");
            this.setupDefaultMaterial(partCyl, Color.GRAY);
            airspaces.add(partCyl);

            // Cake
            Cake cake = new Cake();
            cake.setLayers(Arrays.asList(
                new Cake.Layer(LatLon.fromDegrees(46.7477, -121.6372), 10000.0, Angle.fromDegrees(190.0),
                    Angle.fromDegrees(170.0), 10000.0, 15000.0),
                new Cake.Layer(LatLon.fromDegrees(46.7477, -121.6372), 15000.0, Angle.fromDegrees(190.0),
                    Angle.fromDegrees(90.0), 16000.0, 21000.0),
                new Cake.Layer(LatLon.fromDegrees(46.7477, -121.6372), 12500.0, Angle.fromDegrees(270.0),
                    Angle.fromDegrees(60.0), 22000.0, 27000.0)));
            cake.getLayers().get(0).setTerrainConforming(false, false);
            cake.getLayers().get(1).setTerrainConforming(false, false);
            cake.getLayers().get(2).setTerrainConforming(false, true);
            cake.setValue(DESCRIPTION, "3 layer Cake.");
            this.setupDefaultMaterial(cake, Color.YELLOW);
            airspaces.add(cake);

            cake = new Cake();
            cake.setLayers(Arrays.asList(
                new Cake.Layer(LatLon.fromDegrees(36, -121), 10000.0, Angle.fromDegrees(0.0),
                    Angle.fromDegrees(360.0), 10000.0, 15000.0),
                new Cake.Layer(LatLon.fromDegrees(36.1, -121.1), 15000.0, Angle.fromDegrees(0.0),
                    Angle.fromDegrees(360.0), 16000.0, 21000.0),
                new Cake.Layer(LatLon.fromDegrees(35.9, -120.9), 12500.0, Angle.fromDegrees(0.0),
                    Angle.fromDegrees(360.0), 22000.0, 27000.0)));
            cake.getLayers().get(0).setTerrainConforming(true, true);
            cake.getLayers().get(1).setTerrainConforming(true, true);
            cake.getLayers().get(2).setTerrainConforming(true, true);
            cake.setValue(DESCRIPTION, "3 layer Cake. With disjoint layers.");
            this.setupDefaultMaterial(cake, Color.MAGENTA);
            airspaces.add(cake);

            // Left Orbit
            Orbit orbit = new Orbit();
            orbit.setLocations(LatLon.fromDegrees(45.7477, -123.6372), LatLon.fromDegrees(45.7477, -122.6372));
            orbit.setAltitudes(10000.0, 20000.0);
            orbit.setWidth(30000.0);
            orbit.setOrbitType(Orbit.OrbitType.LEFT);
            orbit.setTerrainConforming(false, true);
            orbit.setValue(DESCRIPTION, "LEFT Orbit.");
            this.setupDefaultMaterial(orbit, Color.LIGHT_GRAY);
            airspaces.add(orbit);

            // Center Orbit
            orbit = new Orbit();
            orbit.setLocations(LatLon.fromDegrees(45.7477, -123.6372), LatLon.fromDegrees(45.7477, -122.6372));
            orbit.setAltitudes(15000.0, 25000.0);
            orbit.setWidth(30000.0);
            orbit.setOrbitType(Orbit.OrbitType.CENTER);
            orbit.setTerrainConforming(true, false);
            orbit.setValue(DESCRIPTION, "CENTER Orbit.");
            this.setupDefaultMaterial(orbit, Color.GRAY);
            airspaces.add(orbit);

            // Right Orbit
            orbit = new Orbit();
            orbit.setLocations(LatLon.fromDegrees(45.7477, -123.6372), LatLon.fromDegrees(45.7477, -122.6372));
            orbit.setAltitudes(10000.0, 20000.0);
            orbit.setWidth(30000.0);
            orbit.setOrbitType(Orbit.OrbitType.RIGHT);
            orbit.setTerrainConforming(true, true);
            orbit.setValue(DESCRIPTION, "RIGHT Orbit.");
            this.setupDefaultMaterial(orbit, Color.DARK_GRAY);
            airspaces.add(orbit);

            // Orbit from Los Angeles to New York
            orbit = new Orbit();
            orbit.setLocations(LatLon.fromDegrees(34.0489, -118.2481), LatLon.fromDegrees(40.7137, -74.0065));
            orbit.setAltitudes(10000.0, 100000.0);
            orbit.setWidth(500000.0);
            orbit.setOrbitType(Orbit.OrbitType.CENTER);
            orbit.setTerrainConforming(true, false);
            orbit.setValue(DESCRIPTION, "Orbit From L.A. to N.Y.");
            this.setupDefaultMaterial(orbit, Color.RED);
            airspaces.add(orbit);

            // Curtain around Snohomish County, WA
            Curtain curtain = new Curtain();
            curtain.setLocations(makeLatLon(SNOHOMISH_COUNTY));
            curtain.setAltitudes(5000.0, 10000.0);
            curtain.setTerrainConforming(true, false);
            curtain.setValue(DESCRIPTION, "Curtain around Snohomish County, WA.");
            this.setupDefaultMaterial(curtain, Color.GREEN);
            airspaces.add(curtain);

            // Curtain around San Juan County, WA
            curtain = new Curtain();
            curtain.setLocations(makeLatLon(SAN_JUAN_COUNTY_2));
            curtain.setAltitudes(5000.0, 10000.0);
            curtain.setTerrainConforming(true, true);
            curtain.setValue(DESCRIPTION, "Curtain around San Juan County, WA.");
            this.setupDefaultMaterial(curtain, Color.GREEN);
            airspaces.add(curtain);

            // Curtains of different path types crossing the dateline.
            curtain = new Curtain();
            curtain.setLocations(Arrays.asList(LatLon.fromDegrees(27.0, -112.0), LatLon.fromDegrees(35.0, 138.0)));
            curtain.setAltitudes(1000.0, 100000.0);
            curtain.setTerrainConforming(true, false);
            curtain.setValue(DESCRIPTION, "Great arc Curtain from America to Japan.");
            this.setupDefaultMaterial(curtain, Color.MAGENTA);
            airspaces.add(curtain);

            curtain = new Curtain();
            curtain.setLocations(Arrays.asList(LatLon.fromDegrees(27.0, -112.0), LatLon.fromDegrees(35.0, 138.0)));
            curtain.setPathType(AVKey.RHUMB_LINE);
            curtain.setAltitudes(1000.0, 100000.0);
            curtain.setTerrainConforming(false, true);
            curtain.setValue(DESCRIPTION, "Rhumb Curtain from America to Japan.");
            this.setupDefaultMaterial(curtain, Color.CYAN);
            airspaces.add(curtain);

            // Polygons of San Juan County, WA
            Polygon poly = new Polygon();
            poly.setLocations(makeLatLon(SAN_JUAN_COUNTY_1));
            poly.setAltitudes(5000.0, 10000.0);
            poly.setTerrainConforming(true, true);
            poly.setValue(DESCRIPTION, "Polygon of San Juan County, WA.");
            this.setupDefaultMaterial(poly, Color.GREEN);
            airspaces.add(poly);

            poly = new Polygon();
            poly.setLocations(makeLatLon(SAN_JUAN_COUNTY_3));
            poly.setAltitudes(5000.0, 10000.0);
            poly.setTerrainConforming(true, true);
            poly.setValue(DESCRIPTION, "Polygon of San Juan County, WA.");
            this.setupDefaultMaterial(poly, Color.GREEN);
            airspaces.add(poly);

            // Polygon over the Sierra Nevada mountains.
            poly = new Polygon();
            poly.setLocations(Arrays.asList(
                LatLon.fromDegrees(40.1323, -122.0911),
                LatLon.fromDegrees(38.0062, -120.7711),
                LatLon.fromDegrees(37.0562, -119.6226),
                LatLon.fromDegrees(36.9231, -118.1829),
                LatLon.fromDegrees(37.8211, -118.8557),
                LatLon.fromDegrees(39.0906, -120.0304),
                LatLon.fromDegrees(40.2609, -120.8295)));
            poly.setAltitudes(10000.0, 100000.0);
            poly.setTerrainConforming(true, true);
            poly.setValue(DESCRIPTION, "Polygon over the Sierra Nevada mountains.");
            this.setupDefaultMaterial(poly, Color.LIGHT_GRAY);
            airspaces.add(poly);

            // PolyArc
            PolyArc polyArc = new PolyArc();
            polyArc.setLocations(Arrays.asList(
                LatLon.fromDegrees(45.5, -122.0),
                LatLon.fromDegrees(46.0, -122.0),
                LatLon.fromDegrees(46.0, -121.0),
                LatLon.fromDegrees(45.5, -121.0)));
            polyArc.setAltitudes(5000.0, 10000.0);
            polyArc.setRadius(30000.0);
            polyArc.setAzimuths(Angle.fromDegrees(-45.0), Angle.fromDegrees(135.0));
            polyArc.setTerrainConforming(true, true);
            this.setupDefaultMaterial(polyArc, Color.GRAY);
            airspaces.add(polyArc);

            // Route
            Route route = new Route();
            route.setAltitudes(5000.0, 20000.0);
            route.setWidth(20000.0);
            route.setLocations(Arrays.asList(
                LatLon.fromDegrees(43.0, -121.0),
                LatLon.fromDegrees(44.0, -121.0),
                LatLon.fromDegrees(44.0, -120.0),
                LatLon.fromDegrees(43.0, -120.0)));
            route.setTerrainConforming(false, true);
            this.setupDefaultMaterial(route, Color.GREEN);
            airspaces.add(route);

            // Track
            TrackAirspace track = new TrackAirspace();
            track.setEnableInnerCaps(false);
            double leftWwidth = 100000d;
            double rightWidth = 100000d;
            double minAlt = 150000d;
            double maxAlt = 250000d;
            Box leg;
            track.addLeg(LatLon.fromDegrees(40.4705, -117.9242), LatLon.fromDegrees(42.6139, -108.3518), minAlt, maxAlt, leftWwidth, rightWidth);
            leg = track.addLeg(LatLon.fromDegrees(42.6139, -108.3518), LatLon.fromDegrees(44.9305,  -97.6665), minAlt/2, maxAlt/2, leftWwidth, rightWidth);
            leg.setTerrainConforming( false, false );
            leg = track.addLeg(LatLon.fromDegrees(44.9305,  -97.6665), LatLon.fromDegrees(47.0121,  -94.9218), minAlt/2, maxAlt/2, leftWwidth, rightWidth);
            leg.setTerrainConforming( false, false );
            leg = track.addLeg(LatLon.fromDegrees(47.0121,  -94.9218), LatLon.fromDegrees(44.7964,  -68.4230), minAlt/4, maxAlt/4, leftWwidth, rightWidth);
            leg.setTerrainConforming( true, false );
            this.setupDefaultMaterial(track, Color.ORANGE);
            airspaces.add(track);

            track = new TrackAirspace();
            leftWwidth = 80000d;
            rightWidth = 80000d;
            minAlt = 150000d;
            maxAlt = 250000d;
            track.addLeg(LatLon.fromDegrees(29.9970, -108.6046), LatLon.fromDegrees(33.5132, -107.7544), minAlt/6, maxAlt/6, leftWwidth, rightWidth).setTerrainConforming(false, false);
            track.addLeg(LatLon.fromDegrees(29.4047, -103.0465), LatLon.fromDegrees(34.4955, -102.2151), minAlt/4, maxAlt/4, leftWwidth, rightWidth).setTerrainConforming(false, true);
            track.addLeg(LatLon.fromDegrees(28.9956, -99.8026), LatLon.fromDegrees(36.0133, -98.3489), minAlt/2, maxAlt/2, leftWwidth, rightWidth).setTerrainConforming(true, true);
            track.addLeg(LatLon.fromDegrees(28.5986, -96.6126), LatLon.fromDegrees(36.8515, -95.0324), minAlt, maxAlt, leftWwidth, rightWidth).setTerrainConforming(true, false);
            track.addLeg(LatLon.fromDegrees(30.4647, -94.1764), LatLon.fromDegrees(35.5636, -92.9371), minAlt/2, maxAlt/2, leftWwidth, rightWidth).setTerrainConforming(false, false);
            track.addLeg(LatLon.fromDegrees(31.0959, -90.9424), LatLon.fromDegrees(35.1470, -89.4267), minAlt/4, maxAlt/4, leftWwidth, rightWidth).setTerrainConforming(false, true);
            track.addLeg(LatLon.fromDegrees(31.5107, -88.5723), LatLon.fromDegrees(34.2444, -87.4563), minAlt/6, maxAlt/6, leftWwidth, rightWidth).setTerrainConforming(true, true);
            this.setupDefaultMaterial(track, Color.MAGENTA);
            airspaces.add(track);

            // Sphere
            SphereAirspace sphere = new SphereAirspace();
            sphere.setLocation(LatLon.fromDegrees(47.7477, -122.6372));
            sphere.setAltitude(5000.0);
            sphere.setTerrainConforming(true);
            sphere.setRadius(5000.0);
            this.setupDefaultMaterial(sphere, Color.ORANGE);
            airspaces.add(sphere);

            sphere = new SphereAirspace();
            sphere.setLocation(LatLon.fromDegrees(47.7477, -121.6372));
            sphere.setAltitude(0.0);
            sphere.setTerrainConforming(true);
            sphere.setRadius(5000.0);
            this.setupDefaultMaterial(sphere, Color.MAGENTA);
            airspaces.add(sphere);

            // Continent sized sphere
            sphere = new SphereAirspace();
            sphere.setLocation(LatLon.fromDegrees(0.0, -180.0));
            sphere.setAltitude(0.0);
            sphere.setTerrainConforming(false);
            sphere.setRadius(1000000.0);
            this.setupDefaultMaterial(sphere, Color.RED);
            airspaces.add(sphere);

            this.setAirspaces(airspaces);
        }

        public void doLoadIntersectingAirspaces()
        {
            ArrayList<Airspace> airspaces = new ArrayList<Airspace>();
            double minAltitude = 1000;
            double maxAltitude = 10000;

            // Cylinder.
            CappedCylinder cyl = new CappedCylinder();
            cyl.setCenter(LatLon.fromDegrees(47.7477, -123.6372));
            cyl.setRadius(30000.0);
            cyl.setAltitudes(minAltitude, maxAltitude);
            cyl.setTerrainConforming(false, false);
            cyl.setValue(DESCRIPTION, "30,000m Radius Cylinder. Top & bottom terrain conformance.");
            this.setupDefaultMaterial(cyl, Color.BLUE);
            airspaces.add(cyl);

            // Radarc
            PartialCappedCylinder partCyl = new PartialCappedCylinder();
            partCyl.setCenter(LatLon.fromDegrees(46.7477, -123.6372));
            partCyl.setAltitudes(minAltitude, maxAltitude);
            partCyl.setTerrainConforming(false, false);
            // To render a Radarc,
            // (1) Specify inner radius and outer radius.
            // (2) Specify start and stop azimuth.
            partCyl.setRadii(15000.0, 30000.0);
            partCyl.setAzimuths(Angle.fromDegrees(0.0), Angle.fromDegrees(90.0));
            partCyl.setValue(DESCRIPTION, "Partial Cylinder from 0 to 90 degrees.");
            this.setupDefaultMaterial(partCyl, Color.DARK_GRAY);
            airspaces.add(partCyl);

            // Radarc
            partCyl = new PartialCappedCylinder();
            partCyl.setCenter(LatLon.fromDegrees(46.7477, -122.6372));
            partCyl.setAltitudes(minAltitude, maxAltitude);
            partCyl.setTerrainConforming(false, false);
            // To render a Radarc,
            // (1) Specify inner radius and outer radius.
            // (2) Specify start and stop azimuth.
            partCyl.setRadii(15000.0, 30000.0);
            partCyl.setAzimuths(Angle.fromDegrees(90.0), Angle.fromDegrees(0.0));
            partCyl.setValue(DESCRIPTION, "Partial Cylinder from 90 to 0 degrees.");
            this.setupDefaultMaterial(partCyl, Color.GRAY);
            airspaces.add(partCyl);

            // Cake
            Cake cake = new Cake();
            cake.setLayers(Arrays.asList(
                new Cake.Layer(LatLon.fromDegrees(46.7477, -121.6372), 10000.0, Angle.fromDegrees(190.0),
                    Angle.fromDegrees(170.0), 10000.0, 15000.0),
                new Cake.Layer(LatLon.fromDegrees(46.7477, -121.6372), 15000.0, Angle.fromDegrees(190.0),
                    Angle.fromDegrees(90.0), 16000.0, 21000.0),
                new Cake.Layer(LatLon.fromDegrees(46.7477, -121.6372), 12500.0, Angle.fromDegrees(270.0),
                    Angle.fromDegrees(60.0), 22000.0, 27000.0)));
            cake.getLayers().get(0).setTerrainConforming(false, false);
            cake.getLayers().get(1).setTerrainConforming(false, false);
            cake.getLayers().get(2).setTerrainConforming(false, false);
            cake.setValue(DESCRIPTION, "3 layer Cake.");
            this.setupDefaultMaterial(cake, Color.YELLOW);
            airspaces.add(cake);

            cake = new Cake();
            cake.setLayers(Arrays.asList(
                new Cake.Layer(LatLon.fromDegrees(36, -121), 10000.0, Angle.fromDegrees(0.0),
                    Angle.fromDegrees(360.0), 10000.0, 15000.0),
                new Cake.Layer(LatLon.fromDegrees(36.1, -121.1), 15000.0, Angle.fromDegrees(0.0),
                    Angle.fromDegrees(360.0), 16000.0, 21000.0),
                new Cake.Layer(LatLon.fromDegrees(35.9, -120.9), 12500.0, Angle.fromDegrees(0.0),
                    Angle.fromDegrees(360.0), 22000.0, 27000.0)));
            cake.getLayers().get(0).setTerrainConforming(false, false);
            cake.getLayers().get(1).setTerrainConforming(false, false);
            cake.getLayers().get(2).setTerrainConforming(false, false);
            cake.setValue(DESCRIPTION, "3 layer Cake. With disjoint layers.");
            this.setupDefaultMaterial(cake, Color.MAGENTA);
            airspaces.add(cake);

            // Left Orbit
            Orbit orbit = new Orbit();
            orbit.setLocations(LatLon.fromDegrees(45.7477, -123.6372), LatLon.fromDegrees(45.7477, -122.6372));
            orbit.setAltitudes(minAltitude, maxAltitude);
            orbit.setWidth(30000.0);
            orbit.setOrbitType(Orbit.OrbitType.LEFT);
            orbit.setTerrainConforming(false, false);
            orbit.setValue(DESCRIPTION, "LEFT Orbit.");
            this.setupDefaultMaterial(orbit, Color.LIGHT_GRAY);
            airspaces.add(orbit);

            // Center Orbit
            orbit = new Orbit();
            orbit.setLocations(LatLon.fromDegrees(45.7477, -123.6372), LatLon.fromDegrees(45.7477, -122.6372));
            orbit.setAltitudes(minAltitude, maxAltitude);
            orbit.setWidth(30000.0);
            orbit.setOrbitType(Orbit.OrbitType.CENTER);
            orbit.setTerrainConforming(false, false);
            orbit.setValue(DESCRIPTION, "CENTER Orbit.");
            this.setupDefaultMaterial(orbit, Color.GRAY);
            airspaces.add(orbit);

            // Right Orbit
            orbit = new Orbit();
            orbit.setLocations(LatLon.fromDegrees(45.7477, -123.6372), LatLon.fromDegrees(45.7477, -122.6372));
            orbit.setAltitudes(minAltitude, maxAltitude);
            orbit.setWidth(30000.0);
            orbit.setOrbitType(Orbit.OrbitType.RIGHT);
            orbit.setTerrainConforming(false, false);
            orbit.setValue(DESCRIPTION, "RIGHT Orbit.");
            this.setupDefaultMaterial(orbit, Color.DARK_GRAY);
            airspaces.add(orbit);

            // Orbit from Los Angeles to New York
            orbit = new Orbit();
            orbit.setLocations(LatLon.fromDegrees(34.0489, -118.2481), LatLon.fromDegrees(40.7137, -74.0065));
            orbit.setAltitudes(minAltitude, maxAltitude);
            orbit.setWidth(500000.0);
            orbit.setOrbitType(Orbit.OrbitType.CENTER);
            orbit.setTerrainConforming(false, false);
            orbit.setValue(DESCRIPTION, "Orbit From L.A. to N.Y.");
            this.setupDefaultMaterial(orbit, Color.RED);
            airspaces.add(orbit);

            this.setAirspaces(airspaces);
        }

        public void doZoomToAirspaces()
        {
            BasicOrbitView view = (BasicOrbitView) this.wwd.getView();
            Position center = Position.fromDegrees(46.7477, -122.6372, 0.0);
            Angle heading = Angle.fromDegrees(0.0);
            Angle pitch = Angle.fromDegrees(30.0);
            double zoom = 600000.0;
            view.addPanToAnimator(center, heading, pitch, zoom, true);
        }

        public void doSaveAirspaces()
        {

            if (this.fileChooser == null)
            {
                this.fileChooser = new JFileChooser();
                this.fileChooser.setCurrentDirectory(new File(Configuration.getCurrentWorkingDirectory()));
            }

            this.fileChooser.setDialogTitle("Choose Directory to Place Airspaces");
            this.fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            this.fileChooser.setMultiSelectionEnabled(false);
            int status = this.fileChooser.showSaveDialog(null);
            if (status != JFileChooser.APPROVE_OPTION)
                return;

            final File dir = this.fileChooser.getSelectedFile();
            if (dir == null)
                return;

            if (!dir.exists())
            {
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            }

            AirspaceLayer layer = (AirspaceLayer) wwd.getModel().getLayers().getLayerByName(AIRSPACE_LAYER_NAME);
            final Iterable<Airspace> airspaces = layer.getAirspaces();

            Thread t = new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        java.text.DecimalFormat f = new java.text.DecimalFormat("####");
                        f.setMinimumIntegerDigits(4);
                        int counter = 0;

                        for (Airspace a : airspaces)
                        {
                            String xmlString = a.getRestorableState();
                            if (xmlString == null)
                                continue;

                            try
                            {
                                PrintWriter of = new PrintWriter(
                                    new File(dir, a.getClass().getName() + "-" + f.format(counter++) + ".xml"));
                                of.write(xmlString);
                                of.flush();
                                of.close();
                            }
                            catch (FileNotFoundException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                    finally
                    {
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            public void run()
                            {
                                frame.setCursor(Cursor.getDefaultCursor());
                                saveButton.setEnabled(true);
                            }
                        });
                    }
                }
            });
            saveButton.setEnabled(false);
            frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            t.start();

        }

        public void doReadAirspaces()
        {
            if (this.fileChooser == null)
            {
                this.fileChooser = new JFileChooser();
                this.fileChooser.setCurrentDirectory(new File(Configuration.getCurrentWorkingDirectory()));
            }

            this.fileChooser.setDialogTitle("Choose Airspace File Directory");
            this.fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            this.fileChooser.setMultiSelectionEnabled(false);
            int status = this.fileChooser.showOpenDialog(null);
            if (status != JFileChooser.APPROVE_OPTION)
                return;

            final File dir = this.fileChooser.getSelectedFile();
            if (dir == null)
                return;

            Thread t = new Thread(new Runnable()
            {
                public void run()
                {
                    final ArrayList<Airspace> airspaces = new ArrayList<Airspace>();
                    try
                    {
                        File[] files = dir.listFiles(new FilenameFilter()
                        {
                            public boolean accept(File dir, String name)
                            {
                                return name.startsWith("gov.nasa.worldwind.render.airspaces") && name.endsWith(".xml");
                            }
                        });

                        for (File file : files)
                        {
                            String[] name = file.getName().split("-");
                            try
                            {
                                Class c = Class.forName(name[0]);
                                Airspace airspace = (Airspace) c.newInstance();
                                BufferedReader input = new BufferedReader(new FileReader(file));
                                String s = input.readLine();
                                airspace.restoreState(s);
                                airspaces.add(airspace);

                                AirspaceAttributes attribs = airspace.getAttributes();
                                if (!attribs.isDrawOutline())
                                {
                                    Color color = attribs.getMaterial().getDiffuse();
                                    attribs.setDrawOutline(true);
                                    attribs.setOutlineMaterial(new Material(makeBrighter(color)));
                                }
                            }
                            catch (ClassNotFoundException e)
                            {
                                e.printStackTrace();
                            }
                            catch (IllegalAccessException e)
                            {
                                e.printStackTrace();
                            }
                            catch (InstantiationException e)
                            {
                                e.printStackTrace();
                            }
                            catch (FileNotFoundException e)
                            {
                                e.printStackTrace();
                            }
                            catch (IOException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                    finally
                    {
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            public void run()
                            {
                                AirspacesController.this.setAirspaces(airspaces);
                                wwd.redraw();
                                frame.setCursor(Cursor.getDefaultCursor());
                                readButton.setEnabled(true);
                            }
                        });
                    }
                }
            });
            readButton.setEnabled(false);
            frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            t.start();
        }

        public void doSetVerticalExaggeration(double ve)
        {
            this.wwd.getSceneController().setVerticalExaggeration(ve);
        }
    }

    public static void main(String[] args)
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e)
        {
            String message = "ExceptionWhileSettingSystemLookAndFeel";
            Logging.logger().log(java.util.logging.Level.WARNING, message, e);
        }

        //Configuration.setValue(AVKey.GLOBE_CLASS_NAME, EarthFlat.class.getName());
        //Configuration.setValue(AVKey.VIEW_CLASS_NAME, FlatOrbitView.class.getName());
        start("World Wind Airspaces", AppFrame.class);
    }

    private static Iterable<LatLon> makeLatLon(double[] src, int offset, int length)
    {
        int numCoords = (int) Math.floor(length / 2.0);
        LatLon[] dest = new LatLon[numCoords];
        for (int i = 0; i < numCoords; i++)
        {
            double lonDegrees = src[offset + 2 * i];
            double latDegrees = src[offset + 2 * i + 1];
            dest[i] = LatLon.fromDegrees(latDegrees, lonDegrees);
        }
        return Arrays.asList(dest);
    }

    private static Iterable<LatLon> makeLatLon(double[] src)
    {
        return makeLatLon(src, 0, src.length);
    }

    // Boundary data for San Juan and Snohomish Counties take from
    // http://www.census.gov/geo/cob/bdy/co/co00ascii/co53_d00_ascii.zip

    private static final double[] SAN_JUAN_COUNTY_1 =
        {
            //-0.123026351250109E+03,       0.487022510493827E+02,
            -0.123025486000000E+03, 0.487179660000000E+02,
            -0.123019699000000E+03, 0.487213120000000E+02,
            -0.123009787000000E+03, 0.487222910000000E+02,
            -0.123007511000000E+03, 0.487188630000000E+02,
            -0.123005086000000E+03, 0.486943420000000E+02,
            -0.123014449000000E+03, 0.486849780000000E+02,
            -0.123021215000000E+03, 0.486814160000000E+02,
            -0.123042337000000E+03, 0.486756630000000E+02,
            -0.123041645000000E+03, 0.486786330000000E+02,
            -0.123035672000000E+03, 0.486853500000000E+02,
            -0.123036360000000E+03, 0.486900800000000E+02,
            -0.123047058000000E+03, 0.486957720000000E+02,
            -0.123070427000000E+03, 0.486999710000000E+02,
            -0.123040179000000E+03, 0.487172960000000E+02,
            -0.123025486000000E+03, 0.487179660000000E+02,
        };
    private static final double[] SAN_JUAN_COUNTY_2 =
        {
            //-0.122948050323932E+03,       0.485739411234568E+02,
            -0.122906298643435E+03, 0.487142756789313E+02,
            -0.122894599000000E+03, 0.487150300000000E+02,
            -0.122883606017278E+03, 0.487133157521183E+02,
            -0.122879724954535E+03, 0.487127105384329E+02,
            -0.122875938000000E+03, 0.487121200000000E+02,
            -0.122836802504198E+03, 0.486993713019117E+02,
            -0.122834290842953E+03, 0.486985531083446E+02,
            -0.122833124000000E+03, 0.486981730000000E+02,
            -0.122831410808922E+03, 0.486973051154063E+02,
            -0.122825683629670E+03, 0.486944037865925E+02,
            -0.122808804479515E+03, 0.486858529865323E+02,
            -0.122805788100163E+03, 0.486843249204562E+02,
            -0.122802545000000E+03, 0.486826820000000E+02,
            -0.122800267000000E+03, 0.486796200000000E+02,
            -0.122761393485795E+03, 0.486676429790988E+02,
            -0.122744791626046E+03, 0.486625279071718E+02,
            -0.122743049000000E+03, 0.486619910000000E+02,
            -0.122742776245752E+03, 0.486616237548803E+02,
            -0.122742082000000E+03, 0.486606890000000E+02,
            -0.122755031000000E+03, 0.486495120000000E+02,
            -0.122762709974974E+03, 0.486457601003223E+02,
            -0.122774165127982E+03, 0.486401631834124E+02,
            -0.122783875000000E+03, 0.486354190000000E+02,
            -0.122792147000000E+03, 0.486335020000000E+02,
            -0.122806316669653E+03, 0.486217713784910E+02,
            -0.122807583040252E+03, 0.486207229903106E+02,
            -0.122809622000000E+03, 0.486190350000000E+02,
            -0.122809583303260E+03, 0.486188448346238E+02,
            -0.122808864000000E+03, 0.486153100000000E+02,
            -0.122799010000000E+03, 0.486046830000000E+02,
            -0.122798770000000E+03, 0.486023520000000E+02,
            -0.122800217000000E+03, 0.486016900000000E+02,
            -0.122801521572817E+03, 0.486000752686420E+02,
            -0.122804869000000E+03, 0.485959320000000E+02,
            -0.122804736685678E+03, 0.485955635328447E+02,
            -0.122802617338743E+03, 0.485896616038081E+02,
            -0.122801096000000E+03, 0.485854250000000E+02,
            -0.122796913105296E+03, 0.485828999845133E+02,
            -0.122787753320193E+03, 0.485773706559316E+02,
            -0.122786586000000E+03, 0.485766660000000E+02,
            -0.122771206000000E+03, 0.485624260000000E+02,
            -0.122770349000000E+03, 0.485581060000000E+02,
            -0.122772384000000E+03, 0.485521430000000E+02,
            -0.122782618000000E+03, 0.485451910000000E+02,
            -0.122788503000000E+03, 0.485303930000000E+02,
            -0.122787596684858E+03, 0.485246062248558E+02,
            -0.122787347000000E+03, 0.485230120000000E+02,
            -0.122777467000000E+03, 0.485177990000000E+02,
            -0.122779073275586E+03, 0.485091830812273E+02,
            -0.122779124000000E+03, 0.485089110000000E+02,
            -0.122799155517055E+03, 0.484953208059023E+02,
            -0.122799759583816E+03, 0.484949109824973E+02,
            -0.122800414000000E+03, 0.484944670000000E+02,
            -0.122809344417182E+03, 0.484907496395120E+02,
            -0.122816332000000E+03, 0.484878410000000E+02,
            -0.122816392492828E+03, 0.484876896530700E+02,
            -0.122817912000000E+03, 0.484838880000000E+02,
            -0.122818113811431E+03, 0.484811078199731E+02,
            -0.122818435504082E+03, 0.484766761409594E+02,
            -0.122818482000648E+03, 0.484760355983364E+02,
            -0.122819730000000E+03, 0.484588430000000E+02,
            -0.122813100000000E+03, 0.484528560000000E+02,
            -0.122810699656295E+03, 0.484489394154454E+02,
            -0.122807708000000E+03, 0.484440580000000E+02,
            -0.122806716195520E+03, 0.484419671792455E+02,
            -0.122802509000000E+03, 0.484330980000000E+02,
            -0.122803004619265E+03, 0.484309676207466E+02,
            -0.122803326834080E+03, 0.484295826064757E+02,
            -0.122803521000000E+03, 0.484287480000000E+02,
            -0.122806510124273E+03, 0.484265382433425E+02,
            -0.122812173045928E+03, 0.484223518403418E+02,
            -0.122812208000000E+03, 0.484223260000000E+02,
            -0.122825307995692E+03, 0.484240652785748E+02,
            -0.122825803000000E+03, 0.484241310000000E+02,
            -0.122852542484940E+03, 0.484208474850799E+02,
            -0.122863360429977E+03, 0.484195190793902E+02,
            -0.122866475017092E+03, 0.484191366190217E+02,
            -0.122866865338919E+03, 0.484190886888710E+02,
            -0.122867939205531E+03, 0.484189568218194E+02,
            -0.122867996814560E+03, 0.484189497476327E+02,
            -0.122874135000000E+03, 0.484181960000000E+02,
            -0.122874527604161E+03, 0.484182203541858E+02,
            -0.122883759000000E+03, 0.484187930000000E+02,
            -0.122888474856222E+03, 0.484206350791676E+02,
            -0.122893646000000E+03, 0.484226550000000E+02,
            -0.122889016000000E+03, 0.484359470000000E+02,
            -0.122903214000000E+03, 0.484369790000000E+02,
            -0.122913888000000E+03, 0.484432310000000E+02,
            -0.122917771000000E+03, 0.484397810000000E+02,
            -0.122927683717092E+03, 0.484399602096806E+02,
            -0.122928004000000E+03, 0.484399660000000E+02,
            -0.122927964788402E+03, 0.484400111660268E+02,
            -0.122916460000000E+03, 0.484532630000000E+02,
            -0.122920099000000E+03, 0.484584280000000E+02,
            -0.122926901000000E+03, 0.484608740000000E+02,
            -0.122937881000000E+03, 0.484562210000000E+02,
            -0.122950555015899E+03, 0.484535630706048E+02,
            -0.122962009000000E+03, 0.484511610000000E+02,
            -0.123001288000000E+03, 0.484556628590224E+02,
            -0.123026267878952E+03, 0.484585258619868E+02,
            -0.123029013724265E+03, 0.484588405698076E+02,
            -0.123033178635901E+03, 0.484593179201736E+02,
            -0.123038888830654E+03, 0.484599723790898E+02,
            -0.123039156000000E+03, 0.484600030000000E+02,
            -0.123058154000000E+03, 0.484715220000000E+02,
            -0.123067675000000E+03, 0.484794970000000E+02,
            -0.123070563309358E+03, 0.484802266082759E+02,
            -0.123070833040893E+03, 0.484802947444538E+02,
            -0.123076030355349E+03, 0.484816076244710E+02,
            -0.123099948917862E+03, 0.484876496300162E+02,
            -0.123119451000000E+03, 0.484925760000000E+02,
            -0.123129287766067E+03, 0.484982542349179E+02,
            -0.123134915117353E+03, 0.485015026015408E+02,
            -0.123141478000000E+03, 0.485052910000000E+02,
            -0.123144822701730E+03, 0.485083136865329E+02,
            -0.123148640952156E+03, 0.485117643302889E+02,
            -0.123150537321568E+03, 0.485134781244465E+02,
            -0.123151065000000E+03, 0.485139550000000E+02,
            -0.123152147442285E+03, 0.485153416540207E+02,
            -0.123152738173008E+03, 0.485160984048828E+02,
            -0.123163234000000E+03, 0.485295440000000E+02,
            -0.123164057000000E+03, 0.485356220000000E+02,
            -0.123161853000000E+03, 0.485392550000000E+02,
            -0.123161750942688E+03, 0.485414834733667E+02,
            -0.123161470000000E+03, 0.485476180000000E+02,
            -0.123167656573858E+03, 0.485526319405021E+02,
            -0.123172412000000E+03, 0.485564860000000E+02,
            -0.123176266000000E+03, 0.485621310000000E+02,
            -0.123176243708612E+03, 0.485624730166654E+02,
            -0.123175852000000E+03, 0.485684830000000E+02,
            -0.123171958000000E+03, 0.485722550000000E+02,
            -0.123173061000000E+03, 0.485790860000000E+02,
            -0.123184941000000E+03, 0.485869700000000E+02,
            -0.123195467450664E+03, 0.485863505553890E+02,
            -0.123196697396232E+03, 0.485862781774168E+02,
            -0.123197754000000E+03, 0.485862160000000E+02,
            -0.123198045125962E+03, 0.485864522812820E+02,
            -0.123202680000000E+03, 0.485902140000000E+02,
            -0.123203026000000E+03, 0.485961780000000E+02,
            -0.123195725000000E+03, 0.486070550000000E+02,
            -0.123179996129946E+03, 0.486207472995959E+02,
            -0.123178425000000E+03, 0.486221150000000E+02,
            -0.123151643000000E+03, 0.486236860000000E+02,
            -0.123139705244805E+03, 0.486227863453916E+02,
            -0.123135644582323E+03, 0.486201710055677E+02,
            -0.123131377372596E+03, 0.486205150057834E+02,
            -0.123107362000000E+03, 0.486224510000000E+02,
            -0.123101647918870E+03, 0.486162765822218E+02,
            -0.123098626049648E+03, 0.486130112657825E+02,
            -0.123098462000000E+03, 0.486128340000000E+02,
            -0.123098254000000E+03, 0.486100920000000E+02,
            -0.123102074000000E+03, 0.486040350000000E+02,
            -0.123101552000000E+03, 0.485978200000000E+02,
            -0.123100979011718E+03, 0.485976923053470E+02,
            -0.123087991970190E+03, 0.485947980476233E+02,
            -0.123079379694944E+03, 0.485928787387418E+02,
            -0.123079368517922E+03, 0.485928762478604E+02,
            -0.123079334903513E+03, 0.485928687566419E+02,
            -0.123078354245955E+03, 0.485926502098924E+02,
            -0.123078029433353E+03, 0.485925778230151E+02,
            -0.123077161967252E+03, 0.485923845018144E+02,
            -0.123074727135920E+03, 0.485918418817440E+02,
            -0.123074611000000E+03, 0.485918160000000E+02,
            -0.123060040000000E+03, 0.485821050000000E+02,
            -0.123056818600556E+03, 0.485785370170628E+02,
            -0.123055637295811E+03, 0.485772286182611E+02,
            -0.123049834129163E+03, 0.485708011012966E+02,
            -0.123048403000000E+03, 0.485692160000000E+02,
            -0.123037067007025E+03, 0.485647482308128E+02,
            -0.123033669000000E+03, 0.485634090000000E+02,
            -0.123025587194622E+03, 0.485622858881320E+02,
            -0.123015046000000E+03, 0.485608210000000E+02,
            -0.123014957175286E+03, 0.485608244377565E+02,
            -0.123005887152572E+03, 0.485611754721491E+02,
            -0.122994982694655E+03, 0.485615975041420E+02,
            -0.122987296000000E+03, 0.485618950000000E+02,
            -0.122986110000000E+03, 0.485699840000000E+02,
            -0.122989649000000E+03, 0.485746680000000E+02,
            -0.122995026000000E+03, 0.485781620000000E+02,
            -0.123001291973563E+03, 0.485798454915671E+02,
            -0.123003071284953E+03, 0.485803235426935E+02,
            -0.123004800000000E+03, 0.485807880000000E+02,
            -0.123016647000000E+03, 0.485802440000000E+02,
            -0.123023206615953E+03, 0.485845746092944E+02,
            -0.123028982347697E+03, 0.485883877041088E+02,
            -0.123033093576815E+03, 0.485911019067628E+02,
            -0.123034101000000E+03, 0.485917670000000E+02,
            -0.123024902000000E+03, 0.485944840000000E+02,
            -0.123023433000000E+03, 0.485994770000000E+02,
            -0.123041189000000E+03, 0.486119470000000E+02,
            -0.123046453150843E+03, 0.486114965755598E+02,
            -0.123046530000000E+03, 0.486114900000000E+02,
            -0.123048652000000E+03, 0.486210020000000E+02,
            -0.123040078194353E+03, 0.486254322142389E+02,
            -0.123023495000000E+03, 0.486340010000000E+02,
            -0.123015592000000E+03, 0.486425670000000E+02,
            -0.123014829000000E+03, 0.486475030000000E+02,
            -0.123009977399319E+03, 0.486549816855750E+02,
            -0.123009924000000E+03, 0.486550640000000E+02,
            -0.123001295006939E+03, 0.486600601213626E+02,
            -0.122999889835340E+03, 0.486608737050326E+02,
            -0.122988884000000E+03, 0.486672460000000E+02,
            -0.122984853000000E+03, 0.486726860000000E+02,
            -0.122970595802002E+03, 0.486809490071059E+02,
            -0.122949116000000E+03, 0.486933980000000E+02,
            -0.122941316000000E+03, 0.487029040000000E+02,
            -0.122942367000000E+03, 0.487067230000000E+02,
            -0.122927109877131E+03, 0.487110138483226E+02,
            -0.122918837934242E+03, 0.487133402143466E+02,
            -0.122918252000000E+03, 0.487135050000000E+02,
            -0.122908658361456E+03, 0.487141235388230E+02,
            -0.122907454714043E+03, 0.487142011426071E+02,
            -0.122906298643435E+03, 0.487142756789313E+02,
        };
    private static final double[] SAN_JUAN_COUNTY_3 =
        {
            //-0.123174473648561E+03,       0.486648342044502E+02,
            -0.123172066000000E+03, 0.486798660000000E+02,
            -0.123147990000000E+03, 0.486680010000000E+02,
            -0.123133285875321E+03, 0.486583191438863E+02,
            -0.123130962000000E+03, 0.486567890000000E+02,
            -0.123122016049879E+03, 0.486470646274710E+02,
            -0.123106165000000E+03, 0.486334730000000E+02,
            -0.123119677246557E+03, 0.486329724058636E+02,
            -0.123134956336931E+03, 0.486372395392956E+02,
            -0.123170932260699E+03, 0.486515091289271E+02,
            -0.123197404105632E+03, 0.486620089945159E+02,
            -0.123215917000000E+03, 0.486693520000000E+02,
            -0.123229744313989E+03, 0.486785441581480E+02,
            -0.123233070031572E+03, 0.486807550374269E+02,
            -0.123237148000000E+03, 0.486834660000000E+02,
            -0.123237135383772E+03, 0.486835850832961E+02,
            -0.123236567000000E+03, 0.486889500000000E+02,
            -0.123213499856684E+03, 0.486896934099409E+02,
            -0.123212892000000E+03, 0.486897130000000E+02,
            -0.123212464368268E+03, 0.486895683569085E+02,
            -0.123202992056872E+03, 0.486863644216061E+02,
            -0.123197953000000E+03, 0.486846600000000E+02,
            -0.123186997647353E+03, 0.486848970569698E+02,
            -0.123186076000000E+03, 0.486849170000000E+02,
            -0.123184143390981E+03, 0.486842202399606E+02,
            -0.123183803136584E+03, 0.486840975686572E+02,
            -0.123172066000000E+03, 0.486798660000000E+02,
        };
    private static final double[] SNOHOMISH_COUNTY =
        {
            //-0.121733244893996E+03,       0.482266785925926E+02,
            -0.121835846000000E+03, 0.482980130000000E+02,
            -0.121706923000000E+03, 0.482980520000000E+02,
            -0.121684141000000E+03, 0.482985310000000E+02,
            -0.121636515000000E+03, 0.482985310000000E+02,
            -0.121577636000000E+03, 0.482992320000000E+02,
            -0.121577236000000E+03, 0.482974320000000E+02,
            -0.121568862000000E+03, 0.482972670000000E+02,
            -0.121560180000000E+03, 0.482970960000000E+02,
            -0.121534083000000E+03, 0.482963750000000E+02,
            -0.121534083000000E+03, 0.482963750000000E+02,
            -0.121526720000000E+03, 0.482962360000000E+02,
            -0.121526720000000E+03, 0.482962360000000E+02,
            -0.121428086000000E+03, 0.482959340000000E+02,
            -0.121414025000000E+03, 0.482959340000000E+02,
            -0.121414025000000E+03, 0.482959340000000E+02,
            -0.121413600000000E+03, 0.482959340000000E+02,
            -0.121413600000000E+03, 0.482959340000000E+02,
            -0.121126218000000E+03, 0.482960420000000E+02,
            -0.121014631000000E+03, 0.482956020000000E+02,
            -0.121014631000000E+03, 0.482956020000000E+02,
            -0.121001414000000E+03, 0.482955480000000E+02,
            -0.121001414000000E+03, 0.482955480000000E+02,
            -0.121000315000000E+03, 0.482947480000000E+02,
            -0.121000315000000E+03, 0.482941480000000E+02,
            -0.121001215000000E+03, 0.482895470000000E+02,
            -0.121002615000000E+03, 0.482859470000000E+02,
            -0.121009915000000E+03, 0.482845470000000E+02,
            -0.121011315000000E+03, 0.482830470000000E+02,
            -0.121011615000000E+03, 0.482804470000000E+02,
            -0.121010115000000E+03, 0.482781470000000E+02,
            -0.121004314000000E+03, 0.482729470000000E+02,
            -0.121004614000000E+03, 0.482699470000000E+02,
            -0.121003914000000E+03, 0.482672470000000E+02,
            -0.120999864000000E+03, 0.482624520000000E+02,
            -0.120991213000000E+03, 0.482558470000000E+02,
            -0.120986913000000E+03, 0.482543480000000E+02,
            -0.120982613000000E+03, 0.482515480000000E+02,
            -0.120980813000000E+03, 0.482495480000000E+02,
            -0.120980613000000E+03, 0.482470480000000E+02,
            -0.120980113000000E+03, 0.482461480000000E+02,
            -0.120972213000000E+03, 0.482351480000000E+02,
            -0.120964613000000E+03, 0.482306480000000E+02,
            -0.120962013000000E+03, 0.482301480000000E+02,
            -0.120959613000000E+03, 0.482282480000000E+02,
            -0.120959313000000E+03, 0.482270480000000E+02,
            -0.120962613000000E+03, 0.482208470000000E+02,
            -0.120964613000000E+03, 0.482145470000000E+02,
            -0.120963912000000E+03, 0.482110470000000E+02,
            -0.120963184000000E+03, 0.482107330000000E+02,
            -0.120963184000000E+03, 0.482107330000000E+02,
            -0.120951312000000E+03, 0.482091470000000E+02,
            -0.120946812000000E+03, 0.482014470000000E+02,
            -0.120940338000000E+03, 0.481999570000000E+02,
            -0.120932512000000E+03, 0.481972480000000E+02,
            -0.120923112000000E+03, 0.481913480000000E+02,
            -0.120919512000000E+03, 0.481881480000000E+02,
            -0.120916112000000E+03, 0.481796480000000E+02,
            -0.120911411000000E+03, 0.481760480000000E+02,
            -0.120908811000000E+03, 0.481728480000000E+02,
            -0.120906211000000E+03, 0.481638480000000E+02,
            -0.120906211000000E+03, 0.481638480000000E+02,
            -0.120915811000000E+03, 0.481594470000000E+02,
            -0.120923511000000E+03, 0.481606470000000E+02,
            -0.120927712000000E+03, 0.481618470000000E+02,
            -0.120930812000000E+03, 0.481616470000000E+02,
            -0.120933112000000E+03, 0.481592460000000E+02,
            -0.120932512000000E+03, 0.481571460000000E+02,
            -0.120932812000000E+03, 0.481569460000000E+02,
            -0.120939112000000E+03, 0.481551460000000E+02,
            -0.120941712000000E+03, 0.481555460000000E+02,
            -0.120947212000000E+03, 0.481551460000000E+02,
            -0.120954512000000E+03, 0.481525460000000E+02,
            -0.120955012000000E+03, 0.481518450000000E+02,
            -0.120955112000000E+03, 0.481498450000000E+02,
            -0.120959312000000E+03, 0.481457450000000E+02,
            -0.120962412000000E+03, 0.481438450000000E+02,
            -0.120964612000000E+03, 0.481411450000000E+02,
            -0.120964112000000E+03, 0.481333450000000E+02,
            -0.120961612000000E+03, 0.481309450000000E+02,
            -0.120958112000000E+03, 0.481292450000000E+02,
            -0.120956112000000E+03, 0.481263450000000E+02,
            -0.120956377000000E+03, 0.481245810000000E+02,
            -0.120956377000000E+03, 0.481245810000000E+02,
            -0.120956712000000E+03, 0.481236450000000E+02,
            -0.120955611000000E+03, 0.481211450000000E+02,
            -0.120951411000000E+03, 0.481165450000000E+02,
            -0.120948511000000E+03, 0.481155450000000E+02,
            -0.120945973000000E+03, 0.481153350000000E+02,
            -0.120945973000000E+03, 0.481153350000000E+02,
            -0.120946411000000E+03, 0.481134450000000E+02,
            -0.120947611000000E+03, 0.481121440000000E+02,
            -0.120950611000000E+03, 0.481116440000000E+02,
            -0.120952911000000E+03, 0.481105440000000E+02,
            -0.120952911000000E+03, 0.481105440000000E+02,
            -0.120954211000000E+03, 0.481089440000000E+02,
            -0.120956611000000E+03, 0.481081440000000E+02,
            -0.120961976000000E+03, 0.481093480000000E+02,
            -0.120962911000000E+03, 0.481097440000000E+02,
            -0.120971012000000E+03, 0.481092440000000E+02,
            -0.120971812000000E+03, 0.481083440000000E+02,
            -0.120970812000000E+03, 0.481067440000000E+02,
            -0.120971811000000E+03, 0.481035430000000E+02,
            -0.120974911000000E+03, 0.481019430000000E+02,
            -0.120979712000000E+03, 0.480970430000000E+02,
            -0.120980011000000E+03, 0.480954430000000E+02,
            -0.120980911000000E+03, 0.480944430000000E+02,
            -0.120984212000000E+03, 0.480932430000000E+02,
            -0.120985812000000E+03, 0.480937430000000E+02,
            -0.120998412000000E+03, 0.480885420000000E+02,
            -0.121004012000000E+03, 0.480812420000000E+02,
            -0.121015012000000E+03, 0.480752410000000E+02,
            -0.121022612000000E+03, 0.480782410000000E+02,
            -0.121031412000000E+03, 0.480782400000000E+02,
            -0.121035612000000E+03, 0.480765400000000E+02,
            -0.121036412000000E+03, 0.480753400000000E+02,
            -0.121040012000000E+03, 0.480731400000000E+02,
            -0.121047812000000E+03, 0.480704390000000E+02,
            -0.121052112000000E+03, 0.480702390000000E+02,
            -0.121052112000000E+03, 0.480702390000000E+02,
            -0.121062436000000E+03, 0.480702140000000E+02,
            -0.121062436000000E+03, 0.480702140000000E+02,
            -0.121071913000000E+03, 0.480673380000000E+02,
            -0.121080613000000E+03, 0.480618380000000E+02,
            -0.121082313000000E+03, 0.480618380000000E+02,
            -0.121089213000000E+03, 0.480648380000000E+02,
            -0.121094513000000E+03, 0.480646370000000E+02,
            -0.121099513000000E+03, 0.480631370000000E+02,
            -0.121100513000000E+03, 0.480626370000000E+02,
            -0.121101713000000E+03, 0.480604370000000E+02,
            -0.121103813000000E+03, 0.480581370000000E+02,
            -0.121107913000000E+03, 0.480567370000000E+02,
            -0.121114413000000E+03, 0.480577360000000E+02,
            -0.121116913000000E+03, 0.480571360000000E+02,
            -0.121120013000000E+03, 0.480516360000000E+02,
            -0.121122913000000E+03, 0.480499360000000E+02,
            -0.121128626000000E+03, 0.480479250000000E+02,
            -0.121131500000000E+03, 0.480439860000000E+02,
            -0.121138507000000E+03, 0.480438970000000E+02,
            -0.121153476000000E+03, 0.480407030000000E+02,
            -0.121153248000000E+03, 0.480385970000000E+02,
            -0.121149034000000E+03, 0.480338950000000E+02,
            -0.121149001000000E+03, 0.480303410000000E+02,
            -0.121141174000000E+03, 0.480254900000000E+02,
            -0.121130671000000E+03, 0.480203750000000E+02,
            -0.121129399000000E+03, 0.480184260000000E+02,
            -0.121128654000000E+03, 0.480152360000000E+02,
            -0.121125737000000E+03, 0.480104310000000E+02,
            -0.121123536000000E+03, 0.480083950000000E+02,
            -0.121118123000000E+03, 0.480064710000000E+02,
            -0.121117212000000E+03, 0.479998350000000E+02,
            -0.121118112000000E+03, 0.479976350000000E+02,
            -0.121119612000000E+03, 0.479967350000000E+02,
            -0.121121213000000E+03, 0.479965350000000E+02,
            -0.121124313000000E+03, 0.479943350000000E+02,
            -0.121130413000000E+03, 0.479870340000000E+02,
            -0.121134260000000E+03, 0.479855460000000E+02,
            -0.121134260000000E+03, 0.479855460000000E+02,
            -0.121139988000000E+03, 0.479833340000000E+02,
            -0.121147013000000E+03, 0.479814340000000E+02,
            -0.121148382000000E+03, 0.479785260000000E+02,
            -0.121148382000000E+03, 0.479785260000000E+02,
            -0.121148715000000E+03, 0.479777300000000E+02,
            -0.121148613000000E+03, 0.479701330000000E+02,
            -0.121155413000000E+03, 0.479618330000000E+02,
            -0.121158813000000E+03, 0.479592330000000E+02,
            -0.121163213000000E+03, 0.479579330000000E+02,
            -0.121164613000000E+03, 0.479562330000000E+02,
            -0.121164713000000E+03, 0.479536330000000E+02,
            -0.121163313000000E+03, 0.479518330000000E+02,
            -0.121161413000000E+03, 0.479476330000000E+02,
            -0.121161913000000E+03, 0.479447330000000E+02,
            -0.121166313000000E+03, 0.479381330000000E+02,
            -0.121170213000000E+03, 0.479370330000000E+02,
            -0.121170913000000E+03, 0.479364330000000E+02,
            -0.121172713000000E+03, 0.479337320000000E+02,
            -0.121172813000000E+03, 0.479315320000000E+02,
            -0.121172813000000E+03, 0.479315320000000E+02,
            -0.121170813000000E+03, 0.479249320000000E+02,
            -0.121178913000000E+03, 0.479195320000000E+02,
            -0.121179513000000E+03, 0.479188320000000E+02,
            -0.121179413000000E+03, 0.479175320000000E+02,
            -0.121173013000000E+03, 0.479149320000000E+02,
            -0.121171913000000E+03, 0.479140320000000E+02,
            -0.121171213000000E+03, 0.479126320000000E+02,
            -0.121172813000000E+03, 0.479087320000000E+02,
            -0.121175613000000E+03, 0.479066320000000E+02,
            -0.121180113000000E+03, 0.478992320000000E+02,
            -0.121178113000000E+03, 0.478958320000000E+02,
            -0.121176813000000E+03, 0.478950320000000E+02,
            -0.121175113000000E+03, 0.478857320000000E+02,
            -0.121166012000000E+03, 0.478792320000000E+02,
            -0.121163112000000E+03, 0.478792320000000E+02,
            -0.121160112000000E+03, 0.478807320000000E+02,
            -0.121157812000000E+03, 0.478788330000000E+02,
            -0.121157912000000E+03, 0.478766330000000E+02,
            -0.121157512000000E+03, 0.478757330000000E+02,
            -0.121155007000000E+03, 0.478718290000000E+02,
            -0.121152512000000E+03, 0.478699330000000E+02,
            -0.121151712000000E+03, 0.478679330000000E+02,
            -0.121151649000000E+03, 0.478662060000000E+02,
            -0.121151649000000E+03, 0.478662060000000E+02,
            -0.121150012000000E+03, 0.478618330000000E+02,
            -0.121147612000000E+03, 0.478594330000000E+02,
            -0.121147012000000E+03, 0.478569330000000E+02,
            -0.121150712000000E+03, 0.478545330000000E+02,
            -0.121152612000000E+03, 0.478521320000000E+02,
            -0.121153712000000E+03, 0.478458320000000E+02,
            -0.121149212000000E+03, 0.478438320000000E+02,
            -0.121138612000000E+03, 0.478429330000000E+02,
            -0.121133212000000E+03, 0.478403330000000E+02,
            -0.121126611000000E+03, 0.478392330000000E+02,
            -0.121123211000000E+03, 0.478381330000000E+02,
            -0.121121311000000E+03, 0.478370330000000E+02,
            -0.121121211000000E+03, 0.478331330000000E+02,
            -0.121120511000000E+03, 0.478317330000000E+02,
            -0.121119111000000E+03, 0.478310330000000E+02,
            -0.121119111000000E+03, 0.478310330000000E+02,
            -0.121107411000000E+03, 0.478288330000000E+02,
            -0.121091611000000E+03, 0.478324340000000E+02,
            -0.121078011000000E+03, 0.478343340000000E+02,
            -0.121076611000000E+03, 0.478320340000000E+02,
            -0.121075211000000E+03, 0.478311340000000E+02,
            -0.121070111000000E+03, 0.478301340000000E+02,
            -0.121070710000000E+03, 0.478260340000000E+02,
            -0.121075011000000E+03, 0.478216340000000E+02,
            -0.121078706000000E+03, 0.478187090000000E+02,
            -0.121078706000000E+03, 0.478187090000000E+02,
            -0.121081711000000E+03, 0.478155340000000E+02,
            -0.121082111000000E+03, 0.478126340000000E+02,
            -0.121086345000000E+03, 0.478073260000000E+02,
            -0.121086345000000E+03, 0.478073260000000E+02,
            -0.121093611000000E+03, 0.477997330000000E+02,
            -0.121099711000000E+03, 0.477969330000000E+02,
            -0.121103211000000E+03, 0.477967330000000E+02,
            -0.121106411000000E+03, 0.477950330000000E+02,
            -0.121109211000000E+03, 0.477923330000000E+02,
            -0.121109211000000E+03, 0.477887330000000E+02,
            -0.121111511000000E+03, 0.477876330000000E+02,
            -0.121115711000000E+03, 0.477872330000000E+02,
            -0.121120411000000E+03, 0.477841330000000E+02,
            -0.121120511000000E+03, 0.477824330000000E+02,
            -0.121119010000000E+03, 0.477799330000000E+02,
            -0.121119010000000E+03, 0.477799330000000E+02,
            -0.121187140000000E+03, 0.477801070000000E+02,
            -0.121192721333333E+03, 0.477797896666667E+02,
            -0.121195512000000E+03, 0.477791310000000E+02,
            -0.121232875000000E+03, 0.477792970000000E+02,
            -0.121232875000000E+03, 0.477792970000000E+02,
            -0.121243947000000E+03, 0.477793470000000E+02,
            -0.121243947000000E+03, 0.477793470000000E+02,
            -0.121455615000000E+03, 0.477803280000000E+02,
            -0.121455715000000E+03, 0.477783280000000E+02,
            -0.121485155000000E+03, 0.477779400000000E+02,
            -0.121500513000000E+03, 0.477777330000000E+02,
            -0.121584519000000E+03, 0.477775260000000E+02,
            -0.121584537000000E+03, 0.477767260000000E+02,
            -0.121610457000000E+03, 0.477767880000000E+02,
            -0.121610457000000E+03, 0.477767880000000E+02,
            -0.121645526000000E+03, 0.477768710000000E+02,
            -0.121645526000000E+03, 0.477768710000000E+02,
            -0.121666777000000E+03, 0.477769220000000E+02,
            -0.121751227000000E+03, 0.477771230000000E+02,
            -0.121751227000000E+03, 0.477771230000000E+02,
            -0.121763454000000E+03, 0.477769110000000E+02,
            -0.121763454000000E+03, 0.477769110000000E+02,
            -0.121838850000000E+03, 0.477771630000000E+02,
            -0.121838850000000E+03, 0.477771630000000E+02,
            -0.121849653000000E+03, 0.477772230000000E+02,
            -0.121850431000000E+03, 0.477772150000000E+02,
            -0.121863172000000E+03, 0.477772230000000E+02,
            -0.121863172000000E+03, 0.477772230000000E+02,
            -0.121864759000000E+03, 0.477772230000000E+02,
            -0.121864759000000E+03, 0.477772230000000E+02,
            -0.121887572000000E+03, 0.477765440000000E+02,
            -0.121922989000000E+03, 0.477769250000000E+02,
            -0.121928146000000E+03, 0.477769710000000E+02,
            -0.121928146000000E+03, 0.477769710000000E+02,
            -0.121932762000000E+03, 0.477768940000000E+02,
            -0.121932762000000E+03, 0.477768940000000E+02,
            -0.121943467000000E+03, 0.477766740000000E+02,
            -0.121943467000000E+03, 0.477766740000000E+02,
            -0.121960557000000E+03, 0.477763290000000E+02,
            -0.121960557000000E+03, 0.477763290000000E+02,
            -0.121967149000000E+03, 0.477761380000000E+02,
            -0.121967149000000E+03, 0.477761380000000E+02,
            -0.121967576000000E+03, 0.477761920000000E+02,
            -0.121967576000000E+03, 0.477761920000000E+02,
            -0.121967660000000E+03, 0.477761880000000E+02,
            -0.121967660000000E+03, 0.477761880000000E+02,
            -0.121967910000000E+03, 0.477761770000000E+02,
            -0.121968079000000E+03, 0.477761690000000E+02,
            -0.121968079000000E+03, 0.477761690000000E+02,
            -0.121975389000000E+03, 0.477758870000000E+02,
            -0.121975389000000E+03, 0.477758870000000E+02,
            -0.121999651000000E+03, 0.477752840000000E+02,
            -0.121999651000000E+03, 0.477752840000000E+02,
            -0.122001512000000E+03, 0.477752840000000E+02,
            -0.122001512000000E+03, 0.477752840000000E+02,
            -0.122010148000000E+03, 0.477753390000000E+02,
            -0.122010148000000E+03, 0.477753390000000E+02,
            -0.122014113000000E+03, 0.477753640000000E+02,
            -0.122014113000000E+03, 0.477753640000000E+02,
            -0.122023485000000E+03, 0.477754310000000E+02,
            -0.122036019000000E+03, 0.477757070000000E+02,
            -0.122045062000000E+03, 0.477755640000000E+02,
            -0.122053927000000E+03, 0.477754290000000E+02,
            -0.122053927000000E+03, 0.477754290000000E+02,
            -0.122054706000000E+03, 0.477754290000000E+02,
            -0.122067798000000E+03, 0.477756200000000E+02,
            -0.122074267000000E+03, 0.477757340000000E+02,
            -0.122076342000000E+03, 0.477757650000000E+02,
            -0.122080600000000E+03, 0.477757260000000E+02,
            -0.122080600000000E+03, 0.477757260000000E+02,
            -0.122082232000000E+03, 0.477756730000000E+02,
            -0.122083026000000E+03, 0.477756420000000E+02,
            -0.122083026000000E+03, 0.477756420000000E+02,
            -0.122091044000000E+03, 0.477756720000000E+02,
            -0.122093211000000E+03, 0.477756710000000E+02,
            -0.122093211000000E+03, 0.477756710000000E+02,
            -0.122094518000000E+03, 0.477756700000000E+02,
            -0.122094518000000E+03, 0.477756700000000E+02,
            -0.122095362000000E+03, 0.477756700000000E+02,
            -0.122095362000000E+03, 0.477756700000000E+02,
            -0.122100848000000E+03, 0.477756670000000E+02,
            -0.122100848000000E+03, 0.477756670000000E+02,
            -0.122101443000000E+03, 0.477756670000000E+02,
            -0.122101443000000E+03, 0.477756670000000E+02,
            -0.122102608000000E+03, 0.477756660000000E+02,
            -0.122102608000000E+03, 0.477756660000000E+02,
            -0.122105900000000E+03, 0.477756640000000E+02,
            -0.122107258000000E+03, 0.477756640000000E+02,
            -0.122108921000000E+03, 0.477756640000000E+02,
            -0.122108921000000E+03, 0.477756640000000E+02,
            -0.122111873000000E+03, 0.477756620000000E+02,
            -0.122111873000000E+03, 0.477756620000000E+02,
            -0.122112797000000E+03, 0.477756610000000E+02,
            -0.122116410000000E+03, 0.477756580000000E+02,
            -0.122116410000000E+03, 0.477756580000000E+02,
            -0.122118122000000E+03, 0.477756570000000E+02,
            -0.122118122000000E+03, 0.477756570000000E+02,
            -0.122131641000000E+03, 0.477757940000000E+02,
            -0.122131641000000E+03, 0.477757940000000E+02,
            -0.122151767000000E+03, 0.477760190000000E+02,
            -0.122151767000000E+03, 0.477760190000000E+02,
            -0.122152866000000E+03, 0.477760200000000E+02,
            -0.122152866000000E+03, 0.477760200000000E+02,
            -0.122153636000000E+03, 0.477760200000000E+02,
            -0.122153636000000E+03, 0.477760200000000E+02,
            -0.122154036000000E+03, 0.477760200000000E+02,
            -0.122154536000000E+03, 0.477761200000000E+02,
            -0.122154836000000E+03, 0.477760200000000E+02,
            -0.122154836000000E+03, 0.477760200000000E+02,
            -0.122158036000000E+03, 0.477760200000000E+02,
            -0.122158036000000E+03, 0.477760200000000E+02,
            -0.122164036000000E+03, 0.477760200000000E+02,
            -0.122166936000000E+03, 0.477761200000000E+02,
            -0.122169564000000E+03, 0.477761200000000E+02,
            -0.122169836000000E+03, 0.477761200000000E+02,
            -0.122171921000000E+03, 0.477761200000000E+02,
            -0.122171921000000E+03, 0.477761200000000E+02,
            -0.122175429000000E+03, 0.477761200000000E+02,
            -0.122181537000000E+03, 0.477763200000000E+02,
            -0.122187252000000E+03, 0.477762360000000E+02,
            -0.122191037000000E+03, 0.477762200000000E+02,
            -0.122191037000000E+03, 0.477762200000000E+02,
            -0.122201737000000E+03, 0.477764200000000E+02,
            -0.122201737000000E+03, 0.477764200000000E+02,
            -0.122203237000000E+03, 0.477764200000000E+02,
            -0.122203237000000E+03, 0.477764200000000E+02,
            -0.122207037000000E+03, 0.477764200000000E+02,
            -0.122212437000000E+03, 0.477765200000000E+02,
            -0.122212437000000E+03, 0.477765200000000E+02,
            -0.122216937000000E+03, 0.477765200000000E+02,
            -0.122220261000000E+03, 0.477765740000000E+02,
            -0.122223137000000E+03, 0.477766200000000E+02,
            -0.122223137000000E+03, 0.477766200000000E+02,
            -0.122226137000000E+03, 0.477766200000000E+02,
            -0.122226137000000E+03, 0.477766200000000E+02,
            -0.122227282000000E+03, 0.477766370000000E+02,
            -0.122227282000000E+03, 0.477766370000000E+02,
            -0.122231138000000E+03, 0.477766940000000E+02,
            -0.122231138000000E+03, 0.477766940000000E+02,
            -0.122232937000000E+03, 0.477767200000000E+02,
            -0.122232937000000E+03, 0.477767200000000E+02,
            -0.122237537000000E+03, 0.477768200000000E+02,
            -0.122237537000000E+03, 0.477768200000000E+02,
            -0.122237596000000E+03, 0.477768200000000E+02,
            -0.122237596000000E+03, 0.477768200000000E+02,
            -0.122243862000000E+03, 0.477768910000000E+02,
            -0.122245475000000E+03, 0.477768170000000E+02,
            -0.122248692000000E+03, 0.477766710000000E+02,
            -0.122248692000000E+03, 0.477766710000000E+02,
            -0.122249137000000E+03, 0.477768190000000E+02,
            -0.122249137000000E+03, 0.477768190000000E+02,
            -0.122253138000000E+03, 0.477769190000000E+02,
            -0.122257838000000E+03, 0.477769190000000E+02,
            -0.122257838000000E+03, 0.477769190000000E+02,
            -0.122260738000000E+03, 0.477769190000000E+02,
            -0.122260738000000E+03, 0.477769190000000E+02,
            -0.122263338000000E+03, 0.477769190000000E+02,
            -0.122263338000000E+03, 0.477769190000000E+02,
            -0.122265175000000E+03, 0.477768320000000E+02,
            -0.122265175000000E+03, 0.477768320000000E+02,
            -0.122265438000000E+03, 0.477768190000000E+02,
            -0.122265438000000E+03, 0.477768190000000E+02,
            -0.122266838000000E+03, 0.477770190000000E+02,
            -0.122271038000000E+03, 0.477770190000000E+02,
            -0.122281539000000E+03, 0.477770190000000E+02,
            -0.122281539000000E+03, 0.477770190000000E+02,
            -0.122284239000000E+03, 0.477770190000000E+02,
            -0.122287039000000E+03, 0.477771190000000E+02,
            -0.122288478000000E+03, 0.477772360000000E+02,
            -0.122290339000000E+03, 0.477773190000000E+02,
            -0.122293739000000E+03, 0.477772190000000E+02,
            -0.122297939000000E+03, 0.477773190000000E+02,
            -0.122303039000000E+03, 0.477774190000000E+02,
            -0.122304039000000E+03, 0.477774190000000E+02,
            -0.122305739000000E+03, 0.477773190000000E+02,
            -0.122305739000000E+03, 0.477773190000000E+02,
            -0.122308339000000E+03, 0.477774190000000E+02,
            -0.122309640000000E+03, 0.477774190000000E+02,
            -0.122309640000000E+03, 0.477774190000000E+02,
            -0.122313740000000E+03, 0.477774190000000E+02,
            -0.122318040000000E+03, 0.477775190000000E+02,
            -0.122323040000000E+03, 0.477775190000000E+02,
            -0.122324640000000E+03, 0.477775190000000E+02,
            -0.122326540000000E+03, 0.477776190000000E+02,
            -0.122329940000000E+03, 0.477776190000000E+02,
            -0.122335340000000E+03, 0.477776190000000E+02,
            -0.122337541000000E+03, 0.477776190000000E+02,
            -0.122346241000000E+03, 0.477777190000000E+02,
            -0.122347641000000E+03, 0.477777190000000E+02,
            -0.122347641000000E+03, 0.477777190000000E+02,
            -0.122351741000000E+03, 0.477777190000000E+02,
            -0.122351741000000E+03, 0.477777190000000E+02,
            -0.122356141000000E+03, 0.477777190000000E+02,
            -0.122358041000000E+03, 0.477778190000000E+02,
            -0.122358041000000E+03, 0.477778190000000E+02,
            -0.122361541000000E+03, 0.477777190000000E+02,
            -0.122364141000000E+03, 0.477778190000000E+02,
            -0.122364141000000E+03, 0.477778190000000E+02,
            -0.122366842000000E+03, 0.477778190000000E+02,
            -0.122372142000000E+03, 0.477778190000000E+02,
            -0.122372142000000E+03, 0.477778190000000E+02,
            -0.122377442000000E+03, 0.477779190000000E+02,
            -0.122377442000000E+03, 0.477779190000000E+02,
            -0.122380042000000E+03, 0.477779190000000E+02,
            -0.122380042000000E+03, 0.477779190000000E+02,
            -0.122382742000000E+03, 0.477779190000000E+02,
            -0.122386961000000E+03, 0.477779190000000E+02,
            -0.122386961000000E+03, 0.477779190000000E+02,
            -0.122388142000000E+03, 0.477779190000000E+02,
            -0.122388142000000E+03, 0.477779190000000E+02,
            -0.122389442000000E+03, 0.477779190000000E+02,
            -0.122389442000000E+03, 0.477779190000000E+02,
            -0.122393142000000E+03, 0.477780190000000E+02,
            -0.122394543000000E+03, 0.477779190000000E+02,
            -0.122396421740573E+03, 0.477779275945007E+02,
            -0.122396421740573E+03, 0.477779275945007E+02,
            -0.122397043000000E+03, 0.477797190000000E+02,
            -0.122396534465244E+03, 0.477854364424491E+02,
            -0.122394944000000E+03, 0.478033180000000E+02,
            -0.122392044000000E+03, 0.478077180000000E+02,
            -0.122384862811656E+03, 0.478138072035186E+02,
            -0.122380307756354E+03, 0.478176696086589E+02,
            -0.122374217477324E+03, 0.478228337885573E+02,
            -0.122360724049282E+03, 0.478342753808922E+02,
            -0.122353244000000E+03, 0.478406180000000E+02,
            -0.122350684564244E+03, 0.478413056096062E+02,
            -0.122348752434043E+03, 0.478418246893617E+02,
            -0.122346544000000E+03, 0.478424180000000E+02,
            -0.122341304034102E+03, 0.478458319171763E+02,
            -0.122339944000000E+03, 0.478467180000000E+02,
            -0.122339827791549E+03, 0.478468805870871E+02,
            -0.122339804214125E+03, 0.478469135742284E+02,
            -0.122337511417390E+03, 0.478501214230407E+02,
            -0.122336387866290E+03, 0.478516933818662E+02,
            -0.122335950000000E+03, 0.478523060000000E+02,
            -0.122335784639446E+03, 0.478527477876357E+02,
            -0.122335125173180E+03, 0.478545096591012E+02,
            -0.122333337627946E+03, 0.478592853771399E+02,
            -0.122333244646775E+03, 0.478595337914727E+02,
            -0.122333166059793E+03, 0.478597437493864E+02,
            -0.122331377698213E+03, 0.478645216484286E+02,
            -0.122329545000000E+03, 0.478694180000000E+02,
            -0.122330145000000E+03, 0.478753180000000E+02,
            -0.122332898615630E+03, 0.478793114719903E+02,
            -0.122333512967759E+03, 0.478802024452958E+02,
            -0.122333543000000E+03, 0.478802460000000E+02,
            -0.122333519287997E+03, 0.478803298532730E+02,
            -0.122333419884438E+03, 0.478806813762436E+02,
            -0.122329940049111E+03, 0.478929871937493E+02,
            -0.122328546000000E+03, 0.478979170000000E+02,
            -0.122327964226333E+03, 0.478991241434502E+02,
            -0.122327508748944E+03, 0.479000692301347E+02,
            -0.122325851976752E+03, 0.479035069273256E+02,
            -0.122324721848515E+03, 0.479058518717186E+02,
            -0.122322126419367E+03, 0.479112372225398E+02,
            -0.122321988336234E+03, 0.479115237362806E+02,
            -0.122321847000000E+03, 0.479118170000000E+02,
            -0.122321670246555E+03, 0.479120287856595E+02,
            -0.122317894659318E+03, 0.479165526874842E+02,
            -0.122313758740151E+03, 0.479215083383773E+02,
            -0.122311922524155E+03, 0.479237084890754E+02,
            -0.122310747000000E+03, 0.479251170000000E+02,
            -0.122309747000000E+03, 0.479291170000000E+02,
            -0.122311148000000E+03, 0.479367170000000E+02,
            -0.122308432988273E+03, 0.479449282549786E+02,
            -0.122307396400753E+03, 0.479480633001614E+02,
            -0.122307048000000E+03, 0.479491170000000E+02,
            -0.122304783815130E+03, 0.479496947375965E+02,
            -0.122292823906295E+03, 0.479527464711706E+02,
            -0.122291553132752E+03, 0.479530707263417E+02,
            -0.122278047000000E+03, 0.479565170000000E+02,
            -0.122265513875538E+03, 0.479578074284483E+02,
            -0.122260369648484E+03, 0.479583370854350E+02,
            -0.122259748739263E+03, 0.479584010151379E+02,
            -0.122249007000000E+03, 0.479595070000000E+02,
            -0.122240423047546E+03, 0.479646724921948E+02,
            -0.122240210295359E+03, 0.479648005182721E+02,
            -0.122233273935967E+03, 0.479689745523765E+02,
            -0.122231285123904E+03, 0.479701713429279E+02,
            -0.122230046000000E+03, 0.479709170000000E+02,
            -0.122226346000000E+03, 0.479764170000000E+02,
            -0.122228793923343E+03, 0.479809913163084E+02,
            -0.122229977428044E+03, 0.479832028744728E+02,
            -0.122229988452089E+03, 0.479832234745734E+02,
            -0.122232135020607E+03, 0.479872346636528E+02,
            -0.122232141937724E+03, 0.479872475893342E+02,
            -0.122232229101693E+03, 0.479874104684412E+02,
            -0.122232391000000E+03, 0.479877130000000E+02,
            -0.122230220000000E+03, 0.480071540000000E+02,
            -0.122228767000000E+03, 0.480124680000000E+02,
            -0.122225088297986E+03, 0.480165060261282E+02,
            -0.122224979000000E+03, 0.480166260000000E+02,
            -0.122225798126625E+03, 0.480182263284835E+02,
            -0.122230710998705E+03, 0.480278246114484E+02,
            -0.122231761000000E+03, 0.480298760000000E+02,
            -0.122232321504034E+03, 0.480301023219974E+02,
            -0.122254599886597E+03, 0.480390979540272E+02,
            -0.122262767321792E+03, 0.480423958254700E+02,
            -0.122281087000000E+03, 0.480497930000000E+02,
            -0.122283631375812E+03, 0.480522213158429E+02,
            -0.122288294092955E+03, 0.480566713463202E+02,
            -0.122293714987741E+03, 0.480618449706844E+02,
            -0.122298354311474E+03, 0.480662726748267E+02,
            -0.122305396518801E+03, 0.480729936566653E+02,
            -0.122305838000000E+03, 0.480734150000000E+02,
            -0.122305965092455E+03, 0.480735118308217E+02,
            -0.122321709000000E+03, 0.480855070000000E+02,
            -0.122326119000000E+03, 0.480928770000000E+02,
            -0.122343241000000E+03, 0.480976310000000E+02,
            -0.122363352984442E+03, 0.481233057283549E+02,
            -0.122363842000000E+03, 0.481239300000000E+02,
            -0.122365078000000E+03, 0.481258220000000E+02,
            -0.122365061393677E+03, 0.481260415638450E+02,
            -0.122365057215413E+03, 0.481260968076112E+02,
            -0.122364525958075E+03, 0.481331209337108E+02,
            -0.122364098326614E+03, 0.481387749493718E+02,
            -0.122363797000000E+03, 0.481427590000000E+02,
            -0.122363828056996E+03, 0.481430392344557E+02,
            -0.122364744000000E+03, 0.481513040000000E+02,
            -0.122365735739464E+03, 0.481537351928586E+02,
            -0.122367846276966E+03, 0.481589090554402E+02,
            -0.122368583107934E+03, 0.481607153546274E+02,
            -0.122370144073460E+03, 0.481645419727863E+02,
            -0.122370204412539E+03, 0.481646898906043E+02,
            -0.122370224407165E+03, 0.481647389062914E+02,
            -0.122370253000000E+03, 0.481648090000000E+02,
            -0.122369253922575E+03, 0.481662291530152E+02,
            -0.122364124285504E+03, 0.481735207496130E+02,
            -0.122363479000000E+03, 0.481744380000000E+02,
            -0.122362044000000E+03, 0.481875680000000E+02,
            -0.122372492000000E+03, 0.481930220000000E+02,
            -0.122372500330439E+03, 0.481930342087307E+02,
            -0.122372618769131E+03, 0.481932077873503E+02,
            -0.122376763295846E+03, 0.481992818262957E+02,
            -0.122378057879514E+03, 0.482011791120790E+02,
            -0.122382102000000E+03, 0.482071060000000E+02,
            -0.122385703000000E+03, 0.482178110000000E+02,
            -0.122387682622564E+03, 0.482199814020855E+02,
            -0.122388193386812E+03, 0.482205413895339E+02,
            -0.122388771591929E+03, 0.482211753172402E+02,
            -0.122390597938308E+03, 0.482231776716597E+02,
            -0.122394813168535E+03, 0.482277991306402E+02,
            -0.122395512417791E+03, 0.482285657677103E+02,
            -0.122395512417791E+03, 0.482285657677103E+02,
            -0.122394763000000E+03, 0.482301100000000E+02,
            -0.122394763000000E+03, 0.482301100000000E+02,
            -0.122392536000000E+03, 0.482345740000000E+02,
            -0.122392612000000E+03, 0.482378770000000E+02,
            -0.122394565000000E+03, 0.482396240000000E+02,
            -0.122399113000000E+03, 0.482420050000000E+02,
            -0.122404259000000E+03, 0.482494520000000E+02,
            -0.122404259000000E+03, 0.482494520000000E+02,
            -0.122405756181268E+03, 0.482521938138136E+02,
            -0.122405756181268E+03, 0.482521938138136E+02,
            -0.122395328000000E+03, 0.482571870000000E+02,
            -0.122392058000000E+03, 0.482696280000000E+02,
            -0.122371693000000E+03, 0.482878390000000E+02,
            -0.122376818000000E+03, 0.482960990000000E+02,
            -0.122378210535294E+03, 0.482975904179386E+02,
            -0.122378210535294E+03, 0.482975904179386E+02,
            -0.122365650000000E+03, 0.482976130000000E+02,
            -0.122365650000000E+03, 0.482976130000000E+02,
            -0.122365342000000E+03, 0.482976130000000E+02,
            -0.122365342000000E+03, 0.482976130000000E+02,
            -0.122352970000000E+03, 0.482975530000000E+02,
            -0.122335067000000E+03, 0.482974850000000E+02,
            -0.122335067000000E+03, 0.482974850000000E+02,
            -0.122324656000000E+03, 0.482975320000000E+02,
            -0.122304895000000E+03, 0.482977150000000E+02,
            -0.122304895000000E+03, 0.482977150000000E+02,
            -0.122304450000000E+03, 0.482977150000000E+02,
            -0.122304450000000E+03, 0.482977150000000E+02,
            -0.122292087000000E+03, 0.482977590000000E+02,
            -0.122292087000000E+03, 0.482977590000000E+02,
            -0.122275001000000E+03, 0.482976750000000E+02,
            -0.122253010000000E+03, 0.482975670000000E+02,
            -0.122253010000000E+03, 0.482975670000000E+02,
            -0.122251997000000E+03, 0.482975610000000E+02,
            -0.122251997000000E+03, 0.482975610000000E+02,
            -0.122226185000000E+03, 0.482973750000000E+02,
            -0.122205616000000E+03, 0.482973270000000E+02,
            -0.122205616000000E+03, 0.482973270000000E+02,
            -0.122204964000000E+03, 0.482973260000000E+02,
            -0.122204964000000E+03, 0.482973260000000E+02,
            -0.122201531000000E+03, 0.482973150000000E+02,
            -0.122187400000000E+03, 0.482972600000000E+02,
            -0.122187400000000E+03, 0.482972600000000E+02,
            -0.122184618000000E+03, 0.482972580000000E+02,
            -0.122184618000000E+03, 0.482972580000000E+02,
            -0.122175875000000E+03, 0.482972610000000E+02,
            -0.122175875000000E+03, 0.482972610000000E+02,
            -0.122170401000000E+03, 0.482973200000000E+02,
            -0.122170401000000E+03, 0.482973200000000E+02,
            -0.122169406000000E+03, 0.482973310000000E+02,
            -0.122169406000000E+03, 0.482973310000000E+02,
            -0.122164651000000E+03, 0.482974030000000E+02,
            -0.122164651000000E+03, 0.482974030000000E+02,
            -0.122140276000000E+03, 0.482977690000000E+02,
            -0.122035569000000E+03, 0.482976920000000E+02,
            -0.122000900000000E+03, 0.482975270000000E+02,
            -0.122000900000000E+03, 0.482975270000000E+02,
            -0.122000793000000E+03, 0.482975430000000E+02,
            -0.122000793000000E+03, 0.482975430000000E+02,
            -0.121998002000000E+03, 0.482975500000000E+02,
            -0.121981660000000E+03, 0.482975960000000E+02,
            -0.121981660000000E+03, 0.482975960000000E+02,
            -0.121972070000000E+03, 0.482976240000000E+02,
            -0.121972070000000E+03, 0.482976240000000E+02,
            -0.121945086000000E+03, 0.482976750000000E+02,
            -0.121935720000000E+03, 0.482976960000000E+02,
            -0.121924030000000E+03, 0.482977220000000E+02,
            -0.121924030000000E+03, 0.482977220000000E+02,
            -0.121915424000000E+03, 0.482976980000000E+02,
            -0.121915424000000E+03, 0.482976980000000E+02,
            -0.121905052000000E+03, 0.482976510000000E+02,
            -0.121876247000000E+03, 0.482975180000000E+02,
            -0.121859728000000E+03, 0.482979430000000E+02,
            -0.121859728000000E+03, 0.482979430000000E+02,
            -0.121857286000000E+03, 0.482979510000000E+02,
            -0.121857286000000E+03, 0.482979510000000E+02,
            -0.121856480000000E+03, 0.482979530000000E+02,
            -0.121835921000000E+03, 0.482980130000000E+02,
            -0.121835921000000E+03, 0.482980130000000E+02,
            -0.121835846000000E+03, 0.482980130000000E+02,
        };
}