/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package data;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author Christian Dernehl
 */
public abstract class UpstreamPacket {
    private final byte[] HEADER = { 'U', 'U', 'T'};
    private int[] packet;
    private final int HEADER_SIZE = HEADER.length;
    private final int PACKET_COUNTER_SIZE = 2;
    private final int PACKET_TYPE_SIZE = 1;
    private final int PAYLOAD_START = HEADER_SIZE + PACKET_COUNTER_SIZE + PACKET_TYPE_SIZE;
    private final int CHECKSUM_SIZE = 20;
    static private int packetCounter = 0;
    
    public int[] getPacketData(){
        return packet;
    }
    
    protected abstract int getPayloadSize();
    protected abstract int getPacketType();
    
    public int getPacketSize(){
        return PAYLOAD_START + getPayloadSize() + CHECKSUM_SIZE;
    }
    
    protected void allocate(){
        packet = new int[getPacketSize()];
    }
    
    protected void assembleHeader(){
        for(int i = 0; i < HEADER_SIZE; ++i){
            packet[i] = HEADER[i];
        }
        packet[HEADER_SIZE] = packetCounter >> 8;
        packet[HEADER_SIZE+1] = packetCounter & 0xFF;
        packet[HEADER_SIZE+2] = getPacketType() & 0xFF;
    }
    
    protected void writePayloadBufferByte(int pos, int data){
        packet[PAYLOAD_START + pos] = 0xFF & data;
    }
    
    protected void writePayloadBufferInt16(int pos, int data){
        packet[PAYLOAD_START + pos] = data & 0xFF00;
        packet[PAYLOAD_START + pos + 1] = data & 0x00FF;
    }
    
    protected void writePayloadBufferInt32(int pos, int data){
        for(int i = 0; i < 4; ++i){
            int mask = 0xFF << (8*(3-i));                        
            packet[PAYLOAD_START + pos + i] = ((data & mask) >> (8*(3-i)));
        }
    }
    
    protected void writePayloadBufferFloat32(int pos, float data){
        int valBytes = Float.floatToRawIntBits(data);
        writePayloadBufferInt32(pos, valBytes);
    }
    
    protected void finishPacket(){
        packetCounter++;
        String key = "umnuavlab";
        String algorithm = "HmacSHA1";
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), algorithm);

        Mac mac;
        try {
            mac = Mac.getInstance(algorithm);
            mac.init(signingKey);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(ParamPacket.class.getName()).log(Level.SEVERE, null, ex);
            return;
        } catch (InvalidKeyException ex) {
            Logger.getLogger(ParamPacket.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        byte[] packet_data = new byte[packet.length - CHECKSUM_SIZE];
        for(int i = 0; i < packet.length - CHECKSUM_SIZE; ++i){
            packet_data[i] = (byte)packet[i];
        }        
        byte[] result = mac.doFinal(packet_data);
        for(int i = 0; i < CHECKSUM_SIZE; ++i){
            int val = 0;
            val |= result[i];            
            packet[getPacketSize()-CHECKSUM_SIZE + i] = val;
        }   
    }
    
    public UpstreamPacket(){
        
    }
    
    
}
