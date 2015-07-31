package des.map;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * PanelDrawing.java
 *
 * Created on Aug 19, 2010, 11:45:04 AM
 */

/**
 *
 * @author david2010a
 */
public class PanelDrawing extends javax.swing.JPanel {

    /** Creates new form PanelDrawing */
    private BufferedImage image=null;
    private double[] wp_lat;
    private double[] wp_lng;
    private int wp_count;
    private double currentAltitude = 0;
    private double currentPhiRad = 0;
    private double currentThetaRad = 0;
    private double currentPsiRad = 0;
    private final String mapsFolderName = "maps";
    private boolean enableWaypointChange = false;
    private int selectedWaypoint = 0;
    
    public enum MapType{
        ROADMAP,
        SATELLITE,
        TERRAIN,
        HYBRID
    }
    
    
    private double centerLatitudeRad = 0;
    private double centerLongitudeRad = 0;       
    private int currentZoom = 0;
    private final int minZoom = 1; //UI must have 1
    private final int maxZoom = 20;
    private MapType mapKind = MapType.SATELLITE;
    private final ArrayList<Point2D> markers = new ArrayList<Point2D>(); // in rad
    private final ArrayList<ArrayList<Point2D> > paths = new ArrayList<ArrayList<Point2D>>();
    private int mapPositionX;
    private int mapPositionY;
    private final int googleMapsDefaultWidth = 256; // are for google Coord system transformation, not the image size!
    private final int googleMapsDefaultHeight = 256;  // are for google Coord system transformation, not the image size!
    
    private final ArrayList<PanelDrawingMapListener> centerChangedlisteners = new ArrayList<PanelDrawingMapListener>();
    
    public void enableWaypointChange(){
        enableWaypointChange = true;
    }
    
    public void disableWaypointChange(){
        enableWaypointChange = false;
    }
    
    public void setSelectedWaypoint(int waypoint){
        selectedWaypoint = waypoint;
    }
    
    public void setWaypoint(int waypoint, double latitudeRad, double longitudeRad, float altitude){
        synchronized(markers){
            while(waypoint >= markers.size()){
                markers.add(new Point2D.Double());
            }
            markers.get(waypoint).setLocation(latitudeRad, longitudeRad);      
        }
    }
    
    public void registerPanelDrawingMapListener(PanelDrawingMapListener listener){
        centerChangedlisteners.add(listener);
    }
    
    private synchronized void notifyCenterChangedListeners(){
        for(PanelDrawingMapListener listener : centerChangedlisteners){
            listener.centerChanged();
        }
    }
    
    private synchronized void notifyZoomChangedListeners(){
        for(PanelDrawingMapListener listener : centerChangedlisteners){
            listener.zoomChanged();
        }
    }
    
    private synchronized void notifyImageSavedChangedListeners(
            int currentZoom, 
            int totalImagesAtThisZoom, 
            int numSavedImagesAtThisZoom){
        for(PanelDrawingMapListener listener : centerChangedlisteners){
            listener.imageSaved(currentZoom, 
                    totalImagesAtThisZoom, numSavedImagesAtThisZoom);
        }
    }   
    
    private synchronized void notifyAllImagesSavedChangedListeners(){
        for(PanelDrawingMapListener listener : centerChangedlisteners){
            listener.allImagesSaved();
        }
    }
    
    private synchronized void notifyWaypointChangeListeners(double latitude, double longitude){
        for(PanelDrawingMapListener listener : centerChangedlisteners){
            listener.newWaypoint(latitude, longitude);
        }
    }
    
    public synchronized void setCenterLatitudeRad(double value){
        centerLatitudeRad = value;
        notifyCenterChangedListeners();
    }
    
    public synchronized void setCenterLongitudeRad(double value){
        centerLongitudeRad = value;
        notifyCenterChangedListeners();
    }
    
    public double getCenterLatitudeRad(){
        return centerLatitudeRad;
    }
    
    public double getCenterLongitudeRad(){
        return centerLongitudeRad;
    }  
    
