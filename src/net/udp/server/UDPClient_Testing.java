/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.udp.server;

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
public class UDPClient_Testing {
     private  DatagramSocket socket;
    private  InetAddress address;
    private int port = 10000; 
    
    
    public UDPClient_Testing (){
        
        
        try {
            //address = InetAddress.getByName("192.168.3.11") ; 
            address = InetAddress.getByName("localhost") ;
        } catch (UnknownHostException ex) {
            Logger.getLogger(UDPClient_Testing.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        sendSample();
        
        
    }
    
    public void sendSample(){
        
        try{
            socket = new DatagramSocket();
        }catch(Exception e){
            
        }
              
        byte[] buf = new byte[8];
        buf[0] = 120;
        buf[1] = 121;
        buf[2] = 122;
        buf[3] = 123;
        
        DatagramPacket packet= new DatagramPacket(buf, buf.length, address, port);
        
        try {
            socket.send(packet);
        } catch (IOException ex) {
            Logger.getLogger(UDPClient_Testing.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        socket.close();
        
        System.out.println("Packet Sent");
        

    }
    
    
    public static void main(String[] args) throws IOException {

        UDPClient_Testing client = new UDPClient_Testing(); 
    }
}
