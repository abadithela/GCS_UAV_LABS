/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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


import java.io.*;
import static java.lang.Thread.sleep;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author Apurva
 */
public class ClientToMatlab {
    private  DatagramSocket socket;
    private  InetAddress address;
    private int port = 10000;
    private Kernel kernel = null;
    private boolean stopThread = false;
    private boolean stopped = false;

        
    public ClientToMatlab(int _port, Kernel _kernel) throws SocketException {
        
        kernel = _kernel;
        port = _port;
        socket = new DatagramSocket(port);
        
        //Port and socket for transfer to MATLAB are initialized below.
        

//setPriority(MIN_PRIORITY)
        kernel.joutln("Client to Matlab service initilized, port: " + port);
    }
   
    public void sendDatatoMatlab(int[] data)
{
    try {
            SocketAddress addr = new InetSocketAddress("localhost", 10000);
            byte[] dataBytes = new byte[data.length];
            for(int i = 0; i < data.length; ++i){
                dataBytes[i] = (byte)data[i];
            }            
            DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length, addr);                        
            socket.send(packet);
        } catch (IOException ex) {
            Logger.getLogger(UDPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println("Packet Sent to Matlab");
}
        
    
public void closeConnection() {
        stopThread = true;        
    }

   
}