    private double deg2rad(double x){
        return x / 180.0 * Math.PI;
    }
    
    private double rad2deg(double x){
        return x * 180 / Math.PI;
    }

    public PanelDrawing() {
        initComponents();  
        reset();
    }
        
    private Point2D getMercatorCenter(Point2D wgs){
        double loc_y = 0; // centerLongitude - centerLongitude     
        double loc_x = 0.5 * Math.log( 
                (1 + Math.sin(wgs.getX())) / 
                (1 - Math.sin(wgs.getX())));
        return new Point2D.Double(loc_x, loc_y);
    }
    
    //! calculate from current lat/lng setting the corresponding mercator coordinates
    private Point2D getMercatorCenter(){                
        return getMercatorCenter(new Point2D.Double(getCenterLatitudeRad(), getCenterLongitudeRad()));
    }
    
    private Point2D mercator2wgs(Point2D mercator, Point2D mercatorCenterRad){
        double latitude = 2 * Math.atan(Math.exp(mercator.getX())) - 0.5*Math.PI;
        double longitude = mercator.getY() + mercatorCenterRad.getY();
        return new Point2D.Double(latitude, longitude);
    }
    
    private Point2D mercator2wgs(Point2D mercator){
        return mercator2wgs(mercator, 
                new Point2D.Double(getCenterLatitudeRad(), getCenterLongitudeRad()));
    }
    
    private Point2D panel2ImageCoordinates(Point2D panelCoord){
        return new Point(
                getMapX((int) panelCoord.getX()), 
                getMapY((int) panelCoord.getY()));
    }
    
    // puts coordinates relative to the center of the image
    private Point2D image2CenteredImageCoordinates(Point2D imageCoord, Dimension mapSize){
        return new Point.Double(imageCoord.getX() - mapSize.getWidth()/2, 
                mapSize.getHeight()/2 - imageCoord.getY()); 
    }
    
    private Point2D centeredImage2MercatorCoordinates(Point2D centeredImageCoord, int zoom){
        final double zoom_scaling_div = Math.pow(2, zoom);                        
        final double scaling_lat = (deg2rad(180.0) / zoom_scaling_div) / (googleMapsDefaultWidth/2);
        final double scaling_lng = (deg2rad(180.0) / zoom_scaling_div) / (googleMapsDefaultHeight/2);
                
        //note that x,y is switched here
        return new Point.Double(
                centeredImageCoord.getY() * scaling_lat, 
                centeredImageCoord.getX() * scaling_lng);       
    }
       
    
    private Point2D obj2wgsCoord(Point2D coord){
        // X step conversion
        // 1. coord is in Panel Coordinates -> transform to Map Image Coordinates
        // 2. translate coordinates relative to the center of the map
        // 3. Map image coordinates are subject to the Google Zoom level
        //    -> Factor for that is 1/2^(zoomlevel)
        //    ->  Scale Coordinates to Mercator Scale        
        // 4. After the dx,dy coordinates are given, the new x_n, y_n 
        //    values can be estimated using wgs2objCoord on the current center
        //    by adding dx,dy
        // 5. with the new x_n and y_n coordinates, the latitude and longitude 
        //    can be calculated        
        Point2D mercatorCoords = centeredImage2MercatorCoordinates(image2CenteredImageCoordinates(panel2ImageCoordinates(coord), 
                new Dimension(getMapWidth(), getMapHeight())), getZoom());
        Point2D p_n = new Point2D.Double(
                getMercatorCenter().getX() + mercatorCoords.getX(), 
                getMercatorCenter().getY() + mercatorCoords.getY());
        return mercator2wgs(p_n);                
    }
    
    
    private Point2D wgs2mercator(Point2D wgs, Point2D latLngCenter){        
        //double x = Math.log(Math.tan(Math.PI/4.0 + (wgs.getX() - latLngCenter.getX()) / 2));        
        //double x = Math.log(Math.tan(0.5 * (wgs.getX() + 0.5 *Math.PI)));
        double x = Math.log(Math.abs(Math.tan(0.5 * (wgs.getX() + 0.5 *Math.PI))));
        double y = wgs.getY() - latLngCenter.getY();
        return new Point2D.Double(x,y);
    }
    
