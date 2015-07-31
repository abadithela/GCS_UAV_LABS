/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package des.graph2d;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.Vector;

/**
 *
 * @author david2010a
 */
public abstract class Entity2D {

    private Vector shapesVector;
    private Vector colorsVector;
    private Vector lineWidthVector;
    private Vector posStringCenterX;
    private Vector posStringCenterY;
    private int nStrings = 0;
    private Vector isStatic;
    private Vector isFilled;
    protected int lineWidth = 2;
    private Vector isDashed;

    public abstract void update(double[] value, Graphics2D g2);

    public Entity2D() {
        shapesVector = new Vector();
        colorsVector = new Vector();
        lineWidthVector = new Vector();
        posStringCenterX = new Vector();
        posStringCenterY = new Vector();
        isFilled = new Vector();
        isStatic = new Vector();
        isDashed = new Vector();
    }

    public void rotateAndTranslate(double angle, int trans_x, int trans_y, Graphics2D g2) {

        int countStrings = 0;


        try {
            for (int i = 0; i < shapesVector.size(); i++) {
                g2.setColor((Color) colorsVector.elementAt(i));

                Object obj = (Object) shapesVector.elementAt(i);
                int width = ((Integer) lineWidthVector.elementAt(i)).intValue();
                g2.setStroke(new BasicStroke(width));

                if (obj instanceof Shape) {

                    AffineTransform atBack = g2.getTransform();
                    AffineTransform at = AffineTransform.getTranslateInstance(trans_x, trans_y);
                    boolean _isStatic = (Boolean) isStatic.elementAt(i);
                    boolean _isFilled = (Boolean) isFilled.elementAt(i);
                    boolean _isDashed = (Boolean) isDashed.elementAt(i);

                    if (_isDashed) {
                        float dash1[] = {10.0f};
                        BasicStroke dashed = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash1, 0.0f);
                        g2.setStroke(dashed);


                    }

                    if (!_isStatic) {
                        at.rotate(Math.toRadians(angle));
                    }


                    if (_isFilled) {
                        g2.fill(at.createTransformedShape((Shape) obj));
                    } else {
                        g2.draw(at.createTransformedShape((Shape) obj));
                    }



                    g2.setTransform(atBack);
                }

                if (obj instanceof String) {
                    //AffineTransform at = AffineTransform.getTranslateInstance(trans_x, trans_y);

                    AffineTransform atBack = g2.getTransform();
                    AffineTransform at = new AffineTransform();
                    //AffineTransform at = AffineTransform.getTranslateInstance(trans_x, trans_y);
                    int newX = ((Integer) posStringCenterX.elementAt(countStrings)).intValue(); // ((Integer)posStringCenter[0].elementAt(countStrings)).intValue() + trans_x;
                    int newY = ((Integer) posStringCenterY.elementAt(countStrings)).intValue();//((Integer)posStringCenter[1].elementAt(countStrings)).intValue() + trans_y;


                    //posStringCenter[0].setElementAt(new Integer(newX), countStrings);
                    //posStringCenter[1].setElementAt(new Integer(newY), countStrings);
                    countStrings++;
                    at.setToTranslation(trans_x, trans_y);
                    //System.out.println("newX: "+newX+" newY= "+newY+" count: "+countStrings);
                    g2.transform(at);

                    boolean _isStatic = (Boolean) isStatic.elementAt(i);
                    if (!_isStatic) {
                        at.setToRotation(Math.toRadians(angle));
                        g2.transform(at);
                    }


                    g2.drawString((String) obj, newX, newY);
                    g2.transform(at);
                    g2.setTransform(atBack);

                }

            }
        } catch (Exception e) {        }





    }

    public void addEntity(Shape shape, Color color, int lineWidth, boolean _isStatic) {

        isStatic.addElement(_isStatic);
        colorsVector.addElement(color);
        shapesVector.addElement(shape);
        lineWidthVector.addElement(new Integer(lineWidth));
        isFilled.addElement(false);
        isDashed.addElement(false);

    }

    public void addEntity(Shape shape, Color color, int lineWidth, boolean _isStatic, boolean _isDashed) {

        isStatic.addElement(_isStatic);
        colorsVector.addElement(color);
        shapesVector.addElement(shape);
        lineWidthVector.addElement(new Integer(lineWidth));
        isFilled.addElement(false);
        isDashed.addElement(_isDashed);

    }

    public void addFilledEntity(Shape shape, Color color, int lineWidth, boolean _isStatic) {

        isStatic.addElement(_isStatic);
        colorsVector.addElement(color);
        shapesVector.addElement(shape);
        lineWidthVector.addElement(new Integer(lineWidth));
        isFilled.addElement(true);
        isDashed.addElement(false);

    }

    public void addFilledEntity(Shape shape, Color color, int lineWidth) {

        isStatic.addElement(false);
        colorsVector.addElement(color);
        shapesVector.addElement(shape);
        lineWidthVector.addElement(new Integer(lineWidth));
        isFilled.addElement(true);
        isDashed.addElement(false);

    }

    public void addEntity(String st, int c_x, int c_y, Color color, int lineWidth) {
        isStatic.addElement(false);
        colorsVector.addElement(color);
        shapesVector.addElement(st);
        lineWidthVector.addElement(new Integer(lineWidth));
        posStringCenterX.addElement(new Integer(c_x));
        posStringCenterY.addElement(new Integer(c_y));
        isFilled.addElement(false);
        isDashed.addElement(false);
        nStrings++;


    }

    public void addEntity(Shape shape, Color color, int lineWidth) {

        isStatic.addElement(false);
        colorsVector.addElement(color);
        shapesVector.addElement(shape);
        lineWidthVector.addElement(new Integer(lineWidth));
        isFilled.addElement(false);
        isDashed.addElement(false);
    }

    public void addEntity(String st, int c_x, int c_y, Color color, int lineWidth, boolean _isStatic) {
        isStatic.addElement(_isStatic);
        colorsVector.addElement(color);
        shapesVector.addElement(st);
        lineWidthVector.addElement(new Integer(lineWidth));
        posStringCenterX.addElement(new Integer(c_x));
        posStringCenterY.addElement(new Integer(c_y));
        isFilled.addElement(false);
        isDashed.addElement(false);
        nStrings++;

    }

    public void removeAll() {
        colorsVector.removeAllElements();
        lineWidthVector.removeAllElements();
        shapesVector.removeAllElements();
        posStringCenterX.removeAllElements();
        posStringCenterY.removeAllElements();
        isFilled.removeAllElements();
        colorsVector.removeAllElements();
        isStatic.removeAllElements();
        isDashed.removeAllElements();
        nStrings = 0;
    }

    public void setLineWidth(int lineWidth) {
        this.lineWidth = lineWidth;
    }

    public String takeDigits(String stDouble, int nDigits) {
        
        String stOut = "";
        
        
        int pointCounter = 0;
        for (int j = 0; j < stDouble.length(); j++) {
            if (stDouble.charAt(j) == '.') {
                if (stDouble.length() -1>= j + nDigits) {
                    stOut = stDouble.substring(0, j + nDigits + 1);
                } else {
                    stOut = stDouble;

                }

                break;
            }

        }



        return stOut;
    }
}
