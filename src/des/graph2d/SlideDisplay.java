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
public class SlideDisplay extends Entity2D {

    private int centerX, centerY, width, dh, nLines, delta;
    private boolean mirror = false, negativeValues = false;

    public SlideDisplay(int _centerX, int _centerY, int _width, int _dh, int _nLines, int _delta, boolean _mirror) {
        centerX = _centerX;
        centerY = _centerY;
        width = _width;
        dh = _dh;
        delta = _delta;
        nLines = _nLines;
        mirror = _mirror;

        
    }

    public void update(double[] value, Graphics2D g2) {

        removeAll();

        if (nLines % 2 == 0) {
            nLines++;
        }
        double height = (nLines - 1) * dh;
        double mod = value[0] % delta;
        int cV = 0;
        int max = 0, min = 0;

        double offsetDeg = 0;
        int offsetPix = 0;

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

        offsetDeg = value[0] - cV;

        offsetPix = (int) (offsetDeg * dh / delta);

        //System.out.println("offsetDeg: " + offsetDeg + " ,value: " + value + " ,cV: " + cV);

        max = cV + ((int) (nLines / 2)) * delta;
        min = cV - ((int) (nLines / 2)) * delta;



        if (mirror) {

            Line2D l = new Line2D.Double(0, -height / 2 - dh / 2, 0, height / 2 + dh / 2);
            addEntity(l, Color.yellow, lineWidth);
            l = new Line2D.Double(0, -height / 2 - dh / 2, width / 2, -height / 2 - dh / 2);
            addEntity(l, Color.yellow, lineWidth);
            l = new Line2D.Double(0, height / 2 + dh / 2, width / 2, height / 2 + dh / 2);
            addEntity(l, Color.yellow, lineWidth);


            int[] points_x = {0, -dh / 2, -dh / 2};
            int[] points_y = {0, dh / 2, -dh / 2};
            Polygon polTriangle = new Polygon(points_x, points_y, 3);
            addEntity(polTriangle, Color.yellow, lineWidth);


            String stVal = "" + value[0];
            stVal=takeDigits(stVal, 1);
            addEntity(stVal, -dh / 2 - 5 - stVal.length() * 8, 0, Color.green, lineWidth);

            int fontSize = 4;
            for (int i = 0; i < nLines; i++) {
                int intLineValue = max - i * delta;

                if (intLineValue >= 0 && negativeValues == false) {
                    String stValue = "" + intLineValue;
                    Line2D l1 = new Line2D.Double(width, -height / 2 + dh * i + offsetPix, 0, -height / 2 + dh * i + offsetPix);
                    addEntity(l1, Color.yellow, lineWidth);
                    //ent.addEntity(stValue, width / 2 + 10, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, 1);
                    addEntity(stValue, width + 10, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, lineWidth);
                } else {
                    if (negativeValues == true) {
                        String stValue = "" + intLineValue;
                        Line2D l1 = new Line2D.Double(width, -height / 2 + dh * i + offsetPix, 0, -height / 2 + dh * i + offsetPix);
                        addEntity(l1, Color.yellow, lineWidth);
                        //ent.addEntity(stValue, width / 2 + 10, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, 1);
                        addEntity(stValue, width + 10, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, lineWidth);
                    }
                }



            }

            double posRef = 0;
            if (value[1] <= max && value[1] >= min) {
                posRef = (int) (((int) value[0] - (int) value[1]) * dh / delta);

            } else {
                if (value[1] > max) {
                    posRef = -1 * (int) ((nLines - 1) * dh / 2) - dh / 2;
                }
                if (value[1] < min) {

                    posRef = (int) ((nLines - 1) * dh / 2) + dh / 2;
                }
            }

            points_x[0] = 0;
            points_x[1] = 0;
            points_x[2] = -dh / 2;

            points_y[0] = (int) posRef;
            points_y[1] = (int) (posRef - dh / 2);
            points_y[2] = (int) (posRef - dh / 2);

            polTriangle = new Polygon(points_x, points_y, 3);
            addEntity(polTriangle, Color.green, lineWidth);

            points_x[0] = 0;
            points_x[1] = 0;
            points_x[2] = -dh / 2;

            points_y[0] = (int) posRef;
            points_y[1] = (int) (posRef + dh / 2);
            points_y[2] = (int) (posRef + dh / 2);

            polTriangle = new Polygon(points_x, points_y, 3);
            addEntity(polTriangle, Color.green, lineWidth);


        } else {

            Line2D l = new Line2D.Double(0, -height / 2 - dh / 2, 0, height / 2 + dh / 2);
            addEntity(l, Color.yellow, lineWidth);
            l = new Line2D.Double(0, -height / 2 - dh / 2, -width / 2, -height / 2 - dh / 2);
            addEntity(l, Color.yellow, lineWidth);
            l = new Line2D.Double(0, height / 2 + dh / 2, -width / 2, height / 2 + dh / 2);
            addEntity(l, Color.yellow, lineWidth);


            int[] points_x = {0, dh / 2, dh / 2};
            int[] points_y = {0, dh / 2, -dh / 2};
            Polygon polTriangle = new Polygon(points_x, points_y, 3);
            addEntity(polTriangle, Color.yellow, lineWidth);


            String stVal = "" + value[0];
            stVal=takeDigits(stVal, 1);
            addEntity(stVal, dh / 2 + 5, 0, Color.green, lineWidth);

            int fontSize = 8;
            for (int i = 0; i < nLines; i++) {
                int intLineValue = max - i * delta;

                if (intLineValue >= 0 && negativeValues == false) {
                    String stValue = "" + intLineValue;
                    Line2D l1 = new Line2D.Double(-width, -height / 2 + dh * i + offsetPix, 0, -height / 2 + dh * i + offsetPix);
                    addEntity(l1, Color.yellow, lineWidth);
                    //ent.addEntity(stValue, width / 2 + 10, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, 1);
                    addEntity(stValue, -width - stValue.length() * fontSize - 10, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, lineWidth);
                } else {
                    if (negativeValues == true) {
                        String stValue = "" + intLineValue;
                        Line2D l1 = new Line2D.Double(-width, -height / 2 + dh * i + offsetPix, 0, -height / 2 + dh * i + offsetPix);
                        addEntity(l1, Color.yellow, lineWidth);
                        //ent.addEntity(stValue, width / 2 + 10, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, 1);
                        addEntity(stValue, -width - stValue.length() * fontSize - 10, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, lineWidth);
                    }
                }



            }

            double posRef = 0;
            if (value[1] <= max && value[1] >= min) {
                posRef = (int) (((int) value[0] - (int) value[1]) * dh / delta);

            } else {
                if (value[1] > max) {
                    posRef = -1 * (int) ((nLines - 1) * dh / 2) - dh / 2;

                }
                if (value[1] < min) {
                    posRef = (int) ((nLines - 1) * dh / 2) + dh / 2;
                }
            }


            points_x[0] = 0;
            points_x[1] = 0;
            points_x[2] = dh / 2;

            points_y[0] = (int) posRef;
            points_y[1] = (int) (posRef - dh / 2);
            points_y[2] = (int) (posRef - dh / 2);

            polTriangle = new Polygon(points_x, points_y, 3);
            addEntity(polTriangle, Color.green, lineWidth);

            points_x[0] = 0;
            points_x[1] = 0;
            points_x[2] = dh / 2;

            points_y[0] = (int) posRef;
            points_y[1] = (int) (posRef + dh / 2);
            points_y[2] = (int) (posRef + dh / 2);

            polTriangle = new Polygon(points_x, points_y, 3);
            addEntity(polTriangle, Color.green, lineWidth);
        }

        rotateAndTranslate(0, centerX, centerY, g2);

    }

    
}