    private Point2D wgs2mercator(Point2D wgs){
        return wgs2mercator(wgs, new Point2D.Double(
            getCenterLatitudeRad(), getCenterLongitudeRad()));
    }
    
    private Point2D mercator2centeredImageCoord(Point2D mercator, int zoom){
        final double zoom_scaling_factor = Math.pow(2, zoom);
        final double scaling_lat_div = (deg2rad(180.0) / zoom_scaling_factor) / (googleMapsDefaultWidth/2);
        final double scaling_lng_div = (deg2rad(180.0) / zoom_scaling_factor) / (googleMapsDefaultHeight/2);
        double x = mercator.getX() / scaling_lat_div;
        double y = mercator.getY() / scaling_lng_div;
        return new Point.Double(y,x); //note x,y switch
    }
    
    private Point2D centeredImageCoord2ImageCoord(Point2D centerImageCoord, Dimension mapSize){
        return new Point.Double(centerImageCoord.getX() + mapSize.getWidth()/2, 
                mapSize.getHeight()/2 - centerImageCoord.getY()); 
    }
    
    private Point2D map2panelCoord(Point2D mapCoord){
        double x = mapCoord.getX() + this.getWidth() / 2 - image.getWidth() / 2;
        double y = mapCoord.getY() + this.getHeight() / 2 - image.getHeight() / 2;
        return new Point((int)x,(int)y);
    }
    
