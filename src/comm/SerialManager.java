package comm;

import data.ConnectionParameters;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import kernel.Kernel;

/**
 *Clase que gestiona la conexión con puertos seriales del PC o interfaz.
 * @author David Escobar Sanabria
 */
public class SerialManager extends Thread {

    private SerialParameters parameters;
    /**
     *Objeto de la clase SerialConnection que gestiona la conexio n con el puerto serial.
     */
    private SerialConnection2 connection;
    private ChannelInterface manager;
    private int tamTramaOut = 0;
    private int tamTramaIn = 0;
    private String stringPort;
    private String stringRate;
    private String stFlowCIn;
    private String stFlowCOut;
    private String stDataBits;
    private String stStopBits;
    private String stParidad;
    private int channelInterface =0;
    public static final int TERMINAL=0;
    public static final int VISUALIZATION=1;
    /**
     *Constructor de la clase.
     * @param _manager Objeto que implementa la interface KernelInterface que conecta
     * el nucleo del sistema para transmisión de informacion a este. 
     */
    public SerialManager(ChannelInterface _manager) {
        manager = _manager;
        this.start();
    }

    /**
     *Constructor de la clase.
     * @param _manager Objeto que implementa la interface KernelInterface que conecta
     * el nucleo del sistema para transmisión de informacion a este.
     * @param _stringPort String con puerto al cual se realizara la conexion.
     * @param _stringRate String con velocidad de transmision en bits por segundo
     * @param _stFlowCIn String que relaciona control de flujo-entrada.
     * @param _stFlowCOut String que relaciona control de flujo-salida.
     * @param _stDataBits String que relaciona numero de bits de datos.
     * @param _stStopBits String que relaciona numero de bits de parada.
     * @param _stParidad String que relaciona la paridad.
     */
    public SerialManager(ChannelInterface _manager, String _stringPort, String _stringRate, String _stFlowCIn, String _stFlowCOut, String _stDataBits, String _stStopBits, String _stParidad) {
        channelInterface=TERMINAL;
        manager = _manager;
        this.stringPort = _stringPort;
        this.stringRate = _stringRate;
        this.stFlowCIn = _stFlowCIn;
        this.stFlowCOut = _stFlowCOut;
        this.stDataBits = _stDataBits;
        this.stStopBits = _stStopBits;
        this.stParidad = _stParidad;
        start();
       

    }

    public SerialManager(ChannelInterface _manager,int _channelInterface, String _stringPort, String _stringRate, String _stFlowCIn, String _stFlowCOut, String _stDataBits, String _stStopBits, String _stParidad) {
        manager = _manager;
        this.stringPort = _stringPort;        
        this.stringRate = _stringRate;
        this.stFlowCIn = _stFlowCIn;
        this.stFlowCOut = _stFlowCOut;
        this.stDataBits = _stDataBits;
        this.stStopBits = _stStopBits;
        this.stParidad = _stParidad;
        channelInterface=_channelInterface;
        configSerial();
        //start();
    }

    public SerialManager(ChannelInterface _manager, ConnectionParameters connectionParameters) {
        channelInterface=TERMINAL;
        manager = _manager;
        this.stringPort = connectionParameters.getPortID();
        this.stringRate = connectionParameters.getBaudRate();
        this.stFlowCIn = connectionParameters.getFlowControlIn();
        this.stFlowCOut = connectionParameters.getFlowControlOut();
        this.stDataBits = connectionParameters.getDataBits();
        this.stStopBits = connectionParameters.getStopBits();
        this.stParidad = connectionParameters.getParity();
        
        start();
    }

    /**
     *Ejecuta configuracion y ejecucion de serialConnection en hilo.
     */
    @Override
    public void run() {

        configSerial();
        //this.manager.app..jout("Comunicacion RS-232 inicializada");
    }

    /**
     *Metodo para configuracion de conexión con puerto serial.
     */
    public void configSerial() {
        parameters = new SerialParameters();
        setParameters();
        connection = new SerialConnection2(this, parameters, channelInterface);

        try {
            connection.openConnection();
            manager.joutln("Serial communication initialized !");

        } catch (SerialConnectionException e2) {
            manager.joutln("Error: Can't open the port", true);
            manager.setSerialPortAvaliable(true);
            return;
        }
    }

    /**
     *Metodo que inicializa parametros de conexión con puerto serial.
     */
    public void setParameters() {
        //System.out.println(this.stringPort + ", " + this.stringRate + ", " + this.stFlowCIn + ", " + this.stFlowCOut + ", " + this.stDataBits + ", " + this.stStopBits + ", " + this.stParidad);
        parameters.setPortName(this.stringPort);	// /dev/ttyS0
        parameters.setBaudRate(this.stringRate);
        parameters.setFlowControlIn(this.stFlowCIn);
        parameters.setFlowControlOut(this.stFlowCOut);
        parameters.setDatabits(this.stDataBits);
        parameters.setStopbits(this.stStopBits);
        parameters.setParity(this.stParidad);

        /*
        parameters.setPortName("/dev/ttyUSB0");	// /dev/ttyS0
        parameters.setBaudRate("19200");
        parameters.setFlowControlIn("None");
        parameters.setFlowControlOut("None");
        parameters.setDatabits("8");
        parameters.setStopbits("1");
        parameters.setParity("None");*/
    }

    /**
     *Metodo que cierra la conexión con puerto serial.
     */
    public void serialExit() {
        this.connection.closeConnection();
    }

    /**
     *Metodo que envia un arreglo de enteros por puerto serial.
     * @param data Arreglo de enteros.
     */
    public void sendData(int[] data) {
        System.out.println();
        for (int i = 0; i < data.length; i++) {
            this.connection.writePort(data[i]);

            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        //this.managerEx.app.jout("Trama enviada");
    }
    
    /**
     *Metodo que envia un arreglo de enteros por puerto serial.
     * @param data Arreglo de enteros.
     */
    public void sendData(byte[] data) {
        System.out.println();
        for (int i = 0; i < data.length; i++) {
            this.connection.writePort(data[i]);

            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        //this.managerEx.app.jout("Trama enviada");
    }    

    /**
     *Metodo que envia un String a traves del puerto serial.
     * @param st String que se envia.
     */
    public void sendData(String st) {
        for (int i = 0; i < st.length(); i++) {

            this.connection.writePort(st.charAt(i));

            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        //this.managerEx.app.jout("Trama enviada");
    }

    /**
     *metodo que asigna String que se lee en puerto serial para ser procesado.
     * @param st String leido en puerto serial.
     */
    public void setDataInSerial(String st) {
        manager.setDataIn(st);
    }

    /**
     *
     * @param intArray
     */
    public void setDataInSerial(int[] intArray) {
        manager.setDataIn(intArray);
    }
    /**
     *
     */
    public void closeConnection(){
        connection.closeConnection();
    }

    public void setInterface(int interf){
        connection.setChannelInterface(interf);
        
    }

    void sendDataToTerminal(char c) {
        manager.sendDataToTerminal(c);
    }

    void sendDataToTerminal(String buffer) {
        manager.sendDataToTerminal(buffer);
    }
   
}
