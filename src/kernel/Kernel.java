/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor. ss
 */
package kernel;

import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLDouble;
import comm.InternetAdapter;
import comm.ChannelInterface;
import comm.SerialManager;
import data.DataPacket;
import data.DataSettings;
import data.ConnectionParameters;
import data.ParamPacket;
import data.WaypointPacket;
import gui.app.AppInterface;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import kml.KMLManager;
import net.udp.server.ClientToMatlab;
import net.udp.server.UDPServer;
import sun.security.util.SecurityConstants;

/**
 *
 * @author David Escobar Sanabria
 */
public class Kernel extends Thread implements ChannelInterface, KernelInterface {

    private UDPServer udpServer = null;
    private ClientToMatlab client = null;
    private SerialManager serialManager = null;
    private DataPacket dataPacket = null;
    private Vector vectorData = null;
    AppInterface gui;
    private int countPackets = 0;
    private int countPacketsCheck = 0;
    private String tempFile = null;
    private boolean runFlag = true;
    private int dataPeriod = 1000;
    private KMLManager kmlManager;
    private InternetAdapter cpsserver;
    private long disconnectedWatchdogMs = 0;
    private final long disconnectionTimeoutMs = 2500;
    private boolean disconnectedWatchdogIsActive = false;
    
    /**
     *
     */
    public static final int SERIAL_PORT = 0;
    /**
     *
     */
    public static final int UDP_PORT = 1;
    public static final int MATLAB_PORT = 2;
    private static final String defaultFile = "default.data";
    private DataSettings dataSettings;
    private ConnectionParameters connectionParameters;
    private boolean forwardToServer = false;
    
    
    private static Kernel kernel;
    
    public static Kernel getInstance(){
        return kernel;
    }
    
    public static void setInstance(Kernel kernel){
        Kernel.kernel = kernel;
    }

    /**
     * Constructor of class Kernel Hi austin
     * @param _gui
     */
    public Kernel(AppInterface _gui) {

        gui = _gui;
        try {
            cpsserver = new InternetAdapter();
        } catch (UnknownHostException ex) {
            Logger.getLogger(Kernel.class.getName()).log(Level.SEVERE, null, ex);
        }
        connectionParameters = new ConnectionParameters();
        dataPacket = new DataPacket();
        reconnect(gui.isUDP());
        System.out.println("Software developed by David Escobar Sanabria, Christian and Apurva");
        System.out.println("UAV research group, Aerospace Engineering and Mechanics, University of Minnesota");
        //InputStream io=new InputStream();
        tempFile = "tempData.data";
        deleteFile(tempFile);

        dataSettings = (DataSettings) openObjectFile(defaultFile);
        if (dataSettings != null) {
            loadDefaultSettingsToGUI(dataSettings);
        } else {

            dataSettings = new DataSettings();
        }


        //((uint16_t)(((uint16_t)getub(buf, (off)) << 8) | (uint16_t)getub(buf, (off)+1)));

        start();

    }