    private Point2D wgs2objCoord(Point2D wgs){
        Point2D mercator = wgs2mercator(wgs);        
        Point2D p_n = new Point2D.Double(
                mercator.getX() - getMercatorCenter().getX(), 
                mercator.getY() - getMercatorCenter().getY());
        Point2D centeredImageCoord = mercator2centeredImageCoord(p_n, getZoom());
        Point2D imageCoordinates = centeredImageCoord2ImageCoord(centeredImageCoord, 
                new Dimension(image.getWidth(), image.getHeight()));
        return map2panelCoord(imageCoordinates);
    }
    
    
    private BufferedImage loadMapByLatLng(double lat, double lng, int zoom, String pathPrefix){
        final String folderDir = pathPrefix + mapsFolderName;
        File folder = new File(folderDir);
        if(!folder.exists() || !folder.isDirectory()){
            return null;
        }
        double dLat = Double.MAX_VALUE;
        double dLng = Double.MAX_VALUE;
        double minLat = 0;
        double minLng = 0;
        int intLat = (int) (Math.pow(10,7) * lat);
        int intLng = (int) (Math.pow(10,7) * lng);
        File bestFit = null;
        for(File imgFile : folder.listFiles()){
            String fileName = imgFile.getName();
            String[] data = fileName.split("_");
            int imgZoom = Integer.parseInt(data[2].replace(".jpg", ""));
            if(imgZoom == zoom){                            
                int imgLat = Integer.parseInt(data[0]);
                int imgLng = Integer.parseInt(data[1]);                
                if(Math.abs(intLat - imgLat) <= dLat){
                    minLat = imgLat;
                    dLat = Math.abs(intLat - imgLat);
                    if(Math.abs(intLng - imgLng) <= dLng){
                        minLng = imgLng;
                        dLng = Math.abs(intLng - imgLng);
                        bestFit = imgFile;
                    }
                }                        
            }
        }
        if(bestFit != null){
            try {
                if(dLat / Math.pow(10,7) <
                        deg2rad(180.0) / Math.pow(2, zoom) || true){
                    if(dLng / Math.pow(10,7) <
                        deg2rad(180.0) / Math.pow(2, zoom) || true){
                                                
                        BufferedImage img = ImageIO.read(bestFit);
                        setCenterLatitudeRad(minLat * 1.0 / Math.pow(10, 7));
                        setCenterLongitudeRad(minLng * 1.0 / Math.pow(10, 7));
                        return img;
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(PanelDrawing.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //!TODO fix
        return null;
    }
    
    public synchronized void saveMapByMarkers(String pathPrefix, int zoomLevelStart, int zoomLevelEnd){
        if(markers.isEmpty()){
            notifyAllImagesSavedChangedListeners();
            return;
        }
        if(zoomLevelStart > zoomLevelStart){
            notifyAllImagesSavedChangedListeners();
            return;
        }
        
        double minLat = markers.get(0).getX();
        double maxLat = markers.get(0).getX();
        double minLng = markers.get(0).getY();
        double maxLng = markers.get(0).getY();

        
        for(Point2D marker : markers) {
            minLat = Math.min(minLat, marker.getX());
            maxLat = Math.max(maxLat, marker.getX());
            minLng = Math.min(minLng, marker.getY());
            maxLng = Math.max(maxLng, marker.getY());            
        }    
        
        if(minLat == maxLat || minLng == maxLng){
            notifyAllImagesSavedChangedListeners();
            return;
        }        
        
        final String folderDir = pathPrefix + mapsFolderName;
        File dir = new File(folderDir);
        dir.mkdir();
        
        int totalImagesAtThisZoom = 0;
        int imagesSavedAtThisZoom;
        
        for(int z = zoomLevelStart; z <= zoomLevelEnd; ++z){
            imagesSavedAtThisZoom = 0;
            double currentLat = minLat;
            double currentLng = minLng;
            double zoom_scaling_div = Math.pow(2, z-1);
            double scaling_lat = (deg2rad(180.0) / zoom_scaling_div) / (googleMapsDefaultHeight/2);
            double scaling_lng = (deg2rad(180.0) / zoom_scaling_div) / (googleMapsDefaultHeight/2);                    
            while(currentLat < maxLat){
                BufferedImage img = null;
                while(currentLng < maxLng){
                    img = getImage(currentLat, currentLng, z-1, false, false);   
                    if(img == null){
                        return;
                    }
                    currentLng += img.getWidth()/2 * scaling_lng;
                    
                    if(imagesSavedAtThisZoom == 0){
                        //calculate amount
                        totalImagesAtThisZoom = (int) 
                                (1 + (maxLng - minLng) / (img.getWidth()/2 * scaling_lng))
                                * (int)
                                (1 + (maxLat - minLat) / (img.getHeight()/2 * scaling_lat));
                    }
                    
                    String fileName = dir + "/" +
                        Integer.toString((int) (currentLat * Math.pow(10,7))) +
                        "_" + 
                        Integer.toString((int) (currentLng * Math.pow(10,7))) +
                        "_" + z + ".jpg";
                    try {
                        ImageIO.write(img, "jpg", new File(fileName));
                        imagesSavedAtThisZoom++;
                        notifyImageSavedChangedListeners(z, totalImagesAtThisZoom, imagesSavedAtThisZoom);
                    } catch (IOException ex) {
                        Logger.getLogger(PanelDrawing.class.getName()).log(Level.SEVERE, null, ex);
                        return;
                    }
                }
                currentLng = minLng;
                if(img == null){
                    return;
                }
                currentLat += img.getHeight()/2 * scaling_lat;                                                                                              
            }
        }      
        notifyAllImagesSavedChangedListeners();
    }

    private int getMapWidth(){
        if(image != null)
            return image.getWidth();
        return 0;            
    }
    
    private int getMapHeight(){
        if(image != null)
            return image.getHeight();
        return 0;    
    }
    
    private int getMapX(int panelX){
        return panelX - mapPositionX;
    }
    
    private int getMapY(int panelY){
        return panelY - mapPositionY;
    }
    
    public void setLongitudeDeg(double longitude){
        setCenterLongitudeRad(deg2rad(longitude));
    }
    
    public double getLongitudeDeg(){
        return rad2deg(getCenterLongitudeRad());
    }
    
    public void setLatitudeDeg(double latitude){
        setCenterLatitudeRad(deg2rad(latitude));
    }
    
    public double getLatitudeDeg(){
        return rad2deg(getCenterLatitudeRad());
    }        
    
    public void setPositionDeg(Point2D pos){        
        setCenterLatitudeRad(deg2rad(pos.getX()));
        setCenterLongitudeRad(deg2rad(pos.getY()));
    }
    
    public void setPositionRad(Point2D posRad){
        setCenterLatitudeRad(posRad.getX());
        setCenterLongitudeRad(posRad.getY());
    }
    
    private Point2D getPositionDeg(){
        return new Point2D.Double(
                rad2deg(getCenterLatitudeRad()), 
                rad2deg(getCenterLongitudeRad()));
    }
    
    public void setMapType(MapType mapKind){
        this.mapKind = mapKind;
    }
    
    public MapType getMapType(){
        return mapKind;
    }
    
    public void setZoom(int zoom){
        if(zoom >= minZoom && zoom <= maxZoom)
        {
            this.currentZoom = zoom;
            notifyZoomChangedListeners();
        }
    }
    
    public int getZoom(){
        return currentZoom;
    }
    
    private void drawMarkers(Graphics g){
        synchronized(markers){
            Graphics2D g2 = (Graphics2D) g;                 
            Color oldColor = g2.getColor();
            Font oldFont = g2.getFont();
            g2.setFont(oldFont.deriveFont(18f));
            //translate to 2d coordinates
            int wp_x[] = new int[markers.size()];
            int wp_y[] = new int[markers.size()];
            int i = 0;
            for(Point2D marker : markers){
                Point2D objPos = wgs2objCoord(marker);
                wp_x[i] = (int) objPos.getX();
                wp_y[i] = (int) objPos.getY();
                //draw the marker
                
                Ellipse2D el = new java.awt.geom.Ellipse2D.Double(
                        objPos.getX(), objPos.getY(), 10, 10);
                g2.setColor(Color.BLUE);
                g2.fill(el);
                g2.setColor(Color.WHITE);
                
                g2.drawString(Integer.toString(1+i), wp_x[i], wp_y[i]);
                i++;
            }
            g2.setColor(Color.BLUE);
            g2.drawPolyline(wp_x, wp_y, markers.size());
            //restore
            g2.setColor(oldColor);    
            g2.setFont(oldFont);
        }
    }
    
    private BufferedImage getImage(double centerLatitudeRad, double centerLongitudeRad, 
            int preferredWidth, int preferredHeight, int zoom, boolean print_markers, boolean print_paths){
        final String markerColor = "red";
        final String pathColor = "blue";
        int targetWidth = Math.min(640,Math.min(preferredWidth, getWidth())); //640
        int targetHeight = Math.min(458,Math.min(preferredHeight, getHeight())); //458    
        String stBase = "http://maps.googleapis.com/maps/api/staticmap?";
        stBase = stBase + "center=" + rad2deg(centerLatitudeRad) + "," + rad2deg(centerLongitudeRad);
        stBase = stBase + "&" + "zoom=" + zoom;
        //stBase = stBase + "&" + "size=" + this.getWidth() + "x" + this.getHeight();       
        stBase = stBase + "&" + "size=" + 
                Integer.toString(targetWidth) + "x" + Integer.toString(targetHeight);       
        stBase = stBase + "&" + "maptype=" + mapKind.toString().toLowerCase();
        stBase = stBase + "&" + "format=" + "jpg";
        char label = 65;
        if(print_markers){
            synchronized(markers){                
                for(Point2D marker : markers){            
                    stBase = stBase + "&" + "markers=color:" + markerColor + 
                            "%7C" + "label:" + label + "%7C" + rad2deg(marker.getX()) + "," + rad2deg(marker.getY());
                    //markers=color:blue%7Clabel:S%7C40.702147,-74.015794
                    label++;
                }
            }            
        }
        if(print_paths){
            for(ArrayList<Point2D> path : paths){
                stBase += "&path=color:" + pathColor + "%7Cweight:5";
                for(Point2D p : path){
                    stBase += "%7C" + rad2deg(p.getX()) + "," + rad2deg(p.getY());
                }
            }
        }
        stBase = stBase + "&" + "key=AIzaSyDa5zvhhjwAva-O70zLNJq53W4dNzWien0";
        BufferedImage img = null;
        try {                        
            URL urlMap = new URL(stBase);          
            img = ImageIO.read(urlMap);                                    
        } catch (MalformedURLException ex) {
            Logger.getLogger(PanelMap.class.getName()).log(Level.SEVERE, null, ex);
            kernel.Kernel.getInstance().joutln("Error getting map: " + ex.getMessage());
        } catch (IOException ex) {
            Logger.getLogger(PanelMap.class.getName()).log(Level.INFO, null, ex);
            kernel.Kernel.getInstance().joutln("Error getting map: " + ex.getMessage());
        }
        if(img == null){
            img = loadMapByLatLng(centerLatitudeRad, centerLongitudeRad, getZoom(), "");
        }
        return img;
    }
    
    private BufferedImage getImage(
            double centerLatitudeRad, 
            double centerLongitudeRad, 
            int zoom, 
            boolean markers,
            boolean paths){
        return getImage(centerLatitudeRad, centerLongitudeRad, 640, 458, zoom, markers, paths);
    }
    
    private BufferedImage getImage(boolean markers, boolean paths){
        return getImage(getCenterLatitudeRad(), getCenterLongitudeRad(), getZoom(), markers, paths);
    }
    
    private void getImage(){
        //image = getImage(true, true);        
        image = getImage(false, false);
    }           
    
    public void updateMap(){
        getImage();
        if (image != null) {
            repaint();
        }        
    }
    
    @Override
    public void paint(Graphics g) {
        super.paint(g);
                      
        Graphics2D g2=(Graphics2D)g;
        if(image!=null){
            //g2.drawImage(image, 0, 0, null);
            //g2.drawImage(image, 0, 0, getWidth(), getHeight(), null);       
            mapPositionX = (getWidth()-image.getWidth())/2;
            mapPositionY = (getHeight()-image.getHeight())/2;
            g2.drawImage(image, 
                    mapPositionX, 
                    mapPositionY, 
                    image.getWidth(), image.getHeight(), null);           
        }

        if( wp_lat != null && wp_lng != null){

            //System.out.println("drw lines: "+sizeXandY + " x: "+x[sizeXandY-1] + " y: "+y[sizeXandY-1]);
            g2.setStroke(new BasicStroke(2));
            
            g2.setColor(Color.red);
                      
            ArrayList<Integer> wp_xs = new ArrayList<Integer>();
            ArrayList<Integer> wp_ys = new ArrayList<Integer>();
            
            //coordinate transformation
            for(int i = 0; i < wp_count; ++i){                                
                Point2D img_c = wgs2objCoord(
                        new Point2D.Double(
                                wp_lat[i], 
                                wp_lng[i]));  
                int wp_x = (int)img_c.getX();
                int wp_y = (int)img_c.getY();
                //is on map?
                if(image != null){
                    if(wp_x >= this.getWidth()/2 - image.getWidth()/2 &&
                            wp_x <= this.getWidth()/2 + image.getWidth()/2){
                        // is on map
                        if(wp_y >= this.getHeight()/2 - image.getHeight()/2 &&
                            wp_y <= this.getHeight()/2 + image.getHeight()/2){
                            wp_xs.add(wp_x);
                            wp_ys.add(wp_y);
                        }
                    } 
                }
            }

            int wp_xs_array[] = new int[wp_xs.size()];
            int wp_ys_array[] = new int[wp_ys.size()];
            int i = 0;
            for(Integer x : wp_xs){
                wp_xs_array[i] = x;
                i++;
            }
            i=0;
            for(Integer y : wp_ys){
                wp_ys_array[i] = y;
                i++;
            }
            g2.drawPolyline(wp_xs_array, wp_ys_array, wp_xs.size());
            
            int currentImageX = 0;
            int currentImageY = 0;
            if(wp_xs_array.length > 1){
                currentImageX = wp_xs_array[wp_xs_array.length-1];
                currentImageY = wp_ys_array[wp_ys_array.length-1];
            }
            Ellipse2D el =new java.awt.geom.Ellipse2D.Double(currentImageX-5, currentImageY-5, 10, 10);
            g2.fill(el);
                                 
            
            g2.setColor(Color.white);            
            String status = "alt = " + String.format("%.2f", currentAltitude) + "\n" +
                    "phi = " + String.format("%.2f",rad2deg(currentPhiRad)) + " (deg)\n" +
                    "theta = " + String.format("%.2f",rad2deg(currentThetaRad)) + " (deg)\n" +
                    "psi = " + String.format("%.2f",rad2deg(currentPsiRad)) + " (deg)";
            final int lineOffset = g.getFontMetrics().getHeight();
            final int constOffsetX = (int) (Math.cos(currentPsiRad) * 10);
            final int constOffsetY = (int) (Math.sin(currentPsiRad) * 10);
            int curLineOffset = 0;
            for(String line : status.split("\n")){
                int printX = currentImageX + constOffsetX;
                int printY = currentImageY + constOffsetY + curLineOffset;
                curLineOffset += lineOffset;                
                g2.drawString(line, printX, printY);
            }
            
            
        }
        if(image != null)
        {
            drawMarkers(g);
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

        setBackground(new java.awt.Color(128, 156, 191));
        setPreferredSize(new java.awt.Dimension(429, 458));
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                formMouseDragged(evt);
            }
        });
        addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                formMouseWheelMoved(evt);
            }
        });
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                formMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                formMouseReleased(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 229, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void formMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseDragged
      
    }//GEN-LAST:event_formMouseDragged

    private void formMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMousePressed

    }//GEN-LAST:event_formMousePressed

