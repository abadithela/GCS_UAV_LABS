/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package wwj.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;

/**
 *
 * @author david
 */
public class AppWW extends JPanel{

    public PanelWorldWind panelWorldWind;
    public PanelWWSettings panelWWSettings;
    private double offset=0;
    private boolean enableWW;
    private final static double UMN_LAT=44.97;
    private final static double UMN_LON=-93.23;

    public AppWW(){

        setBackground(Color.white);
        this.setLayout(new BorderLayout());
        panelWWSettings=new PanelWWSettings();
        panelWWSettings.setParent(this);
        add(panelWWSettings,BorderLayout.NORTH);
        
        //panelWorldWind=new PanelWorldWind(getSize(), true);
        //add(panelWorldWind,BorderLayout.CENTER);
       
        
    }

    void setWW(boolean _selected) {

        enableWW=_selected;
        if(enableWW){
            System.out.println("Loading WWJ...");
            
            panelWorldWind=new PanelWorldWind(getSize(), true);
            add(panelWorldWind,BorderLayout.CENTER);

            panelWorldWind.setBounds(10,110,700,400);
            //panelWorldWind.setOffset();
            validate();
            repaint();
            
        }

        else{

            for(int i=0;i<this.getComponents().length;i++){
                if(this.getComponents()[i]==panelWorldWind){
                    remove(i);
                }
            }
           
            panelWorldWind=null;
            validate();
            repaint();
        }
    }

   public void addPosition(double lat, double lon, double alt)
     {
        if(enableWW){
            panelWorldWind.addPosition(lat, lon, alt);
        }
         
     }
   

    void setWW(boolean selected, double _offset) {

        offset=_offset;
        System.out.println("Offset: "+offset);
        enableWW=selected;
         if(enableWW){
            System.out.println("Loading WWJ...");

            panelWorldWind=new PanelWorldWind(getSize(), true, offset);
            add(panelWorldWind,BorderLayout.CENTER);

            panelWorldWind.setBounds(10,110,700,400);
            //panelWorldWind.setOffset();
            validate();
            repaint();

            demo();

        }

        else{

            for(int i=0;i<this.getComponents().length;i++){
                if(this.getComponents()[i]==panelWorldWind){
                    remove(i);
                }
            }

            panelWorldWind=null;
            validate();
            repaint();
        }


    }

    public void reSizeView(double _offset) {
        offset=_offset;
        if(enableWW){
            panelWorldWind.reSizeView(offset);
        }
        
    }

    public void demo(){


         try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(AppWW.class.getName()).log(Level.SEVERE, null, ex);
        }

        addPosition(UMN_LAT, UMN_LON, 1000); //45.07   93.38
        

    }


}