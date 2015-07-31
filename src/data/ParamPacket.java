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
public class ParamPacket extends UpstreamPacket {        
    
    public ParamPacket(){

    }     
    
    public void assembleParameterPacket(byte paramId, float value)
    {
        super.allocate();
        super.assembleHeader();
        writePayloadBufferByte(0, paramId);
        writePayloadBufferFloat32(1, value);
        super.finishPacket();
    }


    @Override
    protected int getPayloadSize() {
        return 1+4;
    }

    @Override
    protected int getPacketType() {
        return 0;
    }
    
}
