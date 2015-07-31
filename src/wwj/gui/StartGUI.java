/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package wwj.gui;

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.util.StatusBar;

/**
 *
 * @author david
 */
public class StartGUI {

    public WorldWindowGLCanvas wwd;
    public StatusBar statusBar;

    public StartGUI(){
            wwd=new WorldWindowGLCanvas();            
            wwd.setVisible(true);           
    }

    public static void main(String args[]){
        StartGUI stg=new StartGUI();
        
    }

}