    /**
     * 
     */
    public void run() {
        while (runFlag) {
            try {
                Thread.sleep(dataPeriod + 200);
            } catch (InterruptedException ex) {
                Logger.getLogger(Kernel.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (countPackets == countPacketsCheck) {
                gui.setMode((int) (0x0000));
            }

            countPacketsCheck = countPackets;
            if(checkPacketTimeout()){
                gui.setConnectionLost();
            } else {                        
                gui.resetConnectionLost();
            }

        }
    }

    public void joutln(String st, boolean b) {
        gui.joutln(st, b);
    }

    public void joutln(String st) {
        gui.joutln(st);
    }

    public void sendSerial(String st) {
        if (serialManager != null) {
            serialManager.sendData(st);
        }

    }

    public void setDataIn(String st) {

        //dataPacket.setDataString(st);

        joutln(st);

    }

    /**
     *
     * @param isUDP
     */
    public void reconnect(boolean isUDP) {

        try {
            udpServer.closeConnection();
            udpServer.stop();
            udpServer = null;

        } catch (Exception e) {
        }

        try {
            serialManager.closeConnection();
            serialManager.stop();
            serialManager = null;

        } catch (Exception e) {
        }


        if (isUDP) {

            try {
                connectionParameters.setConnectionType(ConnectionParameters.UDP);
                connectionParameters.setPortDecNumber(Integer.parseInt(gui.getUDPPort()));
                udpServer = new UDPServer(Integer.parseInt(gui.getUDPPort()), this);
                activateConnectionLostWatchdog();            
            } catch (SocketException ex) {
                Logger.getLogger(Kernel.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {

            connectionParameters.setConnectionType(ConnectionParameters.SERIAL_TTY);
            connectionParameters.setBaudRate(gui.getRate());
            connectionParameters.setPortID(gui.getSerialPort());
            connectionParameters.setFlowControlIn(gui.getFlowControl()[0]);
            connectionParameters.setFlowControlOut(gui.getFlowControl()[1]);
            connectionParameters.setDataBits(gui.getDataBits());
            connectionParameters.setStopBits(gui.getStopBits());

            // serialManager = new SerialManager(this, gui.getSerialPort(), gui.getRate(), gui.getFlowControl()[0], gui.getFlowControl()[1], gui.getDataBits(), gui.getStopBits(), gui.getParity());
            serialManager = new SerialManager(this, connectionParameters);
            activateConnectionLostWatchdog();

        }

    }

    
    public void reconnectMatlab(boolean isUDP){

        try {
            client.closeConnection();
         
            client = null;

        } catch (Exception e) {
        }

        try {
            serialManager.closeConnection();
            serialManager.stop();
            serialManager = null;

        } catch (Exception e) {
        }


        if (isUDP) {

            try {
                connectionParameters.setConnectionType(ConnectionParameters.MATLAB);
                connectionParameters.setPortDecNumber(Integer.parseInt(gui.getUDPPort()));
                client = new ClientToMatlab(Integer.parseInt(gui.getUDPPort()), this);
                activateConnectionLostWatchdog();            
            } catch (SocketException ex) {
                Logger.getLogger(Kernel.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {

            connectionParameters.setConnectionType(ConnectionParameters.SERIAL_TTY);
            connectionParameters.setBaudRate(gui.getRate());
            connectionParameters.setPortID(gui.getSerialPort());
            connectionParameters.setFlowControlIn(gui.getFlowControl()[0]);
            connectionParameters.setFlowControlOut(gui.getFlowControl()[1]);
            connectionParameters.setDataBits(gui.getDataBits());
            connectionParameters.setStopBits(gui.getStopBits());

            // serialManager = new SerialManager(this, gui.getSerialPort(), gui.getRate(), gui.getFlowControl()[0], gui.getFlowControl()[1], gui.getDataBits(), gui.getStopBits(), gui.getParity());
            serialManager = new SerialManager(this, connectionParameters);
            activateConnectionLostWatchdog();

        }

    }

    /**
     *
     * @param isUDP
     * @param channelInterface
     */
   
    
    

    public void setSerialPortAvaliable(boolean b) {
        gui.enableSerial(b);

    }

    public void setDataIn(int[] intArray) {
        dataPacket.setDataIntArray(intArray);
        
        countPackets++;
        
        if (dataPacket.isCheckSum()) {

            if (vectorData == null) {
                vectorData = new Vector(100);
            }
            vectorData.add(dataPacket.getDataArray().clone());
            if (vectorData.size() == 5000) {
                //System.out.println("Packets: "+vectorData.size());
                saveVectorData();
            }
            //System.out.println("Packets: "+countPackets);
            try {

                gui.setAirSpeed(dataPacket.getIAS(), dataPacket.getIas_ref());
                //gui.setAirSpeed(dataPacket.getTime(), dataPacket.getIas_ref());


                gui.setHeading(dataPacket.getPsi());

                gui.setPitchAndBank(dataPacket.getTheta(), -1 * dataPacket.getPhi());
                //gui.setPitchAndBank(1*dataPacket.getTime(), -1*dataPacket.getTime());


                gui.setTime(dataPacket.getTime());
                //int auxMode= ( ~( ((int)dataPacket.getFlight_mode()) | 0xFE3F)) | ( ((int)dataPacket.getFlight_mode()) & 0xFE3F ) ;
                int auxMode = (int) dataPacket.getFlight_mode();
                //gui.setMode((int) (auxMode));
                gui.setMode((int) (0x0200 | auxMode));
                //gui.setBattery(dataPa);
                gui.setCPULoad(dataPacket.getCpuload());

                gui.setGPS(dataPacket.getAltitude(), dataPacket.getLongitud(), dataPacket.getLatitud(), (int) dataPacket.getGpsSatellites());
                //gui.setAltLonLat(-dataPacket.getTime(),dataPacket.getLongitud(),dataPacket.getLatitud());

                gui.setRefAltitude(dataPacket.getAltRef());
                gui.setAileron(dataPacket.getAileron());
                gui.setElevetor(dataPacket.getElevator());
                gui.setThrottle(100 * dataPacket.getThrottle());
                gui.setRudder(dataPacket.getRudder());

                gui.setCustomData(dataPacket.getCustomData(), dataPacket.getTime());
                gui.setCustomParameters(dataPacket.getCustomParameters());
                gui.setCustomWaypoints(dataPacket.getCustomWaypoints());
                //gui.drawGUI();
                //System.out.println("painting GUI: "+dataPacket.getTime());         
                resetDisconnectedWatchdog();
                
                if(forwardToServer){
                    cpsserver.send(dataPacket.getDataIntArray());
                }

            } catch (IOException e) {
                System.out.println("Problems while painting GUI");
            }

        } else {

            if (dataPacket.isMessagaAvaliable()) {
                joutln(dataPacket.getMessage());
            } else {
                gui.setMode((int) (0xFDFF & (int) dataPacket.getFlight_mode()));
                resetDisconnectedWatchdog();
                System.out.println("Data checksum Failed");
            }


        }

    }

    public void sendParameter(byte paramId, float paramValue){
        ParamPacket packet = new ParamPacket();        
        packet.assembleParameterPacket(paramId, paramValue);
        if(gui.isUDP()){
            udpServer.sendData(packet.getPacketData());
            client.sendDatatoMatlab(packet.getPacketData());
        } else
        {
            serialManager.sendData(packet.getPacketData());
        }
    }        
    
    public void sendWaypoint(byte waypointId, int longitude, int latitude, float altitude){
        WaypointPacket packet = new WaypointPacket();
        packet.assembleWaypointPacket(waypointId, longitude, latitude, altitude);
        if(gui.isUDP()){
            udpServer.sendData(packet.getPacketData());
        } else
        {
            serialManager.sendData(packet.getPacketData());
        }
    }
    
    /**
     *
     */
    public void quit() {
        try {
            serialManager.closeConnection();
            serialManager.stop();
        } catch (Exception e) {
            System.out.println("It is not possible to close the serial connection");
        }

        try {
            udpServer.closeConnection();
            client.closeConnection();
        } catch (Exception e) {
            System.out.println("It is not possible to close the UDP connection");
        }

        connectionParameters.setConnectionType(ConnectionParameters.SERIAL_TTY);
        connectionParameters.setBaudRate(gui.getRate());
        connectionParameters.setPortID(gui.getSerialPort());
        connectionParameters.setFlowControlIn(gui.getFlowControl()[0]);
        connectionParameters.setFlowControlOut(gui.getFlowControl()[1]);
        connectionParameters.setDataBits(gui.getDataBits());
        connectionParameters.setStopBits(gui.getStopBits());


        dataSettings.setConnectionParemeters(connectionParameters);
        dataSettings.setVectorCommands(gui.getListOfCommands());
        saveDefaultSettings(dataSettings);


    }

    private void saveVectorData() {

        Vector tempVector = (Vector) openObjectFile(tempFile);
        if (tempVector != null) {
            tempVector.addAll(vectorData);
            //System.out.println("Total size= "+tempVector.size());
        } else {
            tempVector = vectorData;
        }
        saveObjectFile(tempFile, tempVector);
        vectorData.removeAllElements();

    }

    /**
     *
     * @param path
     */
    public void deleteFile(String path) {
        File file = new File(path);
        try {
            file.delete();
        } catch (Exception e) {
            //System.out.println("Is not possible to delete temporal data");
        }

    }

    /**
     *
     * @param path
     * @param stData
     */
    public void saveFile(String path, String stData) {
        File file = new File(path);
        FileWriter fw = null;
        try {
            fw = new FileWriter(file);
        } catch (IOException ex) {
            Logger.getLogger(Kernel.class.getName()).log(Level.SEVERE, null, ex);
        }
        BufferedWriter textOut = new BufferedWriter(fw);
        try {
            textOut.write(stData);
        } catch (IOException ex) {
            Logger.getLogger(Kernel.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            textOut.close();
        } catch (IOException ex) {
            Logger.getLogger(Kernel.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     *
     * @param path
     * @param obj
     */
    public void saveObjectFile(String path, Object obj) {
        File file = new File(path);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException ex) {
        }
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(fos);
        } catch (IOException ex) {
        }
        try {
            oos.writeObject(obj);
            //System.out.println("Data saved");
        } catch (IOException ex) {
        }

    }

    /**
     *
     * @param path
     * @return
     */
    public Object openObjectFile(String path) {

        Object returnObject = null;

        File file = new File(path);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (Exception e) {
        }

        ObjectInputStream ois = null;

        if (fis != null) {
            try {
                ois = new ObjectInputStream(fis);
            } catch (IOException ex) {
            }
        }

        try {
            returnObject = ois.readObject();
        } catch (Exception e) {
            System.out.println("Data settings error opening");
        }

        return returnObject;
    }

    /**
     *
     * @param file
     */
    public void genMatFile(File file) {

        String MfilePath = file.getPath();
        String MfileName = file.getName();
        if(!MfilePath.endsWith(".mat"))
        {
            MfilePath += ".mat";
            if(MfileName.equals(""))
            {
                MfileName = "export";
            }
            MfileName += ".mat";
        }
        Vector vData = (Vector) openObjectFile(tempFile);

        if (vData != null) {
            vData.addAll(vectorData);
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Logger.getLogger(Kernel.class.getName()).log(Level.SEVERE, null, ex);
            }
            double[][] matrix = getMatrix(vData);
            MLDouble mlDouble = new MLDouble(MfileName.substring(0, MfileName.length() - 4), matrix);
            ArrayList list = new ArrayList();
            list.add(mlDouble);
            try {
                new MatFileWriter(MfilePath, list);
                //gui.setPitchAndBank(-40.7894, 270);
            } catch (IOException ex) {
                Logger.getLogger(Kernel.class.getName()).log(Level.SEVERE, null, ex);
            }
            joutln("Data saved sucessfully");
        } else {
            if (vectorData != null) {
                if (vectorData.size() > 0) {
                    vData = vectorData;
                    double[][] matrix = getMatrix(vData);
                    MLDouble mlDouble = new MLDouble(MfileName.substring(0, MfileName.length() - 4), matrix);
                    ArrayList list = new ArrayList();
                    list.add(mlDouble);
                    try {
                        new MatFileWriter(MfilePath, list);
                        //gui.setPitchAndBank(-40.7894, 270);
                    } catch (IOException ex) {
                        Logger.getLogger(Kernel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    joutln("Data saved sucessfully");
                }



            } else {
                joutln("No Data avaliable", true);
            }
        }

    }

    /**
     *
     * @param vData
     * @return
     */
    public double[][] getMatrix(Vector vData) {

        double[][] matrix = new double[vData.size()][((double[]) vData.elementAt(0)).length];
        for (int i = 0; i < vData.size(); i++) {

            matrix[i] = (double[]) vData.elementAt(i);

        }
        return matrix;
    }

    /**
     *
     * @param MfilePath
     */
    public void genMFile(final String MfilePath) {
        Vector vData = (Vector) openObjectFile(tempFile);

        if (vData != null) {
            vData.addAll(vectorData);
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Logger.getLogger(Kernel.class.getName()).log(Level.SEVERE, null, ex);
            }
            String stMFile = getStringMFile(vData);
            saveFile(MfilePath, stMFile);
            joutln("Data saved sucessfully");
        } else {
            if (vectorData != null) {
                if (vectorData.size() > 0) {
                    vData = vectorData;
                    String stMFile = getStringMFile(vData);
                    saveFile(MfilePath, stMFile);
                    joutln("Data saved sucessfully");
                }



            } else {
                joutln("No Data avaliable", true);
            }
        }
    }

    /**
     *
     * @param vData
     * @return
     */
    public String getStringMFile(Vector vData) {
        String mFile = "data=[\n";
        for (int i = 0; i < vData.size(); i++) {
            double[] array = null;
            try {
                array = (double[]) vData.elementAt(i);
            } catch (Exception e) {
            }

            if (array != null) {
                for (int j = 0; j < array.length; j++) {
                    mFile = mFile + "" + array[j] + " ";

                }
                mFile = mFile + "\n";
            }

        }
        mFile = mFile + "]";
        return mFile;
    }

    /**
     *
     * @param file
     * @param period
     */
    public void setKMLPath(File file, int period) {
        System.out.println("KML ok");
        kmlManager = null;
        kmlManager = new KMLManager(this, file, period);

    }

    /**
     *
     * @return
     */
    public double getLongitud() {
        return dataPacket.getLongitud();
    }

    /**
     *
     * @return
     */
    public double getLatitud() {
        return dataPacket.getLatitud();
    }

    /**
     *
     * @return
     */
    public double getAltitude() {
        return dataPacket.getAltitude();
    }

    /**
     *
     */
    public void kmlStop() {
        kmlManager.setBreak(true);
        kmlManager.stop();
    }

    /**
     *
     * @param inter
     */
    public void setInterface(int inter) {
        if (serialManager != null) {
            serialManager.setInterface(inter);
        }

    }

    /**
     *
     * @param c
     */
    public void sendDataToTerminal(char c) {
        gui.sendDataToTerminal(c);
    }

    /**
     *
     * @param data
     * @param method
     */
    public void sendDataToPort(char data, int method) {
        switch (method) {
            case SERIAL_PORT:

                sendSerial(data);

                break;


            case UDP_PORT:

                break;
        }
    }

    private void sendSerial(char data) {
        serialManager.sendData("" + data);

    }

    /**
     *
     * @param data
     * @param method
     */
    public void sendDataToPort(String data, int method) {
        switch (method) {
            case SERIAL_PORT:

                sendSerial(data);

                break;


            case UDP_PORT:

                break;
        }
    }

    /**
     *
     * @param buffer
     */
    public void sendDataToTerminal(String buffer) {
        gui.sendDataToTerminal(buffer);
    }

    private void loadDefaultSettingsToGUI(DataSettings dataSettings_) {


        gui.setListOfCommands(dataSettings_.getVectorCommands());
        gui.setConnectionParameters(dataSettings_.getConnectionParemeters());


    }

    private void saveDefaultSettings(DataSettings dataSettings_) {

        saveObjectFile(defaultFile, dataSettings_);
        //System.out.println("Data settings saved :" + dataSettings_.getConnectionParemeters().getPortID());

    }

    public void forwardToServer(boolean selected) {
        if(selected){
            if(forwardToServer){
                //do nothing
            } else {
                forwardToServer = true;
                cpsserver.activate();
                //create socket
            }
        } else
        {
            if(forwardToServer){
                forwardToServer = false;
                cpsserver.deactivate();
                //close socket
            } 
        }
    }
                
    public void startDisconnectedWatchdog() {
        disconnectedWatchdogIsActive = true;
        disconnectedWatchdogMs = Calendar.getInstance().getTimeInMillis();
    }

    public void stopDisconnectedWatchdog() {
        disconnectedWatchdogIsActive = false;
    }    
    
    public void resetDisconnectedWatchdog() {
        if(disconnectedWatchdogIsActive){
            disconnectedWatchdogMs = Calendar.getInstance().getTimeInMillis();
        }
    }
    
    public boolean checkPacketTimeout(){
        return disconnectedWatchdogIsActive &&
            Calendar.getInstance().getTimeInMillis() - disconnectedWatchdogMs > disconnectionTimeoutMs;
    }

    private void activateConnectionLostWatchdog() {
        disconnectedWatchdogIsActive = true;
    }
    
}
