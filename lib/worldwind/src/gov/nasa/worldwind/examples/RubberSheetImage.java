/*
Copyright (C) 2001, 2009 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.data.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.examples.util.*;
import gov.nasa.worldwind.formats.gcps.GCPSReader;
import gov.nasa.worldwind.formats.tab.TABRasterReader;
import gov.nasa.worldwind.formats.worldfile.WorldFile;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.File;
import java.util.ArrayList;

/**
 * @author tag
 * @version $Id: RubberSheetImage.java 9579 2009-03-20 19:13:50Z dcollins $
 */
public class RubberSheetImage extends ApplicationTemplate
{
    public static final String OPEN_IMAGE_FILE = "OpenImageFile";
    public static final String SET_IMAGE_OPACITY = "SetImageOpacity";
    public static final String TOGGLE_EDITING = "ToggleEditing";

    protected static final String ENABLE_EDITING = "Enable Editing";
    protected static final String DISABLE_EDITING = "Disable Editing";

    public static class AppFrame extends ApplicationTemplate.AppFrame implements ActionListener
    {
        private Controller controller;
        private JButton toggleEditButton;

        public AppFrame()
        {
            this.controller = new Controller(this);
            this.controller.loadBackgroundData();
            this.getWwd().addSelectListener(this.controller);
            
            this.initComponents();
        }

        public JButton getToggleEditButton()
        {
            return this.toggleEditButton;
        }

        private void initComponents()
        {
            JMenuBar menuBar = new JMenuBar();
            {
                JMenu menu = new JMenu("File");

                JMenuItem item = new JMenuItem("Open...");
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                item.setActionCommand(OPEN_IMAGE_FILE);
                item.addActionListener(this);
                menu.add(item);

                menuBar.add(menu);
            }
            this.setJMenuBar(menuBar);

            JPanel controlPanel = new JPanel(new BorderLayout(0, 0)); // hgap, vgap
            {
                controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 0)); // top, left, bottom, right

                Box vbox = Box.createVerticalBox();

                JLabel label = new JLabel("Opacity");
                final JSlider slider = new JSlider(0, 100, 100);
                slider.addChangeListener(new ChangeListener()
                {
                    public void stateChanged(ChangeEvent changeEvent)
                    {
                        ActionEvent actionEvent = new ActionEvent(slider, 0, SET_IMAGE_OPACITY);
                        actionPerformed(actionEvent);
                    }
                });
                Box box = Box.createHorizontalBox();
                box.setAlignmentX(JComponent.LEFT_ALIGNMENT);
                box.add(label);
                box.add(slider);
                vbox.add(box);

                this.toggleEditButton = new JButton(DISABLE_EDITING);
                this.toggleEditButton.setAlignmentX(JComponent.LEFT_ALIGNMENT);
                this.toggleEditButton.setActionCommand(TOGGLE_EDITING);
                this.toggleEditButton.addActionListener(this);
                vbox.add(this.toggleEditButton);

