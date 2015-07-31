package des.map;


import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.CellEditor;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * PanelMap.java
 *
 * Created on Aug 19, 2010, 10:39:45 AM
 */
/**
 *
 * @author david2010a
 */
public class PanelMap extends javax.swing.JPanel implements Runnable, PanelDrawingMapListener {

    private int imageWidth;
    private int imageHeight;
    private double latLenght = 0;
    private double lonLenght = 0;
    private double[] x;
    private double[] y;
    private int zoom = 1;
    private double latCenter = 0;
    private double lonCenter = 0;
    private double m_lon = 0;
    private double m_lat = 0;
    private double b_lon = 0;
    private double b_lat = 0;
    private int bufferSize = 100;
    private int countBuffer = 0;
    private double resolution = 0;
    private double lat_1 = -10000;
    private double lon_1 = -10000;
    private boolean enable=false;
    private int lastZoom = 0;
    private final ArrayList<Integer> editedRows = new ArrayList<Integer>();
    private boolean userEditingWaypoints = false;       
    
    class Waypoint{
        private double longitude;
        private double latitude;
        private double altitude;
        
        Waypoint(){            
        }
        
        Waypoint(double longitude, double latitude, double altitude){
            this.longitude = longitude;
            this.latitude = latitude;
            this.altitude = altitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getAltitude() {
            return altitude;
        }

        public void setAltitude(double altitude) {
            this.altitude = altitude;
        }
        
        
    }
    
    HashMap<Integer, Waypoint> waypointsToSend = new HashMap<Integer, Waypoint>();

    /** Creates new form PanelMap */
    public PanelMap() {
        initComponents();
        panelDrawing.registerPanelDrawingMapListener(this);

        x = new double[bufferSize];
        y = new double[bufferSize];

        DefaultTableModel model = (DefaultTableModel) tblWaypoints.getModel();   
        
        model.addTableModelListener(new TableModelListener() {

            public synchronized void tableChanged(TableModelEvent e) {     
                synchronized(editedRows){
                    int row = e.getFirstRow();               
                    if(userEditingWaypoints) {
                        addEditedRow(row);               
                        userEditingWaypoints = false;
                    }  
                }                
            }
        });
        tblWaypoints.setDefaultRenderer(Double.class, renderer);
        tblWaypoints.setDefaultRenderer(Integer.class, renderer);     
    }

    public void setPoint(double lat, double lon) {
        if(countBuffer>bufferSize-1){
            x[0]=x[bufferSize-1];
            y[0]=y[bufferSize-1];
            countBuffer=1;
        }
        //x[countBuffer] = (int) (lon * m_lon + b_lon);
        //y[countBuffer] = imageHeight - (int) (lat * m_lat + b_lat);
        x[countBuffer] = lat;
        y[countBuffer] = lon;
        countBuffer++;
        lat_1 = lat;
        lon_1 = lon;

        panelDrawing.setPositions(x, y, countBuffer);
    }
    
    public void setAltitude(double altitude){
        if(enable) panelDrawing.setCurrentAltitude(altitude);
    }
    
    public void setPhi(double phiRad){
        if(enable) panelDrawing.setCurrentPhi(phiRad);
    }
    
    public void setTheta(double thetaRad){
        if(enable) panelDrawing.setCurrentTheta(thetaRad);
    }
        
    public void setPsi(double psiRad){
        if(enable) panelDrawing.setCurrentPsi(psiRad);
    }
    
    public void setFlightState(double altitude, 
            double phiRad, double thetaRad, double psiRad){
        if(enable){
            panelDrawing.setCurrentAltitude(altitude);
            panelDrawing.setCurrentAttitude(phiRad, thetaRad, psiRad);
        }
    }
    
    

    private void updateMap() {

        set();
        panelDrawing.setLatitudeDeg(latCenter);
        panelDrawing.setLongitudeDeg(lonCenter);
        panelDrawing.setZoom(zoom);
        switch(comboBoxMapType.getSelectedIndex()){
            case 0:
                panelDrawing.setMapType(PanelDrawing.MapType.SATELLITE);
                break;
            case 1:
                panelDrawing.setMapType(PanelDrawing.MapType.ROADMAP);
                break;
            case 2:
                panelDrawing.setMapType(PanelDrawing.MapType.TERRAIN);
                break;
            case 3:
                panelDrawing.setMapType(PanelDrawing.MapType.HYBRID);
                break;
        }
        
        panelDrawing.updateMap();        
    }

