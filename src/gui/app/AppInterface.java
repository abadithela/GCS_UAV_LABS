/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.app;

import data.ConnectionParameters;
import data.Waypoint;
import java.util.Vector;

/**
 *
 * @author David Escobar Sanabria
 */
public interface AppInterface {

    /**
     *
     * @return
     */
    public String getUDPPort();

    /**
     *
     * @return
     */
    public String getRate();

    /**
     *
     * @return
     */
    public String[] getFlowControl();

    /**
     *
     * @return
     */
    public String getDataBits();

    /**
     *
     * @return
     */
    public String getStopBits();

    /**
     *
     * @return
     */
    public String getParity();
    /**
     *
     * @param message
     */
    public void joutln(String message);

    /**
     *
     * @param message
     * @param error
     */
    public void joutln(String message, boolean error);

    /**
     *
     * @return
     */
    public boolean isUDP();

    /**
     *
     * @return
     */
    public String getSerialPort();

    /**
     *
     * @param b
     */
    public void enableSerial(boolean b);

    /**
     *
     * @param GPS
     */
   

    /**
     *
     * @param airSpeed
     */
    public void setAirSpeed(double airSpeed);


    /**
     * 
     * @param heading
     */
    public void setHeading(double heading);

    /**
     *
     * @param pitch
     * @param bank
     */
    public void setPitchAndBank(double pitch, double bank);

    /**
     *
     * @param time
     */
    public void setTime(double time);

    /**
     *
     * @param mode
     */
    public void setMode(int mode);

    /**
     *
     */
    public void repaint();

    /**
     *
     */
    public void updateGUI();

    /**
     *
     * @param iAS
     * @param ias_ref
     */
    public void setAirSpeed(double iAS, double ias_ref);

   

    public void setBattery(double batteryLevel);

    public void setCPULoad(double CPULoad);

    public void setAileron(double aileron);
    public void setElevetor(double elevetor);
    public void setThrottle(double throttle);
    public void setRudder(double rudder);

    public void setGPS(double altitude, double longitude, double latitude, int nSatellites);

    public void setInterface(int interf);

    public void sendDataToTerminal(char c);

    public void sendDataToPort(char data, int method);

    public void sendDataToPort(String data, int method);

    public void sendDataToTerminal(String buffer);

    public void drawGUI();

    public void setRefAltitude(double altRef);

    public Vector getListOfCommands();

    public void setListOfCommands(Vector vectorCommands);

    public void setConnectionParameters(ConnectionParameters connectionParemeters);

    public void setCustomData(double[] values, double time);
    
    public void setCustomParameters(double[] params);

    public void setCustomWaypoints(Waypoint[] customWaypoints);

    public void setConnectionLost();

    public void resetConnectionLost();
    
}
