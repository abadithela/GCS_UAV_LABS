/*
Copyright (C) 2001, 2009 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.util.dashboard;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.util.StatisticsPanel;
import gov.nasa.worldwind.util.*;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author tag
 * @version $Id: DashboardDialog.java 11475 2009-06-06 01:39:21Z tgaskins $
 */
public class DashboardDialog extends JDialog
{
    private WorldWindow wwd;

    public DashboardDialog(Frame parent, WorldWindow wwd) throws HeadlessException
    {
        super(parent, "WWJ Dashboard");

        this.wwd = wwd;

        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(this.createControls(), BorderLayout.CENTER);
        this.pack();

        this.setResizable(false);
        this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
    }

    @Override
    public void dispose()
    {
        super.dispose();

        this.wwd = null;
    }

    public void raiseDialog()
    {
        makeCurrent();
        WWUtil.alignComponent(this.getParent(), this, AVKey.RIGHT);
        this.setVisible(true);
    }

    public void lowerDialog()
    {
        setVisible(false);
    }

    private void makeCurrent()
    {
    }

    private class OkayAction extends AbstractAction
    {
        public OkayAction()
        {
            super("Okay");
        }

        public void actionPerformed(ActionEvent e)
        {
            lowerDialog();
        }
    }

    private JTabbedPane createControls()
    {
        JTabbedPane tabPane = new JTabbedPane();

        JPanel panel = new JPanel(new BorderLayout(10, 20));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.add(makeControlPanel(), BorderLayout.CENTER);
        panel.add(makeOkayCancelPanel(), BorderLayout.SOUTH);
        tabPane.add("Controls", panel);

        tabPane.add("Performance", new StatisticsPanel(this.wwd, new Dimension(250, 500)));

        return tabPane;
    }

    private JPanel makeControlPanel()
    {
        JPanel panel = new JPanel(new FlowLayout(SwingConstants.VERTICAL));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        panel.add(makeTerrainControlPanel());

        return panel;
    }

    private JPanel makeTerrainControlPanel()
    {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));

        panel.setBorder(new CompoundBorder(new TitledBorder("Terrain"), new EmptyBorder(10, 10, 10, 10)));

        final JRadioButton triangleButton = new JRadioButton("Show Triangles w/o Skirts");
        panel.add(triangleButton);

        final JRadioButton skirtsButton = new JRadioButton("Make Triangles w/ Skirts");
        panel.add(skirtsButton);

        JCheckBox tileButton = new JCheckBox("Show Tile Boundaries");
        panel.add(tileButton);

        JCheckBox extentsButton = new JCheckBox("Show Tile Extents");
        panel.add(extentsButton);

        ActionListener listener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                boolean tris = triangleButton.isSelected();
                boolean skirts = skirtsButton.isSelected();

                if (tris && e.getSource() == triangleButton)
                {
                    wwd.getModel().setShowWireframeInterior(true);
                    wwd.getModel().getGlobe().getTessellator().setMakeTileSkirts(false);
                    skirtsButton.setSelected(false);
                }
                else if (skirts && e.getSource() == skirtsButton)
                {
                    wwd.getModel().setShowWireframeInterior(true);
                    wwd.getModel().getGlobe().getTessellator().setMakeTileSkirts(true);
                    triangleButton.setSelected(false);
                }
                else
                {
                    wwd.getModel().setShowWireframeInterior(false);
                    wwd.getModel().getGlobe().getTessellator().setMakeTileSkirts(true);
                }

                wwd.redraw();
            }
        };
        triangleButton.addActionListener(listener);
        skirtsButton.addActionListener(listener);
//
//        skirtsButton.addActionListener(new ActionListener()
//        {
//            public void actionPerformed(ActionEvent e)
//            {
//                boolean tf = ((JRadioButton)e.getSource()).isSelected();
//                if (tf)
//                    triangleButton.setSelected(false);
//                wwd.getModel().setShowWireframeInterior(tf);
//                wwd.getModel().getGlobe().getTessellator().setMakeTileSkirts(true);
//                wwd.redraw();
//            }
//        });

        tileButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                wwd.getModel().setShowWireframeExterior(!wwd.getModel().isShowWireframeExterior());
                wwd.redraw();
            }
        });

        extentsButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                wwd.getModel().setShowTessellationBoundingVolumes(!wwd.getModel().isShowTessellationBoundingVolumes());
                wwd.redraw();
            }
        });

        return panel;
    }

    private JPanel makeOkayCancelPanel()
    {
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 10));

        panel.add(new JLabel(""));
        panel.add(new JButton(new OkayAction()));

        return panel;
    }
}
