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
public class BarDisplay extends Entity2D {

    private boolean vertical = true;
    private int centerX, centerY, width, height;
    private String name;

    public BarDisplay(String name_,int centerX_, int centerY_, int width_, int heigth_, boolean verical_) {
        centerX = centerX_;
        centerY = centerY_;
        width = width_;
        height = heigth_;
        vertical = verical_;
        name=name_;
    }

    @Override
    public void update(double[] value, Graphics2D g2) {


        double v=value[0];

        if(v>100){
            v=100;
        }
        if(v<0){
            v=0;
        }
        removeAll();
        Rectangle2D r = new Rectangle2D.Double(0, 0, width, height);
        addEntity(r, Color.yellow, lineWidth);

        if (vertical) {
            int l = (int) (v * height / 100);
            r = new Rectangle2D.Double(0, height - l, width, l);
            addFilledEntity(r, Color.yellow, lineWidth, true);

        } else {
            int l = (int) (v * width / 100);
            r = new Rectangle2D.Double(0, 0, l, height);
            addFilledEntity(r, Color.yellow, lineWidth, true);
        }

        addEntity(name, width/2-name.length()*4, -10, Color.yellow, lineWidth, true);
        String stVal=""+v;
        addEntity(stVal, width/2-stVal.length()*4, height+20, Color.yellow, lineWidth, true);

        rotateAndTranslate(0, centerX, centerY, g2);

    }
}
