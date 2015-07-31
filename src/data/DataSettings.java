/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package data;

import java.io.Serializable;
import java.util.Vector;

/**
 *
 * @author david2010a
 */
public class DataSettings implements Serializable{

    private Vector vectorCommands;
    private ConnectionParameters connectionParemeters;

    public DataSettings(){
        vectorCommands=new Vector();
        connectionParemeters=new ConnectionParameters();
    }

    public ConnectionParameters getConnectionParemeters() {
        return connectionParemeters;
    }

    public void setConnectionParemeters(ConnectionParameters serialParemeters) {
        this.connectionParemeters = serialParemeters;
    }

    public Vector getVectorCommands() {
        return vectorCommands;
    }

    public void setVectorCommands(Vector vectorCommands) {
        this.vectorCommands = vectorCommands;
    }

    
}
