package comm;

//import javax.comm.*;
import data.DataPacket;
import gnu.io.*;
import java.awt.TextArea;
import java.awt.event.*;
import java.io.*;
import java.util.TooManyListenersException;

/**
 *
 * @author David Escobar Sanabria
 */
public class SerialConnection2 implements SerialPortEventListener,
        CommPortOwnershipListener {

    private SerialManager parent;
    private SerialParameters parameters;
    private OutputStream os;
    private InputStream is;
    boolean init = false;
    private CommPortIdentifier portId;
    private SerialPort sPort;
    private boolean open;
    private int channelInterface = 0;
    private String buffer = "   ";
    private int frameSize = DataPacket.getDataPacketSize();;


    /**
     *
     *
     *
     */
    public String stringData = "";
    private int[] intArray = new int[200];
    private int cont = 0;
    public final static int TERMINAL = 0;
    public final static int VISUALIZATION = 1;

    /**
     *
     * @param parent
     * @param parameters
     */
    public SerialConnection2(SerialManager parent, SerialParameters parameters) {
        this.parent = parent;
        this.parameters = parameters;
        open = false;
    }

    SerialConnection2(SerialManager parent, SerialParameters parameters, int _channel) {
        this.parent = parent;
        this.parameters = parameters;
        open = false;
        channelInterface = _channel;
        System.out.println("Interface= " + channelInterface);
    }

    /**
     *
     * @throws SerialConnectionException
     */
    public void openConnection() throws SerialConnectionException {

        try {
            portId =
                    CommPortIdentifier.getPortIdentifier(parameters.getPortName());
        } catch (NoSuchPortException e) {
            throw new SerialConnectionException(e.getMessage());
        }

        try {
            sPort = (SerialPort) portId.open("UavGui", 30000);
        } catch (PortInUseException e) {
            throw new SerialConnectionException(e.getMessage());
        }

        try {
            setConnectionParameters();
        } catch (SerialConnectionException e) {
            sPort.close();
            throw e;
        }

        try {
            os = sPort.getOutputStream();
            is = sPort.getInputStream();
        } catch (IOException e) {
            sPort.close();
            throw new SerialConnectionException("Error opening i/o streams");
        }

        try {
            sPort.addEventListener(this);
        } catch (TooManyListenersException e) {
            sPort.close();
            throw new SerialConnectionException("too many listeners added");
        }

        sPort.notifyOnDataAvailable(true);

        sPort.notifyOnBreakInterrupt(true);

        try {
            sPort.enableReceiveTimeout(30);
        } catch (UnsupportedCommOperationException e) {
        }

        // Add ownership listener to allow ownership event handling.
        portId.addPortOwnershipListener(this);

        open = true;
    }

    /**
     *
     * @throws SerialConnectionException
     */
    public void setConnectionParameters() throws SerialConnectionException {

        int oldBaudRate = sPort.getBaudRate();
        int oldDatabits = sPort.getDataBits();
        int oldStopbits = sPort.getStopBits();
        int oldParity = sPort.getParity();
        int oldFlowControl = sPort.getFlowControlMode();

        try {

            sPort.setSerialPortParams(parameters.getBaudRate(), parameters.getDatabits(), parameters.getStopbits(), parameters.getParity());
        } catch (UnsupportedCommOperationException e) {
            parameters.setBaudRate(oldBaudRate);
            parameters.setDatabits(oldDatabits);
            parameters.setStopbits(oldStopbits);
            parameters.setParity(oldParity);
            throw new SerialConnectionException("Unsupported parameter");
        }

        try {
            sPort.setFlowControlMode(parameters.getFlowControlIn() | parameters.getFlowControlOut());
        } catch (UnsupportedCommOperationException e) {
            throw new SerialConnectionException("Unsupported flow control");
        }
    }

    /**
     *
     */
    public void closeConnection() {
        if (!open) {
            return;
        }

        if (sPort != null) {
            try {
                os.close();
                is.close();
            } catch (IOException e) {
                System.err.println(e);
            }

            sPort.close();

            portId.removePortOwnershipListener(this);
        }

        open = false;
    }

    /**
     *
     */
    public void sendBreak() {
        sPort.sendBreak(1000);
    }

    /**
     *
     * @return
     */
    public boolean isOpen() {
        return open;
    }

    /**
     *
     * @param e
     */
    public void serialEvent(SerialPortEvent e) {
        StringBuffer inputBuffer = new StringBuffer();
        int newData = 0;
        int contTam = 0;
        String stData = "";
        switch (e.getEventType()) {

            case SerialPortEvent.DATA_AVAILABLE:

                if (channelInterface == VISUALIZATION) {
                    while (newData != -1) {

                        try {
                            newData = is.read();

                            if (newData == -1) {
                                break;

                            } else {

                                if (cont == frameSize-1) {

                                    intArray[cont] = newData;
                                    if (intArray[0] == (int) 'U' && intArray[1] == (int) 'U' && intArray[2] == (int) 'T') {

                                        parent.setDataInSerial(intArray);

                                    }

                                    cont = 0;
                                }

                                if (cont>0 && intArray[cont-1] =='\0'  && intArray[0] == (int) 'U' && intArray[1] == (int) 'U' && intArray[2] == (int) 'M') {

                                     parent.setDataInSerial(intArrayToString(intArray, 3, cont-1));
                                     cont =0;
                                }


                                intArray[cont] = newData;

                                if (cont > 1 && intArray[cont - 2] == (int) 'U' && intArray[cont - 1] == (int) 'U' && intArray[cont] == (int) 'T') {
                                    intArray[0] = intArray[cont - 2];
                                    intArray[1] = intArray[cont - 1];
                                    intArray[2] = intArray[cont];
                                    cont = 2;                                    
                                    frameSize = DataPacket.getDataPacketSize();;
                                    //System.out.println("sync");
                                }


                                 if (cont > 1 && intArray[cont - 2] == (int) 'U' && intArray[cont - 1] == (int) 'U' && intArray[cont] == (int) 'M') {

                                    intArray[0] = 'U';
                                    intArray[1] = 'U';
                                    intArray[2] = 'M';
                                    cont = 2;
                                    frameSize = DataPacket.getMessagePacketSize();
                                   
                                }

                                /*if (cont > 1 && intArray[cont - 2] == (int) 'U' && intArray[cont - 1] == (int) 'U' && intArray[cont] == (int) 'M') {
                                intArray[0] = intArray[cont - 2];
                                intArray[1] = intArray[cont - 1];
                                intArray[2] = intArray[cont];
                                cont = 2;
                                }*/

                                cont++;

                                //stData = stData + (char) (newData);
                            /*if (stData.length() > 2 && stData.substring(stData.length() - 3, stData.length()).compareTo("UUT") == 0) {
                                stData = "UUT";
                                }

                                if (stData.length() > 2 && stData.substring(0, 3).compareTo("UUT") == 0 && stData.length() == 51) {
                                //System.out.println("Frame data = "+stData);
                                this.parent.setDataInSerial(stData);
                                }*/
                                //inputBuffer.append((char) newData);
                            }

                        } catch (IOException ex) {
                            System.err.println(ex);
                            return;
                        }

                    }
                } else {

                    while (newData != -1) {
                        try {
                            newData = is.read();

                            if (newData == -1) {
                                break;

                            } else {
                                buffer = buffer + (char) newData;

                                if(buffer.length()>100){
                                    parent.sendDataToTerminal(buffer);
                                    buffer="";
                                }
                                //System.out.println(""+(int)buffer.charAt(buffer.length()-1));
                            }

                        } catch (IOException ex) {
                            System.err.println(ex);
                            return;
                        }


                    }
                    parent.sendDataToTerminal(buffer);
                    buffer="";

                }


                // Append received data to messageAreaIn.
                //messageAreaIn.append(new String(inputBuffer));
                break;
            case SerialPortEvent.BI:
                System.out.println("\n--- BREAK RECEIVED ---\n");
        }
    }

    /**
     *
     * @param type
     */
    public void ownershipChange(int type) {
        if (type == CommPortOwnershipListener.PORT_OWNERSHIP_REQUESTED) {
            System.out.println("PUERTO OCUPADO...");
            //PortRequestedDialog prd = new PortRequestedDialog(parent);
        }
    }

    /**
     *
     * @param ch
     */
    public void writePort(char ch) {
        try {
            this.os.write((int) ch);
        } catch (IOException e) {
            System.err.println("OutputStream write error: " + e);
        }

    }

    /**
     *
     * @param ch
     */
    public void writePort(int ch) {
        try {
            this.os.write(ch);
        } catch (IOException e) {
            System.err.println("OutputStream write error: " + e);
        }

    }

    /**
     *
     * @param b
     */
    public void writePort(byte b) {
        try {
            this.os.write(b);
        } catch (IOException e) {
            System.err.println("OutputStream write error: " + e);
        }

    }

    /**
     *
     */
    public void run() {
    }

    public void setChannelInterface(int channelInterface) {
        this.channelInterface = channelInterface;
    }


    public String intArrayToString(int[] byteArray, int begin, int end){
        String st = "";
        for(int i = begin; i<=end;i++){
            st=st+(char)intArray[i];
        }
        return st;
    }
}


