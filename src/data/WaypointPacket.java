/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package data;

/**
 *
 * @author Christian Dernehl
 */
public class WaypointPacket extends UpstreamPacket {

    public WaypointPacket() {
    }
    
    

    @Override
    protected int getPayloadSize() {
        return 1+4+4+4;
    }
    
    public void assembleWaypointPacket(byte waypointId, int longitude, int latitude, float altitude)
    {
        super.allocate();
        super.assembleHeader();
        writePayloadBufferByte(0, waypointId);
        writePayloadBufferInt32(1, longitude);
        writePayloadBufferInt32(5, latitude);                
        writePayloadBufferFloat32(9, altitude);
        super.finishPacket();
    }

    @Override
    protected int getPacketType() {
        return 1;
    }
    
}
