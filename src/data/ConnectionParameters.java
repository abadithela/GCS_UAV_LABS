/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package data;

import java.io.Serializable;

/**
 *
 * @author david2010a
 */
public class ConnectionParameters implements Serializable{

    private String portID;
    private String parity;
    private String baudRate;
    private String flowControlIn;
    private String flowControlOut;
    private String dataBits;
    private String stopBits;
    private int connectionType;
    private int portDecNumber; 

    /// Definition of constants:
    // Add more if there is a new protocol / port type
    public static final int UDP = 0;
    public static final int SERIAL_TTY = 1;
    public static final int MATLAB = 2;


    public ConnectionParameters(){
        portID="/dev/ttyS0";
        parity="";
        baudRate="";
        flowControlIn="";
        flowControlOut="";
        dataBits="";
        stopBits="";
        connectionType=UDP;        
        portDecNumber=0;


    }

    public int getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(int connectionType) {
        this.connectionType = connectionType;
    }
    
    public String getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(String baudRate) {
        this.baudRate = baudRate;
    }

    public String getDataBits() {
        return dataBits;
    }

    public void setDataBits(String dataBits) {
        this.dataBits = dataBits;
    }

    public String getFlowControlIn() {
        return flowControlIn;
    }



    public String getParity() {
        return parity;
    }

    public void setParity(String parity) {
        this.parity = parity;
    }

    public String getPortID() {
        return portID;
    }

    public void setPortID(String portID) {
        this.portID = portID;
    }

    public String getStopBits() {
        return stopBits;
    }

    public void setStopBits(String stopBits) {
        this.stopBits = stopBits;
    }

    public int getPortDecNumber() {
        return portDecNumber;
    }

    public void setPortDecNumber(int portDecNumber) {
        this.portDecNumber = portDecNumber;
    }

    public void setFlowControlIn(String flowControlIn) {
        this.flowControlIn = flowControlIn;
    }

    public void setFlowControlOut(String flowControlOut) {
        this.flowControlOut = flowControlOut;
    }

    public String getFlowControlOut() {
        return flowControlOut;
    }


    
    

}

//serialManager = new SerialManager(this, gui.getSerialPort(), gui.getRate(), gui.getFlowControl()[0], gui.getFlowControl()[1], gui.getDataBits(), gui.getStopBits(), gui.getParidad());