package des.graph2d;


import des.graph2d.Entity2D;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author david
 */
public class HeadingDisplay extends Entity2D {

    private int height,dw,nLines,delta, centerX,centerY;
    private double lastOffsetDeg=0;
    private boolean isLineWidthLarge=true;


    public HeadingDisplay( int _centerX, int _centerY,int _height, int _dw, int _nLines, int _delta){
    
        height=_height;
        dw=_dw;
        nLines=_nLines;
        delta=_delta;
        centerX=_centerX;
        centerY=_centerY;

    }

    public void update(double[] value, Graphics2D g2){
        removeAll();

        if (nLines % 2 == 0) {
            nLines++;
        }
        double width = (nLines - 1) * dw;
        double mod = value[0] % delta;
        int cV = 0;
        int max = 0;
        int min = 0;

        double offsetDeg = 0;
        int offsetPix = 0;

        // Drawing  top arc

        double xi = 0, r = (3 * height / 2 + dw) / 2;

        //Line2D l1 = new Line2D.Double(-width / 2, -height / 2 + dh * i + modulus, width / 2, -height / 2 + dh * i + modulus);
        if (mod > 0) {
            if (mod > delta / 2) {
                cV = ((int) (((int) value[0]) / delta) + 1) * delta;
            } else {
                cV = ((int) (((int) value[0]) / delta)) * delta;
            }
        } else {
            if (Math.abs(mod) > delta / 2) {
                cV = ((int) (((int) value[0]) / delta) - 1) * delta;
            } else {
                cV = ((int) (((int) value[0]) / delta)) * delta;
            }
        }

        offsetDeg =  cV-value[0];
        offsetPix = (int) (offsetDeg * dw / delta);

        //System.out.println("offsetDeg: " + offsetDeg + " ,Pitch: " + pitch + " ,cV: " + cV);
        max = cV + ((int) (nLines / 2)) * delta;
        min = cV - ((int) (nLines / 2)) * delta;

//        if (lastOffsetDeg < -(delta / 4) && offsetDeg >= delta / 4) {
//            isLineWidthLarge = !isLineWidthLarge;
//
//        } else {
//            if (lastOffsetDeg >= (delta / 4) && offsetDeg < -delta / 4) {
//                isLineWidthLarge = !isLineWidthLarge;
//            }
//        }

        if (Math.abs(cV % (2 * delta)) == 0) {
            isLineWidthLarge = true;
        }

        if (Math.abs(cV % (2 * delta)) == delta) {
            isLineWidthLarge = false;
        }

        int fontSize = 8;
        for (int i = 0; i < nLines; i++) {
            int intLineValue = min + i * delta;
            if(intLineValue>=360){
                intLineValue=intLineValue-360;
            }
            if(intLineValue<0){
                intLineValue=360+intLineValue;
            }
            String stValue = "" + intLineValue;
            if(intLineValue==0){
                stValue = "N";
            }

            if(intLineValue==180){
                stValue = "S";
            }
            if(intLineValue==90){
                stValue = "E";
            }

            if(intLineValue==270){
                stValue = "W";
            }

            if (!isLineWidthLarge) {
                if (i % 2 == 0) {
                    Line2D l1 = new Line2D.Double(-width / 2 + dw * i + offsetPix, 0, -width / 2 + dw * i + offsetPix, height / 2);
                    addEntity(l1, Color.yellow, lineWidth);
                    addEntity(stValue, (int) (-width / 2 + dw * i + offsetPix - stValue.length() * fontSize / 2), height / 2 + 20, Color.yellow, lineWidth);

                } else {
                    Line2D l1 = new Line2D.Double(-width / 2 + dw * i + offsetPix, 0, -width / 2 + dw * i + offsetPix, height / 4);
                    addEntity(l1, Color.yellow, lineWidth);
                    //ent.addEntity(stValue, (int) (-height / 2 + dh * i + offsetPix), width / 4 + 10, Color.yellow, 1);
                    //ent.addEntity(stValue, -width / 4 - 10 - stValue.length() * fontSize, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, 1);
                }
            } if (isLineWidthLarge) {
                if (i % 2 == 0) {
                    Line2D l1 = new Line2D.Double(-width / 2 + dw * i + offsetPix, 0, -width / 2 + dw * i + offsetPix, height / 4);
                    addEntity(l1, Color.yellow, lineWidth);
                    //ent.addEntity(stValue,  (int) (-height / 2 + dh * i + offsetPix),width / 4 + 10, Color.yellow, 1);
                    //ent.addEntity(stValue, -width / 4 - 10 - stValue.length() * fontSize, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, 1);

                } else {
                    Line2D l1 = new Line2D.Double(-width / 2 + dw * i + offsetPix, 0, -width / 2 + dw * i + offsetPix, height / 2);
                    addEntity(l1, Color.yellow, lineWidth);
                    addEntity(stValue, (int) (-width / 2 + dw * i + offsetPix) - stValue.length() * fontSize / 2, height / 2 + 20, Color.yellow, 2);
                    //ent.addEntity(stValue, -width / 2 - 10 - stValue.length() * fontSize, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, 1);
                }
            }


        }
        
        int[] points_x = {-height/4, 0, height/4 };
        int[] points_y = {(int) (-height/2 - height/4), (int) (-height/2), (int) (-height/2 - height/4)};
        Polygon polTrianglePhi = new Polygon(points_x, points_y, 3);
        addEntity(polTrianglePhi, Color.yellow, 2*lineWidth);

        lastOffsetDeg = offsetDeg;
        rotateAndTranslate(0, centerX, centerY, g2);

    }

    



}
