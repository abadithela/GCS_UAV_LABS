/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package des.graph2d;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Line2D;

/**
 *
 * @author david
 */
public class SurfacesDisplay extends Entity2D {

    private int width, height, centerX, centerY;
    private final int max;
    private final int min;

    public SurfacesDisplay(int _center_x, int _center_y, int _width, int _height,  int min_, int max_) {
        width = _width;
        height = _height;
        centerX = _center_x;
        centerY = _center_y;
        max=max_;
        min=min_;
    }

    public void update(double[] surfaces_, Graphics2D g2) {
        removeAll();

        double[] surfaces=new double[4];

        surfaces[0]=surfaces_[0];
        if(surfaces[0]> max){
            surfaces[0]=max;
        }
        if(surfaces[0]< min){
            surfaces[0]=min;
        }

        surfaces[1]=surfaces_[1];
        if(surfaces[1]> max){
            surfaces[1]=max;
        }
        if(surfaces[1]< min){
            surfaces[1]=min;
        }


        surfaces[2]=surfaces_[2];
        if(surfaces[2]> max){
            surfaces[2]=max;
        }
        if(surfaces[2]< min){
            surfaces[2]=min;
        }

        surfaces[3]=surfaces_[3];
        if(surfaces[3]> max){
            surfaces[3]=max;
        }
        if(surfaces[3]< min){
            surfaces[3]=min;
        }


        // Aileron
        Line2D l = new Line2D.Double(-width / 2, height, width / 2, height);
        addEntity(l, Color.yellow, lineWidth, true);
        l = new Line2D.Double(-width / 2, height, -width / 2, 0);
        addEntity(l, Color.yellow, lineWidth, true);
        l = new Line2D.Double(width / 2, height, width / 2, 0);
        addEntity(l, Color.yellow, lineWidth, true);

        int b= -(int)( (width*min)/(max-min) );
        int[] points_x = {(int) (width * surfaces[0] / (max-min) +b - width / 2), (int) (width * surfaces[0] / (max-min) +b+ height / 2 - width / 2), (int) (width * surfaces[0] / (max-min) + b - height / 2 - width / 2)};
        int[] points_y = {height, 0, 0};
        Polygon polSurface = new Polygon(points_x, points_y, 3);
        addEntity(polSurface, Color.yellow, lineWidth, true);

        String stValue = "" + surfaces_[0];
        stValue=takeDigits(stValue, 1);
        stValue="ail: "+stValue;
        addEntity(stValue, -stValue.length() * 4, +height + 15, Color.yellow, lineWidth, true);


        // Rudder
        l = new Line2D.Double(-width / 2, width, width / 2, width);
        addEntity(l, Color.yellow, lineWidth, true);
        l = new Line2D.Double(-width / 2, width, -width / 2, width - height);
        addEntity(l, Color.yellow, lineWidth, true);
        l = new Line2D.Double(width / 2, width, width / 2, width - height);
        addEntity(l, Color.yellow, lineWidth, true);

        //b= -(int)( (width*min)/(max-min) );
        points_x[0] = (int) (width * surfaces[1] / (max-min) + b - width / 2);
        points_x[1] = (int) (width * surfaces[1] / (max-min) + b + height / 2 - width / 2);
        points_x[2] = (int) (width * surfaces[1] / (max-min) + b - height / 2 - width / 2);

        points_y[0] = width;
        points_y[1] = width - height;
        points_y[2] = width - height;
        polSurface = new Polygon(points_x, points_y, 3);
        addEntity(polSurface, Color.yellow, lineWidth, true);

        stValue = "" + surfaces_[1];
        stValue=takeDigits(stValue, 1);
        stValue="rud: "+stValue;
        addEntity(stValue, -stValue.length() * 4, width - height-5, Color.yellow, lineWidth, true);


        //Throttle
        l = new Line2D.Double(-width / 2 - 2 * height, 0, -width / 2 - 2 * height, width);
        addEntity(l, Color.yellow, lineWidth, true);
        l = new Line2D.Double(-width / 2 - 1 * height, 0, -width / 2 - 2 * height, 0);
        addEntity(l, Color.yellow, lineWidth, true);
        l = new Line2D.Double(-width / 2 - 1 * height, width, -width / 2 - 2 * height, width);
        addEntity(l, Color.yellow, lineWidth, true);

        points_x[0] = -width / 2 - 2 * height;
        points_x[1] = -width / 2 - 1 * height;
        points_x[2] = -width / 2 - 1 * height;

        //b= -(int)( (width*min)/(max-min) );
        points_y[0] = (int) (width * surfaces[2] / (max-min) +b);
        points_y[1] = (int) (width * surfaces[2] / (max-min) +b + height / 2);
        points_y[2] = (int) (width * surfaces[2] / (max-min) +b- height / 2);
        polSurface = new Polygon(points_x, points_y, 3);
        addEntity(polSurface, Color.yellow, lineWidth, true);

        stValue = "" + surfaces_[2];
        stValue=takeDigits(stValue, 1);
        stValue="thr: "+stValue;
        addEntity(stValue, -width / 2 + 3, width / 2, Color.yellow, lineWidth, true);


        //Elevator
        l = new Line2D.Double(width / 2 + 1 * height, 0, width / 2 + 1 * height, width);
        addEntity(l, Color.yellow, lineWidth, true);
        l = new Line2D.Double(width / 2 + 1 * height, 0, width / 2 + 2 * height, 0);
        addEntity(l, Color.yellow, lineWidth, true);
        l = new Line2D.Double(width / 2 + 1 * height, width, width / 2 + 2 * height, width);
        addEntity(l, Color.yellow, lineWidth, true);
        points_x[0] = width / 2 + 1 * height;
        points_x[1] = width / 2 + 2 * height;
        points_x[2] = width / 2 + 2 * height;

        //b= -(int)( (width*min)/(max-min) );
        
        points_y[0] = (int) (width * surfaces[3] / (max-min) +b);
        points_y[1] = (int) (width * surfaces[3] / (max-min) +b+ height / 2);
        points_y[2] = (int) (width * surfaces[3] / (max-min) +b- height / 2);
        polSurface = new Polygon(points_x, points_y, 3);
        addEntity(polSurface, Color.yellow, lineWidth, true);

        stValue = "" + surfaces_[3];
        stValue=takeDigits(stValue, 1);
        stValue="ele: "+stValue;
        addEntity(stValue, width / 2 - stValue.length() * 4 - 10, width / 2, Color.yellow, lineWidth, true);

        rotateAndTranslate(0, centerX, centerY, g2);

    }
}
