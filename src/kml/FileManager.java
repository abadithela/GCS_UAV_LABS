/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package kml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


/**
 *
 * @author david
 */
public  class FileManager {


    public static void saveFile(String path, String stData) {
        File file = new File(path);
        FileWriter fw = null;
        try {
            fw = new FileWriter(file);
        } catch (IOException ex) {

        }
        BufferedWriter textOut = new BufferedWriter(fw);
        try {
            textOut.write(stData);
        } catch (IOException ex) {

        }
        try {
            textOut.close();
        } catch (IOException ex) {
            
        }

    }

    void saveFile(File file, String stData) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(file);
        } catch (IOException ex) {

        }
        BufferedWriter textOut = new BufferedWriter(fw);
        try {
            textOut.write(stData);
        } catch (IOException ex) {

        }
        try {
            textOut.close();
        } catch (IOException ex) {

        }

    }
}
