/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * PanelConsole.java
 *
 * Created on Aug 3, 2009, 2:25:38 PM
 */

package gui.app;

import java.awt.Color;

/**
 *
 * @author David Escobar Sanabria
 */
public class PanelConsole extends javax.swing.JPanel {

    /** Creates new form PanelConsole */
    public Color color1=Color.white; //new Color(255,204,51);
    /**
     *
     */
    public Color color2=new Color(173,0,35);
    /**
     *
     */
    public PanelConsole() {
        initComponents();
    }


    /**
     *
     * @param st
     */
    public void joutln(String st){

        setBackground(color1);
        labelMessage.setText(st);
        textMessage.append(st+"\n");
        textMessage.setCaretPosition(textMessage.getText().length());
    }

    /**
     *
     * @param st
     * @param error
     */
    public void joutln(String st,boolean error){

        if(error){
           setBackground(Color.red);
        }
        else{
            setBackground(color1);
        }
        labelMessage.setText(st);
        textMessage.append(st+"\n");
    }
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        labelMessage = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        textMessage = new javax.swing.JTextArea();

        setBackground(new java.awt.Color(255, 255, 255));
        setForeground(new java.awt.Color(0, 255, 0));

        labelMessage.setFont(new java.awt.Font("Tahoma", 1, 12));
        labelMessage.setText("Message:");

        textMessage.setColumns(20);
        textMessage.setRows(5);
        jScrollPane1.setViewportView(textMessage);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 529, Short.MAX_VALUE)
                    .addComponent(labelMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 521, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(labelMessage)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel labelMessage;
    private javax.swing.JTextArea textMessage;
    // End of variables declaration//GEN-END:variables

}
