/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.formats.vpf.*;
import gov.nasa.worldwind.util.*;

import javax.swing.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

/**
 * @author dcollins
 * @version $Id: VPFLayerDemo.java 12523 2009-08-26 22:36:07Z tgaskins $
 */
public class VPFLayerDemo extends ApplicationTemplate
{
    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        public AppFrame()
        {
            JPanel layerPanelx = this.getLayerPanel();
            this.getContentPane().remove(layerPanelx);

            JPanel panel = new JPanel(new BorderLayout(0, 0)); // hgap, vgap
            {
                panel.add(layerPanelx, BorderLayout.CENTER);

                JPanel buttonPanel = new JPanel(new GridLayout(1, 0, 0, 0)); // nrows, ncols, hgap, vgap
                buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10)); // top, left, bottom, right
                JButton button = new JButton("Open VPF Database");
                button.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent actionEvent)
                    {
                        showOpenDialog();
                    }
                });
                buttonPanel.add(button);
                panel.add(buttonPanel, BorderLayout.SOUTH);
            }
            this.getContentPane().add(panel, BorderLayout.WEST);
        }

        public void showOpenDialog()
        {
            JFileChooser fc = new JFileChooser(Configuration.getUserHomeDirectory());
            fc.addChoosableFileFilter(new VPFFileFilter());

            int retVal = fc.showOpenDialog(this);
            if (retVal != JFileChooser.APPROVE_OPTION)
                return;

            File file = fc.getSelectedFile();
            this.addVPFLayer(file);
        }

        public void addVPFLayer(File file)
        {
            VPFDatabase db = VPFUtils.readDatabase(file);
            VPFLayer layer = new VPFLayer(db);
            insertBeforePlacenames(this.getWwd(), layer);
            this.getLayerPanel().update(this.getWwd());
            this.openVPFCoveragePanel(db, layer);
        }

        private void openVPFCoveragePanel(VPFDatabase db, VPFLayer layer)
        {
            VPFCoveragePanel panel = new VPFCoveragePanel(getWwd(), db);
            panel.setLayer(layer);
            JFrame frame = new JFrame(db.getName());
            frame.setResizable(true);
            frame.setAlwaysOnTop(true);
            frame.add(panel);
            frame.pack();
            WWUtil.alignComponent(this, frame, AVKey.CENTER);
            frame.setVisible(true);
        }
    }

    public static class VPFFileFilter extends FileFilter
    {
        private VPFDatabaseFilter filter;

        public VPFFileFilter()
        {
            this.filter = new VPFDatabaseFilter();
        }

        public boolean accept(File file)
        {
            if (file == null)
            {
                String message = Logging.getMessage("nullValue.FileIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            return file.isDirectory() || this.filter.accept(file);
        }

        public String getDescription()
        {
            return "VPF Databases (dht)";
        }
    }

    public static void main(String[] args)
    {
        start("World Wind VPF Shapes", AppFrame.class);
    }
}
