/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package des.graph2d;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 *
 * @author david2010a
 */
public class TimeDisplay extends Entity2D{

    private int centerX,  centerY,  fontSize;

    public TimeDisplay(int centerX_, int centerY_, int fontSize_){
        centerX=centerX_;
        centerY=centerY_;
        fontSize=fontSize_;
    }
    @Override
    public void update(double[] value, Graphics2D g2) {

        removeAll();
        double time= value[0];
        String stT = ""+time;
        stT=this.takeDigits(stT, 6);
        String stTime="Time: "+stT;

        addEntity(stTime, 0, 0, Color.yellow, lineWidth, true);
     
        rotateAndTranslate(0, centerX, centerY, g2);

    }

    

}