                controlPanel.add(vbox, BorderLayout.NORTH);
            }
            this.getLayerPanel().add(controlPanel, BorderLayout.SOUTH);
        }

        public void actionPerformed(ActionEvent e)
        {
            if (e == null)
                return;

            if (this.controller == null)
                return;

            this.controller.actionPerformed(e);
        }
    }

    public static class SurfaceImageEntry
    {
        private SurfaceImage surfaceImage;
        private SurfaceImageEditor editor;
        private RenderableLayer layer;

        public SurfaceImageEntry(WorldWindow wwd, SurfaceImage surfaceImage, String name)
        {
            this.surfaceImage = surfaceImage;
            this.editor = new SurfaceImageEditor(wwd, surfaceImage);

            this.layer = new RenderableLayer();
            this.layer.setName(name);
            this.layer.setPickEnabled(true);
            this.layer.addRenderable(surfaceImage);

            insertBeforePlacenames(wwd, this.layer);
        }

        public SurfaceImage getSurfaceImage()
        {
            return this.surfaceImage;
        }

        public SurfaceImageEditor getEditor()
        {
            return this.editor;
        }

        public RenderableLayer getLayer()
        {
            return this.layer;
        }
    }

    public static class Controller implements ActionListener, SelectListener
    {
        private AppFrame appFrame;
        private JFileChooser openFileChooser;
        private ImageIOReader imageReader;

        private ArrayList<SurfaceImageEntry> entryList = new ArrayList<SurfaceImageEntry>();

        public Controller(AppFrame appFrame)
        {
            this.appFrame = appFrame;
            this.imageReader = new ImageIOReader();
        }

        public void actionPerformed(ActionEvent e)
        {
            String actionCommand = e.getActionCommand();
            if (actionCommand == null)
                return;

            //noinspection StringEquality
            if (actionCommand == OPEN_IMAGE_FILE)
            {
                this.doOpenImageFile();
            }
            else //noinspection StringEquality
                if (actionCommand == SET_IMAGE_OPACITY)
            {
                JSlider slider = (JSlider) e.getSource();
                this.doSetImageOpacity(slider.getValue() / 100.0);
            }
            else //noinspection StringEquality
                if (actionCommand == TOGGLE_EDITING)
            {
                String text = this.appFrame.getToggleEditButton().getText();
                boolean enabled = DISABLE_EDITING.equals(text);

                this.appFrame.getToggleEditButton().setText(enabled ? ENABLE_EDITING : DISABLE_EDITING);
                this.enableEditing(!enabled);
            }
        }

        public void selected(SelectEvent e)
        {
            PickedObject topObject = e.getTopPickedObject();

            if (e.getEventAction().equals(SelectEvent.LEFT_PRESS))
            {
                if (topObject != null && !topObject.isTerrain() && topObject.getObject() instanceof SurfaceImage)
                {
                    SurfaceImageEntry entry = this.getEntryFor((SurfaceImage) topObject.getObject());
                    if (entry != null)
                    {
                        this.setSelectedEntry(entry);
                    }
                }
            }
        }

        protected void enableEditing(boolean enable)
        {
            for (SurfaceImageEntry entry : this.entryList)
            {
                entry.getLayer().setPickEnabled(enable);
                if (!enable)
                {
                    entry.getEditor().setArmed(false);
                }
            }
        }

        protected void addSurfaceImage(SurfaceImage surfaceImage, String name)
        {
            SurfaceImageEntry entry = new SurfaceImageEntry(this.appFrame.getWwd(), surfaceImage, name);
            this.entryList.add(entry);
            this.setSelectedEntry(entry);

            this.appFrame.getLayerPanel().update(this.appFrame.getWwd());
        }

        protected SurfaceImageEntry getEntryFor(SurfaceImage surfaceImage)
        {
            for (SurfaceImageEntry entry : this.entryList)
            {
                if (entry.getSurfaceImage() == surfaceImage)
                {
                    return entry;
                }
            }

            return null;
        }

        protected void setSelectedEntry(SurfaceImageEntry selected)
        {
            for (SurfaceImageEntry entry : this.entryList)
            {
                if (entry != selected)
                {
                    if (entry.getEditor().isArmed())
                    {
                        entry.getEditor().setArmed(false);
                    }
                }
            }

            if (!selected.getEditor().isArmed())
            {
                selected.getEditor().setArmed(true);
            }
        }

        protected void doOpenImageFile()
        {
            if (this.openFileChooser == null)
            {
                this.openFileChooser = new JFileChooser(Configuration.getUserHomeDirectory());
                this.openFileChooser.setAcceptAllFileFilterUsed(false);
                this.openFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                this.openFileChooser.setMultiSelectionEnabled(true);
                this.openFileChooser.addChoosableFileFilter(new ImageIOFileFilter(this.imageReader));
            }

            int retVal = this.openFileChooser.showOpenDialog(this.appFrame);
            if (retVal != JFileChooser.APPROVE_OPTION)
                return;

            File[] files = this.openFileChooser.getSelectedFiles();
            this.loadFiles(files);
        }

        protected void doSetImageOpacity(double opacity)
        {
            for (SurfaceImageEntry entry : this.entryList)
            {
                entry.getSurfaceImage().setOpacity(opacity);
            }

            this.appFrame.getWwd().redraw();
        }

        protected void loadBackgroundData()
        {
            DataDescriptor dataDescriptor = findDataDescriptor("Mercury_Orthophoto_2007");
            if (dataDescriptor != null)
            {
                TiledImageLayer layer = new BasicTiledImageLayer(dataDescriptor);
                layer.setName(dataDescriptor.getName());
                layer.setUseTransparentTextures(true);
                insertBeforePlacenames(this.appFrame.getWwd(), layer);

                this.appFrame.getLayerPanel().update(this.appFrame.getWwd());
            }
        }

        protected void loadFiles(final File[] files)
        {
            this.appFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            Thread thread = new Thread(new Runnable()
            {
                public void run()
                {
                    for (File f : files)
                    {
                        loadFile(f);
                    }

                    appFrame.setCursor(null);
                }
            });
            thread.start();
        }

        protected void loadFile(File file)
        {
            try
            {
                java.awt.image.BufferedImage image = this.imageReader.read(file);
                SurfaceImage si = null;

                File tabFile = this.getAssociatedTABFile(file);
                if (tabFile != null)
                    si = this.createSurfaceImageFromTABFile(image, tabFile);

                if (si == null)
                {
                    File gcpsFile = this.getAssociatedGCPSFile(file);
                    if (gcpsFile != null)
                        si = this.createSurfaceImageFromGCPSFile(image, gcpsFile);
                }

                if (si == null)
                {
                    File[] worldFiles = this.getAssociatedWorldFiles(file);
                    if (worldFiles != null)
                        si = this.createSurfaceImageFromWorldFiles(image, worldFiles);
                }

                if (si == null)
                {
                    StringBuilder message = new StringBuilder();
                    message.append("Unable to find geographic coordinates for: ");
                    message.append("\"").append(file.getPath()).append("\"");
                    message.append("\n");
                    message.append("Open image anyway?");

                    int retVal = JOptionPane.showConfirmDialog(this.appFrame, message, null, JOptionPane.YES_NO_OPTION);
                    if (retVal == JOptionPane.YES_OPTION)
                    {
                        si = this.createSurfaceImageFromViewport(image, this.appFrame.getWwd());
                    }
                }

                if (si != null)
                {
                    this.addSurfaceImage(si, file.getName());
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        public File getAssociatedTABFile(File file)
        {
            File tabFile = TABRasterReader.getTABFileFor(file);
            if (tabFile != null && tabFile.exists())
            {
                TABRasterReader reader = new TABRasterReader();
                if (reader.canRead(tabFile))
                    return tabFile;
            }

            return null;
        }

        public File getAssociatedGCPSFile(File file)
        {
            File gcpsFile = GCPSReader.getGCPSFileFor(file);
            if (gcpsFile != null && gcpsFile.exists())
            {
                GCPSReader reader = new GCPSReader();
                if (reader.canRead(gcpsFile))
                    return gcpsFile;
            }

            return null;
        }

        public File[] getAssociatedWorldFiles(File file)
        {
            try
            {
                File[] worldFiles = WorldFile.getWorldFiles(file);
                if (worldFiles != null && worldFiles.length > 0)
                    return worldFiles;
            }
            catch (Exception ignored)
            {
            }

            return null;
        }

        protected SurfaceImage createSurfaceImageFromWorldFiles(java.awt.image.BufferedImage image, File[] worldFiles)
            throws java.io.IOException
        {
            AVList worldFileParams = new AVListImpl();
            WorldFile.decodeWorldFiles(worldFiles, worldFileParams);
            
            BufferedImage alignedImage = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
            Sector sector = ImageUtil.warpImageWithWorldFile(image, worldFileParams, alignedImage);

            return new SurfaceImage(alignedImage, sector);
        }

        protected SurfaceImage createSurfaceImageFromTABFile(java.awt.image.BufferedImage image, File tabFile)
            throws java.io.IOException
        {
            TABRasterReader reader = new TABRasterReader();
            RasterControlPointList controlPoints = reader.read(tabFile);

            return this.createSurfaceImageFromControlPoints(image, controlPoints);
        }

        protected SurfaceImage createSurfaceImageFromGCPSFile(java.awt.image.BufferedImage image, File gcpsFile)
            throws java.io.IOException
        {
            GCPSReader reader = new GCPSReader();
            RasterControlPointList controlPoints = reader.read(gcpsFile);

            return this.createSurfaceImageFromControlPoints(image, controlPoints);
        }

        protected SurfaceImage createSurfaceImageFromControlPoints(java.awt.image.BufferedImage image,
            RasterControlPointList controlPoints) throws java.io.IOException
        {
            int numControlPoints = controlPoints.size();
            Point2D[] imagePoints = new Point2D[numControlPoints];
            LatLon[] geoPoints = new LatLon[numControlPoints];

            for (int i = 0; i < numControlPoints; i++)
            {
                RasterControlPointList.ControlPoint p = controlPoints.get(i);
                imagePoints[i] = p.getRasterPoint();
                geoPoints[i] = p.getWorldPointAsLatLon();
            }

            BufferedImage destImage = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
            Sector sector = ImageUtil.warpImageWithControlPoints(image, imagePoints, geoPoints, destImage);

            return new SurfaceImage(destImage, sector);
        }

        protected SurfaceImage createSurfaceImageFromViewport(java.awt.image.BufferedImage image, WorldWindow wwd)
        {
            Position position = ShapeUtils.getNewShapePosition(wwd);
            Angle heading = ShapeUtils.getNewShapeHeading(wwd, false);
            double sizeInMeters = ShapeUtils.getViewportScaleFactor(wwd);

            java.util.List<LatLon> corners = ShapeUtils.createSquareInViewport(wwd, position, heading, sizeInMeters);

            return new SurfaceImage(image, corners);
        }
    }

    protected static DataDescriptor findDataDescriptor(String name)
    {
        for (File file : WorldWind.getDataFileStore().getLocations())
        {
            if (!WorldWind.getDataFileStore().isInstallLocation(file.getPath()))
                continue;

            for (DataDescriptor dataDescriptor : WorldWind.getDataFileStore().findDataDescriptors(file.getPath()))
            {
                if (dataDescriptor.getName().equals(name))
                    return dataDescriptor;
            }
        }

        return null;
    }

    public static void main(String[] args)
    {
        ApplicationTemplate.start("World Wind Rubber Sheet Image", RubberSheetImage.AppFrame.class);
    }
}
