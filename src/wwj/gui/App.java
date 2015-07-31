/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package wwj.gui;

import java.awt.BorderLayout;
import javax.swing.JFrame;

/**
 *
 * @author david
 */
public class App extends JFrame{

    private AppWW appWW;
    public  App(){
        setLayout(new BorderLayout());
        setDefaultCloseOperation(this.EXIT_ON_CLOSE);
        setSize(800,600);
        appWW=new AppWW();
        add(appWW,BorderLayout.CENTER);

        
    }


    public static void main(String args[]){
        new App().setVisible(true);
    }
    

}
