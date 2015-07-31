/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package comm;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * Stream data to server to allow mulitple users accessing information over
 * the web
 * 
 * @author Christian Dernehl
 */
public class InternetAdapter {
    
    private DatagramSocket client = null;
    private final int port = 36867;
    private final InetAddress addr;
    private boolean active = false;

    public InternetAdapter() throws UnknownHostException {
        this.addr = InetAddress.getLocalHost();
    }
    
    public void activate(){
        if(client == null){
            try {
                client = new DatagramSocket();                
            } catch (SocketException ex) {
                Logger.getLogger(InternetAdapter.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
        }
        active = true;

        try {
            initHttp();
        } catch (IOException ex) {
            Logger.getLogger(InternetAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    public void initHttp () throws MalformedURLException, IOException{
        
        class ExecuteConnection extends Thread{
            public void run(){
                try{
                    String uname = "test";                
                    String password = "test";
                    URL server = new URL("http://localhost/execute_receiver.php");
                    HttpURLConnection cn = (HttpURLConnection) server.openConnection();
                    cn.setRequestMethod("POST");
                    String parameters = "username=" + uname + "&password=" + password;
                    cn.setDoOutput(true);
                    DataOutputStream wr = new DataOutputStream(cn.getOutputStream());
                    wr.writeBytes(parameters);
                    wr.flush();
                    wr.close();   

                    cn.getResponseCode();
                } catch (Exception ex){
                }
            }
        }
        
        ExecuteConnection thread = new ExecuteConnection();
        new Thread(thread).start();
    }
    
    public void deactivate(){
        active = false;
    }
    
    public void send(int[] data) throws IOException{
        byte[] bdata = new byte[data.length];
        for(int i = 0; i < data.length; ++i){
            bdata[i] = (byte)(data[i] & 0xFF);
        }
        send(bdata);
    }
    
    public void send(byte[] data) throws IOException{
        if(client != null){            
            //client.send(new DatagramPacket(data, data.length));
            client.send(new DatagramPacket(data, data.length, addr, port));
        }
    }
    
}
