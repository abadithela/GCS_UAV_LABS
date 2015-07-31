/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package comm;

/**
 *
 * @author David Escobar Sanabria
 */
public interface ChannelInterface {

    /**
     *Show messages to user
     * @param st String with the Message.
     * @param b  True if alram, false otherwise.
     */
    public void joutln(String st, boolean b);

    /**
     *Show messages to user
     * @param st String with the Message.
     */
    public void joutln(String st);

    /**
     *Send data to serial port.
     * @param st Data.
     */
    public void sendSerial(String st);

    /**
     *Receive data from serial port
     * @param st Data String
     */
    public void setDataIn(String st);

    /**
     *If new data is available the parameter become true.
     * @param b Parameter that indicates data available
     */
    public void setSerialPortAvaliable(boolean b);

    /**
     *Receive data from serial port.
     * @param intArray 
     */
    public void setDataIn(int[] intArray);

    public void setInterface(int iter);

    public void sendDataToTerminal(char c);

    public void sendDataToTerminal(String buffer);
}