    private void formMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseReleased
        if(evt.getButton() == java.awt.event.MouseEvent.BUTTON1){           
            setPositionRad(obj2wgsCoord(evt.getPoint()));
            updateMap();
        }
        if(evt.getButton() == java.awt.event.MouseEvent.BUTTON3){
            if(enableWaypointChange){                
                Point2D wgsTarget = obj2wgsCoord(evt.getPoint());

                if(paths.isEmpty()) { paths.add(new ArrayList<Point2D>()); }
                paths.get(0).add(wgsTarget);            
                setWaypoint(selectedWaypoint, wgsTarget.getX(), wgsTarget.getY(), 0);                
                notifyWaypointChangeListeners(wgsTarget.getX(), wgsTarget.getY());
                updateMap();
            }
        }        
    }//GEN-LAST:event_formMouseReleased

    private void formMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_formMouseWheelMoved
        if(evt.getWheelRotation() < 0)
        {
            setPositionRad(obj2wgsCoord(evt.getPoint()));
        }        
        setZoom(getZoom()-evt.getWheelRotation());
        updateMap();
    }//GEN-LAST:event_formMouseWheelMoved

    public void setPositions(double[] wp_lat, double [] wp_lng, int wp_count) {
        this.wp_lat = wp_lat;
        this.wp_lng = wp_lng;
        this.wp_count = wp_count;
        repaint();
    }
    
    public void setCurrentAltitude(double altitude){
        currentAltitude = altitude;
    }
    
    public void setCurrentPhi(double phiRad){
        currentPhiRad = phiRad;
    }
    
    public void setCurrentTheta(double thetaRad){
        currentThetaRad = thetaRad;
    }
    
    public void setCurrentPsi(double psiRad){
        currentPsiRad = psiRad;
    }
    
    public void setCurrentAttitude(double phiRad, double thetaRad, double psiRad){
        currentPhiRad = phiRad;
        currentThetaRad = thetaRad;
        currentPsiRad = psiRad;
    }

    public final void reset() {
        wp_lat = null;
        wp_lng = null;
        wp_count = 0;
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

}