    private void set() {
        bufferSize = 1024;
        resolution = 7; 

        x = new double[bufferSize];
        y = new double[bufferSize];
        countBuffer = 0;

        imageWidth = panelDrawing.getWidth();
        imageHeight = panelDrawing.getHeight();
        zoom = comboBoxZoom.getSelectedIndex() + 1;
        latCenter = Double.parseDouble(textLat.getText());
        lonCenter = Double.parseDouble(textLon.getText());

        latLenght = 180 / (Math.pow(2, zoom - 1));
        lonLenght = 360 / (Math.pow(2, zoom - 1));

        m_lon = imageWidth / lonLenght;
        b_lon = -1 * (m_lon) * (lonCenter - lonLenght / 2);

        m_lat = imageHeight / latLenght;
        b_lat = -1 * m_lat * (latCenter - latLenght / 2);

        //System.out.println("Par: latlenght:" + latLenght + " LonLength: " + lonLenght + " mlon: " + m_lon + " b_lon: " + b_lon);


    }
    
    private synchronized Double getTableContent(int row, int column){
        DefaultTableModel model = (DefaultTableModel) tblWaypoints.getModel();
        Object elem = model.getValueAt(row, column);        
        if(elem instanceof Double){
            return (Double) elem;
        }
        else
        {
            if(elem instanceof Float){
                return new Double((Float)elem);
            }
            Integer v = (Integer) elem;
            return new Double(v);
        }
    }
    
    private synchronized void addEditedRow(int row){
        for(Integer i : editedRows){
            if(i == row){
                return;
            }
        }
        editedRows.add(row);
    }
    
    private synchronized void sendWaypoints(){        
        try{
            DefaultTableModel model = (DefaultTableModel) tblWaypoints.getModel();
            for(Integer i : editedRows){
                //get waypoint                
                Integer id = (Integer) model.getValueAt(i, 0);
                assert(id - 1 == i);
                Double lat = getTableContent(i, 1) / 180 * Math.PI; // to radians
                Double lng = getTableContent(i, 2) / 180 * Math.PI;
                Double alt = getTableContent(i, 3) / 180 * Math.PI;
                Waypoint wp = new Waypoint(lat, lng, alt);
                waypointsToSend.put(1 + i, wp);
            }                                                
            for(Integer key : waypointsToSend.keySet()){
                Waypoint wp = waypointsToSend.get(key);                
                int longitude = (int) (wp.getLongitude() / Math.PI * 180* Math.pow(10, 7));
                int latitude = (int) (wp.getLatitude() / Math.PI * 180 * Math.pow(10, 7));                
                Double altitude = getTableContent(key - 1, 3);
                float altitude_float = altitude.floatValue();
                kernel.Kernel.getInstance().sendWaypoint(key.byteValue(), longitude, latitude, altitude_float);   
                Thread.sleep(500);
            }
            editedRows.clear();            
            userEditingWaypoints = false;
        } catch (InterruptedException ex){
            ex.printStackTrace();
        }
    }


