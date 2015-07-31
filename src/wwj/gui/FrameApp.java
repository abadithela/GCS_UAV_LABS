/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package wwj.gui;

import gov.nasa.worldwind.examples.LayerPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;

/**
 *
 * @author david
 */
public class FrameApp extends JFrame{
    //private final LayerPanel layerPanel;


    public FrameApp(){
        Dimension d=new Dimension(800,600);

        this.setDefaultCloseOperation(this.EXIT_ON_CLOSE);
        this.setSize(d);
        setLayout(new BorderLayout());
        PanelWorldWind p1=new PanelWorldWind(d,true);

        add(p1,BorderLayout.CENTER);

        this.setVisible(true);
      
        p1.addPosition(44.9802, -93.2343, 40000);

         try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            Logger.getLogger(FrameApp.class.getName()).log(Level.SEVERE, null, ex);
        }
        p1.addPosition(45.000, -93.3343, 40000);

          try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            Logger.getLogger(FrameApp.class.getName()).log(Level.SEVERE, null, ex);
        }
        p1.addPosition(45.050, -93.3543, 40000);

           try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            Logger.getLogger(FrameApp.class.getName()).log(Level.SEVERE, null, ex);
        }
        p1.addPosition(44.950, -93.3543, 40000);

    }

    public static void main(String args[]){
        FrameApp fapp=new FrameApp();
    }

}
