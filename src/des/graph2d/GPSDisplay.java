/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package des.graph2d;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;


/**
 *
 * @author david2010a
 */
public class GPSDisplay extends Entity2D{

    private int centerX;
    private int centerY;
    private int width;
    private int height;
    private int fontSize;
    public GPSDisplay(int _centerX, int _centerY, int _width, int _height, int _fontSize){
        centerX=_centerX;
        centerY=_centerY;
        width=_width;
        height=_height;
        fontSize=_fontSize;

    }
    @Override
    public void update(double[] value, Graphics2D g2) {

        removeAll();
        double lat=value[0];
        double lon=value[1];
        double nSatellites=value[2];

         Rectangle2D r = new Rectangle2D.Double(0,0, width, height);
         addEntity(r, Color.yellow, lineWidth, true);
         String stLon="Lon: "+lon;
         stLon = takeDigits(stLon, 5);
         String stLat="Lat: "+lat;
         stLat = takeDigits(stLat, 5);
         String stNSat="# Sats: "+nSatellites;

         

         addEntity(stLat, 10, height/3, Color.yellow, lineWidth, true);
         addEntity(stLon, 10, 2*height/3+10, Color.yellow, lineWidth, true);
         addEntity(stNSat, 2*width/3, 2*height/3, Color.yellow, lineWidth, true);
         
         rotateAndTranslate(0, centerX, centerY, g2);
         
        
    }


}
