/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.

 */
//This program serves as a server at port 36865 and a client at port 10000
package net.udp.server;


import data.DataPacket;
import gui.app.AppInterface;
import gui.app.PanelConsole;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import kernel.Kernel;
import kernel.KernelInterface;
import terminal.gui.PanelTerminal;


/**
 *
 * @author David Escobar Sanabria and Apurva Badithela
 */
public class UDPServer extends Thread {

    private byte[] buffer = new byte[202];
    private DatagramSocket socket = null;
    private boolean connection = true;
    private int port = 9001;
    private Kernel kernel;
    private int cont = 0;
    private int[] intArray = new int[51];
    private int contFrames = 0;
    private boolean stopThread = false;
    private boolean stopped = false;

    
    
    /**
     *
     * @param _port
     * @param _kernel
     * @throws SocketException
     */
    public UDPServer(int _port, Kernel _kernel) throws SocketException {
        
        kernel = _kernel;
        port = _port;
        socket = new DatagramSocket(port);
              

//setPriority(MIN_PRIORITY);
        start();
        kernel.joutln("UDP service initilized, port: " + port);

    }
    
    
    public void sendData(int[] data)
    {
        try {
            SocketAddress addr = new InetSocketAddress("127.0.0.1", 36866);
            byte[] dataBytes = new byte[data.length];
            for(int i = 0; i < data.length; ++i){
                dataBytes[i] = (byte)data[i];
            }            
            DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length, addr);                        
            socket.send(packet);
        } catch (IOException ex) {
            Logger.getLogger(UDPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    //Added function to send data to Matlab

    
        @Override
    public void run() {

        System.out.println("Listening on port " + port);
        try {
            socket.setSoTimeout(1000);
        } catch (SocketException ex) {
            Logger.getLogger(UDPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        while (connection) {
            if(socket.isClosed()){
                return;
            }
            if(stopThread){
                stopped = true;
                socket.close();
                return;
            }
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                try{
                    socket.receive(packet);
                } catch (SocketTimeoutException ex){
                    if(stopThread){
                        stopped = true;
                        socket.close();
                        return;
                    }
                    continue;
                }
                //System.out.println(" ........... "+packet.getLength());

                for (int i = 0; i < packet.getLength(); i++) {

                    //System.out.println(">>"+packet.getData()[i]);
                    if (i > 1 && packet.getData()[i - 2] == (int) 'U' && packet.getData()[i - 1] == (int) 'U' && packet.getData()[i] == (int) 'T') {
                        //System.out.println("85 85 84 OK");
                        intArray = copy(packet.getData(), i - 2, i + DataPacket.getDataPacketSize() - 2);
                        for (int j = 0; j < intArray.length; j++) {
                            if (intArray[j] < 0) {
                                intArray[j] = intArray[j] + 256;
                            }
                        }

                        kernel.setDataIn(intArray);
                        
                        break;
                    }

                }
                //System.out.println("Brak ok");
                /*if (cont == 50) {
                intArray[cont] = (int) (packet.getData()[0]);
                if (intArray[cont] < 0) {
                intArray[cont] = intArray[cont] + 256;
                }
                //System.out.println(" ,, " + intArray[cont] + " , " + packet.getData()[0]);
                if (intArray[0] == (int) 'U' && intArray[1] == (int) 'U' && intArray[2] == (int) 'T') {
                //System.out.println("Complete frame # " + contFrames++);
                kernel.setDataIn(intArray);
                }
                cont = 0;
                } else {
                intArray[cont] = (packet.getData()[0]);
                if (intArray[cont] < 0) {
                intArray[cont] = intArray[cont] + 256;
                }

                System.out.println(" ,, " + intArray[cont] + " , " + packet.getData()[0]);

                if (cont > 1 && intArray[cont - 2] == (int) 'U' && intArray[cont - 1] == (int) 'U' && intArray[cont] == (int) 'T') {
                intArray[0] = intArray[cont - 2];
                intArray[1] = intArray[cont - 1];
                intArray[2] = intArray[cont];
                //System.out.println(" ..................................... ");
                cont = 2;
                }
                cont++;
                }*/


            } catch (IOException ex) {
                Logger.getLogger(UDPServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            //System.out.println(">> " + packet.getLength());
            //kernel.setDataIn(dataString);

        }

    }

    /**
     *
     * @return
     */
    public boolean isConnection() {
        return connection;
    }

    /**
     *
     * @param connection
     */
    public void setConnection(boolean connection) {
        this.connection = connection;
    }

    /**
     *
     * @return
     */
    public int getPort() {
        return port;
    }

    /**
     *
     * @param port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     *
     */
    public void closeConnection() {
        stopThread = true;        
    }
    
    
    private int[] copy(byte[] data, int i, int i0) {
        int k = i;
        int[] intArray2 = new int[i0 - i];
        for (int j = 0; j < i0 - i; j++) {
            intArray2[j] = data[k];
            //System.out.println("Data copied, lenght= "+intArray.length);
            k++;
        }
        //System.out.println("Data copied, lenght= "+intArray2.length);
        return intArray2;
    }
}

   