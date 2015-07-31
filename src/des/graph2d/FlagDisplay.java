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
 * @author david
 */
public class FlagDisplay extends Entity2D {

    private int width, heigth, rows, columns, centerX, centerY;
    private String[] names;
    

    public FlagDisplay(int _centerX, int _centerY, int _width, int _heigth, int _rows, int _columns, String[] _names) {

        centerX = _centerX;
        centerY = _centerY;
        width = _width;
        heigth = _heigth;
        rows = _rows;
        columns = _columns;
        names = _names;

    }

    public void update(double values[], Graphics2D g2) {

        int modes = (int) values[0];

        removeAll();
        Rectangle2D r;
        int count = 0;
        double xj = 0, yi = 0;
        Color c;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                xj = (j + 1) * width / 4 + j * width;
                yi = (i + 1) * heigth / 4 + i * heigth;
                r = new Rectangle2D.Double(xj, yi, width, heigth);

                if (((modes << (15 - count)) & 32768) == 32768) {
                    c = Color.YELLOW;
                } else {
                    c = Color.BLACK;
                }
                addEntity(r, c, lineWidth, true);
                addEntity(names[count], (int) (xj + width / 2 - names[count].length() * 4), (int) (yi + heigth / 2 + 4), c, lineWidth, true);
                count++;
            }
        }


        rotateAndTranslate(0, centerX, centerY, g2);

    }


}