    public void run() {

        System.out.println("Thread");
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ex) {
            Logger.getLogger(PanelMap.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (int i = -180; i < 180; i++) {
            setPoint(50*Math.sin(Math.toRadians(i)), i);
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Logger.getLogger(PanelMap.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGetImage = new javax.swing.JButton();
        textLat = new javax.swing.JTextField();
        textLon = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        comboBoxZoom = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        comboBoxMapType = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        panelDrawing = new des.map.PanelDrawing();
        checkBoxEnable = new javax.swing.JCheckBox();
        imageProgressBar = new javax.swing.JProgressBar();
        SaveImages = new javax.swing.JButton();
        txtZoomFrom = new javax.swing.JTextField();
        txtZoomTo = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblWaypoints = new javax.swing.JTable();
        cmdUpdateWaypoints = new javax.swing.JButton();

        setBackground(new java.awt.Color(254, 254, 254));

        buttonGetImage.setText("Get image");
        buttonGetImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonGetImageActionPerformed(evt);
            }
        });

        textLat.setText("44.727");

        textLon.setText("-93.076216");
        textLon.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                textLonActionPerformed(evt);
            }
        });

        jLabel1.setText("Latitude:");

        jLabel2.setText("Longitude:");

        comboBoxZoom.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20" }));
        comboBoxZoom.setSelectedIndex(16);

        jLabel3.setText("Zoom:");

        comboBoxMapType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Satellite", "Road", "Terrain", "Hybrid" }));

        jLabel4.setText("Map type:");

        panelDrawing.setPreferredSize(new java.awt.Dimension(256, 256));

        javax.swing.GroupLayout panelDrawingLayout = new javax.swing.GroupLayout(panelDrawing);
        panelDrawing.setLayout(panelDrawingLayout);
        panelDrawingLayout.setHorizontalGroup(
            panelDrawingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 640, Short.MAX_VALUE)
        );
        panelDrawingLayout.setVerticalGroup(
            panelDrawingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        checkBoxEnable.setText("Enable");
        checkBoxEnable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxEnableActionPerformed(evt);
            }
        });

        SaveImages.setText("Save Images");
        SaveImages.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveImagesActionPerformed(evt);
            }
        });

        txtZoomFrom.setText("14");

        txtZoomTo.setText("16");

        tblWaypoints.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "No", "Latitude", "Longitude", "Altitude"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblWaypoints.setColumnSelectionAllowed(true);
        tblWaypoints.getTableHeader().setReorderingAllowed(false);
        tblWaypoints.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tblWaypointsMouseReleased(evt);
            }
        });
        tblWaypoints.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tblWaypointsKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(tblWaypoints);
        tblWaypoints.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        cmdUpdateWaypoints.setText("Send Waypoints");
        cmdUpdateWaypoints.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdUpdateWaypointsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelDrawing, javax.swing.GroupLayout.PREFERRED_SIZE, 640, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(textLat)
                            .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(textLon, javax.swing.GroupLayout.DEFAULT_SIZE, 79, Short.MAX_VALUE)
                            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 79, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 59, Short.MAX_VALUE)
                            .addComponent(comboBoxZoom, 0, 59, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, 77, Short.MAX_VALUE)
                            .addComponent(comboBoxMapType, 0, 77, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(imageProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtZoomFrom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtZoomTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(SaveImages)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonGetImage, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(cmdUpdateWaypoints, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(checkBoxEnable))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 341, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(comboBoxZoom, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(imageProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(txtZoomFrom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtZoomTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(SaveImages, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(buttonGetImage, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(comboBoxMapType, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cmdUpdateWaypoints, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(checkBoxEnable)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(textLon, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(textLat, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(panelDrawing, javax.swing.GroupLayout.DEFAULT_SIZE, 458, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 458, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void textLonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_textLonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_textLonActionPerformed

    private void buttonGetImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonGetImageActionPerformed
        // TODO add your handling code here:
        updateMap();
    }//GEN-LAST:event_buttonGetImageActionPerformed

    private void checkBoxEnableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxEnableActionPerformed
        // TODO add your handling code here:
        enable=!enable;
    }//GEN-LAST:event_checkBoxEnableActionPerformed

    private void SaveImagesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveImagesActionPerformed
        int from;
        int to;
        try{
            from = Integer.parseInt(txtZoomFrom.getText());
            to = Integer.parseInt(txtZoomTo.getText());
        } catch (NumberFormatException ex){
            return;
        }        
        if(from > to){
            return;
        }  
        final int finalFrom = from;
        final int finalTo = to;
        SaveImages.setEnabled(false);
        Thread t = new Thread(new Runnable() {
            public void run() {
                panelDrawing.saveMapByMarkers("", finalFrom, finalTo);
            }
        });
        t.start();
    }//GEN-LAST:event_SaveImagesActionPerformed

    private void cmdUpdateWaypointsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdUpdateWaypointsActionPerformed
       sendWaypoints();
    }//GEN-LAST:event_cmdUpdateWaypointsActionPerformed

    private void tblWaypointsKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tblWaypointsKeyReleased
        synchronized (this){
            if(evt.getKeyCode() == 127){
                int row = tblWaypoints.getSelectedRow();
                DefaultTableModel model = (DefaultTableModel) tblWaypoints.getModel();
                model.setValueAt(0.0, row, 1);
                model.setValueAt(0.0, row, 2);
                model.setValueAt(0.0, row, 3);
                addEditedRow(row);
            }
        }
    }//GEN-LAST:event_tblWaypointsKeyReleased

    private void tblWaypointsMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblWaypointsMouseReleased
        synchronized(tblWaypoints){
            panelDrawing.setSelectedWaypoint(tblWaypoints.getSelectedRow());
            panelDrawing.enableWaypointChange();
        }
    }//GEN-LAST:event_tblWaypointsMouseReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton SaveImages;
    private javax.swing.JButton buttonGetImage;
    private javax.swing.JCheckBox checkBoxEnable;
    private javax.swing.JButton cmdUpdateWaypoints;
    private javax.swing.JComboBox comboBoxMapType;
    private javax.swing.JComboBox comboBoxZoom;
    private javax.swing.JProgressBar imageProgressBar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane1;
    private des.map.PanelDrawing panelDrawing;
    private javax.swing.JTable tblWaypoints;
    private javax.swing.JTextField textLat;
    private javax.swing.JTextField textLon;
    private javax.swing.JTextField txtZoomFrom;
    private javax.swing.JTextField txtZoomTo;
    // End of variables declaration//GEN-END:variables

    public void addPosition(double latitude, 
            double longitude) {

        if(enable){
            setPoint(latitude, longitude);           
        }
    }
    
    public void centerChanged() {      
        Locale l = new Locale("en");
        textLat.setText(String.format(l, "%.7f", panelDrawing.getLatitudeDeg()));
        textLon.setText(String.format(l, "%.7f", panelDrawing.getLongitudeDeg()));
    }

    public void zoomChanged() {
        comboBoxZoom.setSelectedIndex(panelDrawing.getZoom() - 1);
    }

    public void imageSaved(int currentZoom, int totalImagesAtThisZoom, int numSavedImagesAtThisZoom) {
        if(lastZoom != currentZoom){
            lastZoom  = currentZoom;
            imageProgressBar.setIndeterminate(false);
            imageProgressBar.setMinimum(0);
            imageProgressBar.setMaximum(totalImagesAtThisZoom);
            imageProgressBar.setString("Zoom " + currentZoom);
            imageProgressBar.setStringPainted(true);
        }
        imageProgressBar.setValue(numSavedImagesAtThisZoom);
    }

    public void allImagesSaved() {
        SaveImages.setEnabled(true);
        imageProgressBar.setValue(0);
        imageProgressBar.setString("");
    }
    
    private RedRenderer renderer = new RedRenderer();

    public synchronized void newWaypoint(double latitude, double longitude) {
        //change
        int row = tblWaypoints.getSelectedRow();
        if(row < 0){
            return;
        }
        DefaultTableModel model = (DefaultTableModel) tblWaypoints.getModel();
        model.setValueAt(latitude  * 180 / Math.PI , row, 1);
        model.setValueAt(longitude  * 180 / Math.PI , row, 2);
                       
        addEditedRow(row);
        
        /*DefaultTableModel model = (DefaultTableModel) tblWaypoints.getModel();
        int pos = model.getRowCount() + 1;
        renderer.setRowRed(pos-1);
        addRow(pos ,
            latitude*180.0 / Math.PI, longitude*180.0 / Math.PI, (double) 0);   
        waypointsToSend.put(pos, new Waypoint(latitude, longitude, 0)); */
    }
    
    public synchronized void setWaypointReply(data.Waypoint[] customWaypoints) { 
        if(userEditingWaypoints){
            return;
        }
        DefaultTableModel model = (DefaultTableModel) tblWaypoints.getModel();                
        for(int i = 0; i < customWaypoints.length; ++i){
            while(model.getRowCount() < customWaypoints[i].getID()){
                //add row
                int pos = model.getRowCount() + 1;                
                addRow(pos, 0.0, 0.0, 0.0);
            }            
            for(int j = 0; j < model.getRowCount(); ++j){
                Integer value = (Integer) model.getValueAt(j, 0);
                if(customWaypoints[i].getID() == value){
                    //update
                    boolean doUpdate = true;
                    for(Integer er : editedRows){
                        if(er == j){
                            doUpdate = false;
                            break;
                        }                        
                    }
                    if(doUpdate){                                                  
                        model.setValueAt(customWaypoints[i].getLongitude(), j, 1);
                        model.setValueAt(customWaypoints[i].getLatitude(), j, 2);
                        model.setValueAt(customWaypoints[i].getAltitude(), j, 3);    
                        double latitudeRad = customWaypoints[i].getLatitude() / 180.0 * Math.PI;
                        double longitudeRad = customWaypoints[i].getLongitude() / 180.0 * Math.PI;
                        panelDrawing.setWaypoint(customWaypoints[i].getID() - 1, 
                                longitudeRad, 
                                latitudeRad,
                                customWaypoints[i].getAltitude());
                    } 
                }
            }            
        }            
    }
    
    private synchronized void addRow(Integer id, Double Latitude, Double Longitude, Double Altitude){
        DefaultTableModel model = (DefaultTableModel) tblWaypoints.getModel();   
        model.addRow(new Object[]{id ,
                   Latitude, Longitude,  Altitude}); 
        
        CellEditor editor = tblWaypoints.getCellEditor(id - 1, 3);
        editor.addCellEditorListener(new CellEditorListener() {

            public synchronized void editingStopped(ChangeEvent e) {                                
                userEditingWaypoints = true;                                                   
            }

            public void editingCanceled(ChangeEvent e) {
                
            }
        });

        
        
    }
    
    class RedRenderer extends JLabel implements TableCellRenderer{                          

        public synchronized Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {                        
            if(editedRows.contains(row)) {            
                this.setForeground(Color.red);
            } else {
                this.setForeground(Color.black);
            }            
            if(isSelected || tblWaypoints.getSelectedRow() == row) {                                
                this.setForeground(Color.ORANGE);
            }
            this.setText(value.toString());
            return this;            
        }
        
    }
    
    
    
}
