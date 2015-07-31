/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * PanelHUD.java
 *
 * Created on Jun 14, 2010, 4:06:03 PM
 */
package gui.hud;


import des.graph2d.BarDisplay;
import des.graph2d.Entity2D;
import des.graph2d.FlagDisplay;
import des.graph2d.GPSDisplay;
import des.graph2d.HeadingDisplay;
import des.graph2d.SlideDisplay;
import des.graph2d.SurfacesDisplay;
import des.graph2d.ThetaPhiDisplay;
import des.graph2d.TimeDisplay;
import gui.app.AppInterface;
import gui.app.PanelApp;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;

/**
 *
 * @author david2010a
 */
public class PanelHUD extends javax.swing.JPanel implements Runnable{

    /** Creates new form PanelHUD */
    private double angle = 0;
    private Graphics2D g2;
    private JFrame app;
    private ThetaPhiDisplay thetaPhiDisplay;
    private SlideDisplay altitudeDisplay;
    private HeadingDisplay headingDisplay;
    private SlideDisplay airSpeedDisplay;
    private SurfacesDisplay surfacesSlides;
    private FlagDisplay flagDisplay;
    private BarDisplay batteryBar;
    private BarDisplay cpuLoadBar;
    private GPSDisplay gpsDisplay;
    private TimeDisplay timeDisplay;
    private int center_x = 0;
    private int center_y = 0;
    private boolean isLineWidthLarge = true;
    private double lastOffsetDeg = 0;
    private boolean isLineWidthLarge2 = true;
    private double lastOffsetDeg2 = 0;
    private boolean testFlag = false;
    private double airSpeed = 0;
    private double airSpeedRef = 0;
    private int modes = 0;
    
    private double time = 0;
    private double theta = 0;
    private double phi = 0;
    private double yaw = 0;
    private double altitude = 0;
    private double longitude = 0;
    private double latitude = 0;
    private int gpsSatellites;
    private double batteryLevel = 0;
    private double cpuLoad = 0;
    private double aileron = 0;
    private double elevetor = 0;
    private double throttle = 0;
    private double rudder = 0;
    private double altitudeRef = 0;
    private boolean init=false;
    private AppInterface parent;


//    Thread t1=null;
//    int countYaw=0;
//    private boolean switchYaw;

    public PanelHUD()  {

        initComponents();
        setBackground(new java.awt.Color(73, 68, 103));

        center_x = 600;
        center_y = 350;

        thetaPhiDisplay = new ThetaPhiDisplay(center_x, center_y, 52, 5, 5);

        altitudeDisplay = new SlideDisplay(center_x + 310, center_y, 40, 40, 10, 5, false);

        airSpeedDisplay = new SlideDisplay(center_x - 310, center_y, 40, 40, 10, 5, true);

        String[] stNames = {"", "AHD", "IHD", "WPN", "MAN", "", "IMU DATA", "GPS DATA", "GPS LOCK", "TELE DATA"};
        flagDisplay = new FlagDisplay(0, center_y - 170, 80, 30, 5, 2, stNames);

        surfacesSlides = new SurfacesDisplay(120, center_y + 70, 150, 15,-25,25);

        headingDisplay = new HeadingDisplay(center_x, 50, 40, 60, 10, 5);

        batteryBar = new BarDisplay("Battery", 20, 40, 200, 20, false);

        cpuLoadBar = new BarDisplay("CPU load", 20, 120, 200, 20, false);

        gpsDisplay = new GPSDisplay(center_x - 170, center_y + 170, 250, 50, 2);

        timeDisplay = new TimeDisplay(center_x + 100, center_y + 170 + 25, 4);

        repaint();

        //Thread t = new Thread(this);
        //t.start();

        //System.out.println("mod: " +(-15 % 10));
       
              
    }



    public void setParent(JFrame _app) {
        app = _app;
    }

    public void setParent(AppInterface app){
        parent=app;
    }

    public void draw(Graphics2D g2) {

        //g2.setStroke(new BasicStroke(10));
        double[] values = new double[3];
        values[0] = theta;
        values[1] = phi;
        thetaPhiDisplay.update(values, g2);

        values[0] = altitude;

        altitudeDisplay.update(values, g2);

        values[0] = yaw;
        headingDisplay.update(values, g2);

        values[0] = airSpeed;
        values[1] = airSpeedRef;
        airSpeedDisplay.update(values, g2);

        double[] surfaces = {aileron, rudder, throttle, elevetor};
        surfacesSlides.update(surfaces, g2);


        values[0] = modes;
        flagDisplay.update(values, g2);

        values[0] = batteryLevel;
        batteryBar.update(values, g2);

        values[0] = cpuLoad;
        cpuLoadBar.update(values, g2);

        values[0] = latitude;
        values[1] = longitude;
        values[2] = gpsSatellites;
        gpsDisplay.update(values, g2);

        values[0] = time;
        timeDisplay.update(values, g2);

       

    }

    public void draw() {

        //g2.setStroke(new BasicStroke(10));


        draw(g2);

    }

    public void paint(Graphics g) {

        super.paint(g);
        g2 = (Graphics2D) g;

        draw(g2);
//        if(t1==null){
//            t1 =new Thread(this);
//        t1.start();
//        }


    }

    public void setAirSpeed(double iAS, double ias_ref) {
        airSpeed = iAS;
        airSpeedRef = ias_ref;
    }

    public void setMode(int mode) {
        modes = mode;
    }

    public void setTime(double time_) {
        time = time_;
    }

    public void setPitchAndBank(double pitch, double bank) {
        theta = pitch;
        phi = bank;
    }

    public void setHeading(double heading) {
        yaw = heading;
    }

    public void setAltitude(double altitude_, double altitudeRef_) {
        altitude = altitude_;
        altitudeRef = altitudeRef_;
    }

    public void setLongitude(double longitude_) {
        longitude = longitude_;
    }

    public void setLatitude(double latitude_) {
        latitude = latitude_;
    }

    public void setBatteryLevel(double batteryLevel_) {
        batteryLevel = batteryLevel_;
    }

    public void setCPULoad(double CPULoad) {
        cpuLoad = CPULoad;
    }

    public void setAileron(double aileron_) {
        aileron = aileron_;
    }

    public void setElevetor(double elevetor_) {
        elevetor = elevetor_;
    }

    public void setThrottle(double throttle_) {
        throttle = throttle_;
    }

    public void setRudder(double rudder_) {
        rudder = rudder_;
    }


    public void run() {
        int count =80;
        try {
                Thread.sleep(1200);
            } catch (InterruptedException ex) {
                Logger.getLogger(HeadingDisplay.class.getName()).log(Level.SEVERE, null, ex);
            }
        while (true){
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Logger.getLogger(HeadingDisplay.class.getName()).log(Level.SEVERE, null, ex);
            }

            setAirSpeed(count, 100);
            count--;

           
            repaint();
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

        setBackground(new java.awt.Color(87, 91, 134));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 878, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 672, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    public void setRefAltitude(double altRef) {
        altitudeRef = altRef;
    }

    public void setNumberSatellites(int nSatellites) {
        gpsSatellites = nSatellites;
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
