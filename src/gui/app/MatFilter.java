/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gui.app;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author David Escobar Sanabria
 */
public class MatFilter extends FileFilter{

    @Override
    public boolean accept(File f) {
        return true;
    }

    @Override
    public String getDescription() {
        return "mat";
    }


}
