/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package des.graph2d;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;

/**
 *
 * @author david2010a
 */
public class ThetaPhiDisplay extends Entity2D {

    private int dh;
    private int nLines;
    private int delta;
    private double lastOffsetDeg;
    private boolean isLineWidthLarge = true;
    private int center_x, center_y;
    private boolean fillNegativeValues = false;

    public ThetaPhiDisplay(int _center_x, int _center_y, int _dh, int _nLines, int _delta) {

        super();
        dh = _dh;
        delta = _delta;
        nLines = _nLines;
        center_x = _center_x;
        center_y = _center_y;

    }

    public void update(double[] value, Graphics2D g2) {

        double theta = value[0];
        double phi = value[1];
        this.removeAll();
        if (nLines % 2 == 0) {
            nLines++;
        }
        double height = (nLines - 1) * dh;
        int width = (int) (height / 2);
        double mod = theta % delta;
        int cV = 0;
        int max = 0;

        double offsetDeg = 0;
        int offsetPix = 0;

        // Drawing  top arc

        double xi = 0, r = 7 * width / 4;

        double arcOffset = 0;

        int startAngle = 30;
        int arcAngle = 120;
        Arc2D arc = new Arc2D.Double(-r, -r, 2 * r, 2 * r, startAngle, arcAngle, Arc2D.OPEN);
        addEntity(arc, Color.yellow, lineWidth);



        Line2D la;//= new Line2D.Double(xi, -r-arcOffset, xi, -r-arcOffset-dh/2);
        //ent.addEntity(la, Color.yellow, 2);

        double rot = startAngle;

        int sectionAngle = 15;
        int nDiv = (int) (arcAngle / sectionAngle) + 1;


        for (int i = 0; i < nDiv; i++) {
            if (rot == 90) {
                int[] points_x = {0, dh / 8, -dh / 8};
                int[] points_y = {(int) (-r - arcOffset - dh / 4), (int) (-r - arcOffset - dh / 2 - dh / 4), (int) (-r - arcOffset - dh / 2 - dh / 4)};
                Polygon polTrianglePhi = new Polygon(points_x, points_y, 3);
                this.addEntity(polTrianglePhi, Color.yellow, 2 * lineWidth);

            } else {
                if (i % 2 == 0) {
                    la = new Line2D.Double(r * Math.cos(Math.toRadians(rot)), -r * Math.sin(Math.toRadians(rot)) - arcOffset, (r + dh / 2) * Math.cos(Math.toRadians(rot)), -(r + dh / 2) * Math.sin(Math.toRadians(rot)) - arcOffset);
                    addEntity(la, Color.yellow, lineWidth);

                } else {
                    la = new Line2D.Double(r * Math.cos(Math.toRadians(rot)), -r * Math.sin(Math.toRadians(rot)) - arcOffset, (r + dh / 4) * Math.cos(Math.toRadians(rot)), -(r + dh / 4) * Math.sin(Math.toRadians(rot)) - arcOffset);
                    addEntity(la, Color.yellow, lineWidth);

                }
            }
            rot = rot + sectionAngle;


        }
        //Line2D l1 = new Line2D.Double(-width / 2, -height / 2 + dh * i + modulus, width / 2, -height / 2 + dh * i + modulus);
        if (mod > 0) {
            if (mod > delta / 2) {
                cV = ((int) (((int) theta) / delta) + 1) * delta;
            } else {
                cV = ((int) (((int) theta) / delta)) * delta;
            }
        } else {
            if (Math.abs(mod) > delta / 2) {
                cV = ((int) (((int) theta) / delta) - 1) * delta;
            } else {
                cV = ((int) (((int) theta) / delta)) * delta;
            }
        }



        offsetDeg = theta - cV;
        offsetPix = (int) (offsetDeg * dh / delta);

        //System.out.println("offsetDeg: " + offsetDeg + " ,Pitch: " + pitch + " ,cV: " + cV);

        max = cV + ((int) (nLines / 2)) * delta;

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
            int intLineValue = max - i * delta;
            String stValue = "" + intLineValue;



            if (isLineWidthLarge) {
                if (i % 2 == 0) {
                    Line2D l1 = new Line2D.Double(-width / 2, -height / 2 + dh * i + offsetPix, width / 2, -height / 2 + dh * i + offsetPix);
                    if (intLineValue < 0) {
                        addEntity(l1, Color.yellow, lineWidth, false, true);
                        addEntity(stValue, width / 2 + 10, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, lineWidth);
                        addEntity(stValue, -width / 2 - stValue.length() * fontSize - 10, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, lineWidth);
                    } else {
                        if (intLineValue == 0) {
                            l1 = new Line2D.Double(-3 * width / 2, -height / 2 + dh * i + offsetPix, 3 * width / 2, -height / 2 + dh * i + offsetPix);
                        } else {
                            addEntity(stValue, width / 2 + 10, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, lineWidth);
                            addEntity(stValue, -width / 2 - stValue.length() * fontSize - 10, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, lineWidth);
                        }
                        addEntity(l1, Color.yellow, lineWidth);
                    }




                } else {
                    Line2D l1 = new Line2D.Double(-width / 4, -height / 2 + dh * i + offsetPix, width / 4, -height / 2 + dh * i + offsetPix);
                    if (intLineValue < 0) {
                        addEntity(l1, Color.yellow, lineWidth, false, true);
                        addEntity(stValue, width / 4 + 10, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, lineWidth);
                        addEntity(stValue, -width / 4 - 10 - stValue.length() * fontSize, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, lineWidth);
                    } else {
                        if (intLineValue == 0) {
                            l1 = new Line2D.Double(-3 * width / 2, -height / 2 + dh * i + offsetPix, 3 * width / 2, -height / 2 + dh * i + offsetPix);
                        } else {
                            addEntity(stValue, width / 4 + 10, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, lineWidth);
                            addEntity(stValue, -width / 4 - 10 - stValue.length() * fontSize, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, lineWidth);
                        }
                        addEntity(l1, Color.yellow, lineWidth);
                    }

                }
            } else {
                if (i % 2 == 0) {
                    Line2D l1 = new Line2D.Double(-width / 4, -height / 2 + dh * i + offsetPix, width / 4, -height / 2 + dh * i + offsetPix);
                    if (intLineValue < 0) {
                        addEntity(l1, Color.yellow, lineWidth, false, true);
                        addEntity(stValue, width / 4 + 10, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, lineWidth);
                        addEntity(stValue, -width / 4 - 10 - stValue.length() * fontSize, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, lineWidth);
                    } else {
                        if (intLineValue == 0) {
                            l1 = new Line2D.Double(-3 * width / 2, -height / 2 + dh * i + offsetPix, 3 * width / 2, -height / 2 + dh * i + offsetPix);

                        } else {
                            addEntity(stValue, width / 4 + 10, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, lineWidth);
                            addEntity(stValue, -width / 4 - 10 - stValue.length() * fontSize, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, lineWidth);
                        }
                        addEntity(l1, Color.yellow, lineWidth);
                    }


                } else {
                    Line2D l1 = new Line2D.Double(-width / 2, -height / 2 + dh * i + offsetPix, width / 2, -height / 2 + dh * i + offsetPix);
                    if (intLineValue < 0) {
                        addEntity(l1, Color.yellow, lineWidth, false, true);
                        addEntity(stValue, width / 2 + 10, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, lineWidth);
                        addEntity(stValue, -width / 2 - 10 - stValue.length() * fontSize, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, lineWidth);
                    } else {
                        if (intLineValue == 0) {
                            l1 = new Line2D.Double(-3 * width / 2, -height / 2 + dh * i + offsetPix, 3 * width / 2, -height / 2 + dh * i + offsetPix);
                        } else {
                            addEntity(stValue, width / 2 + 10, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, lineWidth);
                            addEntity(stValue, -width / 2 - 10 - stValue.length() * fontSize, (int) (-height / 2 + dh * i + offsetPix), Color.yellow, lineWidth);
                        }
                        addEntity(l1, Color.yellow, lineWidth);
                    }

                }
            }

        }

        lastOffsetDeg = offsetDeg;

        int[] points_x = {- 20, 0, 20, 0};
        int[] points_y = {30, 0, 30, 10};
        Polygon polTriangleCenter = new Polygon(points_x, points_y, 4);

        addEntity(polTriangleCenter, Color.green, lineWidth, true);
        String stAngle = "" + theta;
        if (stAngle.length() > 7) {
            stAngle = stAngle.substring(0, 7);
        }
        //g2.drawString(stAngle,  - (int) (8 * (stAngle.length() / 2)),  - 5);
        addEntity(stAngle, -stAngle.length() * 4, -5, Color.green, lineWidth, true);

        int[] points_x_tr = {-dh / 8, dh / 8, 0};
        int[] points_y_tr = {(int) (-r - arcOffset + 2 * dh / 4), (int) (-r - arcOffset + 2 * dh / 4), (int) (-r - arcOffset + dh / 4)};
        Polygon polTriangStatic = new Polygon(points_x_tr, points_y_tr, 3);
        addEntity(polTriangStatic, Color.yellow, 2 * lineWidth, true);
        rotateAndTranslate(phi, center_x, center_y, g2);

    }
}
