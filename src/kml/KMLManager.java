/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package kml;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import kernel.KernelInterface;

/**
 *
 * @author david
 */
public class KMLManager extends Thread{

    private boolean breakFlag=false;
    private int period=2;
    private File KMLFile;
    private KMLFrame kmlFrame;
    private KernelInterface kernel;
    private double lon_1=0,alt_1=0, lat_1=0, lon=0,lat=0,alt=0;
    private FileManager fileManager;
    public KMLManager(KernelInterface _kernel, File file, int _period){
        KMLFile=file;
        period=_period;
        kernel=_kernel;
        kmlFrame=new KMLFrame();
        fileManager=new FileManager();
        start();
    }

    public void run(){

        while(!breakFlag){            

            lon= kernel.getLongitud();
            lat=kernel.getLatitud();
            alt=kernel.getAltitude();
            if(alt!=alt_1 || lon_1!=lon || lat_1!=lat){
                String KMLString =kmlFrame.getKMLLineString( lon,lat,alt);
                try{
                    fileManager.saveFile(KMLFile, KMLString);
                }
                catch(Exception e){
                    
                }

                
            }
            try {
                Thread.sleep(period * 1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(KMLManager.class.getName()).log(Level.SEVERE, null, ex);
            }

            lon_1=lon;
            lat_1=lat;
            alt_1=alt;
            
           
        }
        
    }

    public void setBreak(boolean b){
        breakFlag=b;
    }

}