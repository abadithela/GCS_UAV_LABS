/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * PanelSettings.java
 *
 * Created on Aug 3, 2009, 12:32:10 PM
 */

package gui.app;

import data.ConnectionParameters;
import java.util.Vector;

/**
 *
 * @author David Escobar Sanabria
 */

public class PanelSettings extends javax.swing.JPanel {

    public AppInterface parent;
    private int interf=0;
    private int mode=0;
    /** Creates new form PanelSettings */
    public PanelSettings() {
        initComponents();
        panelTerminal1.setParent(this);
    }

    void setParent(PanelApp _parent) {
        parent =_parent;

    }

    /**
     *
     * @return
     */
    public String getRate() {
        String rta = "";

        switch (this.comboBoxRate.getSelectedIndex()) {
            case 0:
                rta = "4800";
                break;

            case 1:
                rta = "9600";
                break;

            case 2:
                rta = "14400";
                break;

            case 3:
                rta = "19200";
                break;

            case 4:
                rta = "28800";
                break;

            case 5:
                rta = "38400";
                break;

            case 6:
                rta = "57600";
                break;
            case 7:
                rta = "115200";
                break;
        }
        return rta;
    }

    /**
     *
     * @return
     */
    public String[] getFLowControl() {
        String[] rta = new String[2];

        switch (this.comboBoxFlowControl.getSelectedIndex()) {
            case 0:
                rta[0] = "None";
                rta[1] = "None";
                break;

            case 1:
                rta[0] = "Xon/Xoff In";
                rta[1] = "Xon/Xoff Out";
                break;

            case 2:
                rta[0] = "RTS/CTS In";
                rta[1] = "RTS/CTS Out";
                break;
        }
        return rta;
    }

    /**
     *
     * @return
     */
    public String getDataBits() {
        String rta = "";

        switch (this.comboBoxDataBits.getSelectedIndex()) {
            case 0:
                rta = "5";
                break;

            case 1:
                rta = "6";
                break;

            case 2:
                rta = "7";
                break;

            case 3:
                rta = "8";
                break;

        }
        return rta;
    }

    /**
     *
     * @return
     */
    public String getStopBits() {
        String rta = "";

        switch (this.comboBoxStopBits.getSelectedIndex()) {
            case 0:
                rta = "1";
                break;

            case 1:
                rta = "1.5";
                break;

            case 2:
                rta = "2";
                break;

        }
        return rta;
    }

    /**
     *
     * @return
     */
    public String getParity() {
        String rta = "";

        switch (this.comboBoxStopBits.getSelectedIndex()) {
            case 0:
                rta = "None";
                break;

            case 1:
                rta = "Even";
                break;

            case 2:
                rta = "Odd";
                break;

        }
        return rta;
    }

    /**
     *
     * @return
     */
    public String getUDPPort(){
        return textUDPPort.getText();
    }

    /**
     *
     * @return
     */
    public String getSerialPort(){
        return textSerialPort.getText();
    }
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        textSerialPort = new javax.swing.JTextField();
        comboBoxRate = new javax.swing.JComboBox();
        comboBoxDataBits = new javax.swing.JComboBox();
        comboBoxStopBits = new javax.swing.JComboBox();
        comboBoxParity = new javax.swing.JComboBox();
        comboBoxFlowControl = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        textUDPPort = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        comboBoxInterface = new javax.swing.JComboBox();
        panelTerminal1 = new terminal.gui.PanelTerminal();
        jLabel11 = new javax.swing.JLabel();
        comboBoxMode = new javax.swing.JComboBox();

        jLabel1.setText("Bit Rate");

        jLabel2.setText("Data bits");

        jLabel3.setText("Stop bits");

        jLabel4.setText("Parity");

        jLabel5.setText("Port name");

        textSerialPort.setBackground(new java.awt.Color(73, 68, 103));
        textSerialPort.setForeground(new java.awt.Color(254, 254, 254));
        textSerialPort.setText("COM1");
        textSerialPort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                textSerialPortActionPerformed(evt);
            }
        });

        comboBoxRate.setBackground(new java.awt.Color(73, 68, 103));
        comboBoxRate.setForeground(new java.awt.Color(255, 255, 255));
        comboBoxRate.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "4800", "9600", "14400", "19200", "28800", "38400", "57600", "115200" }));
        comboBoxRate.setSelectedIndex(7);

        comboBoxDataBits.setBackground(new java.awt.Color(73, 68, 103));
        comboBoxDataBits.setForeground(new java.awt.Color(255, 255, 255));
        comboBoxDataBits.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "5", "6", "7", "8" }));
        comboBoxDataBits.setSelectedIndex(3);
        comboBoxDataBits.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxDataBitsActionPerformed(evt);
            }
        });

        comboBoxStopBits.setBackground(new java.awt.Color(73, 68, 103));
        comboBoxStopBits.setForeground(new java.awt.Color(255, 255, 255));
        comboBoxStopBits.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "1.5", "2" }));

        comboBoxParity.setBackground(new java.awt.Color(73, 68, 103));
        comboBoxParity.setForeground(new java.awt.Color(255, 255, 255));
        comboBoxParity.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "None", "even", "odd" }));

        comboBoxFlowControl.setBackground(new java.awt.Color(73, 68, 103));
        comboBoxFlowControl.setForeground(new java.awt.Color(255, 255, 255));
        comboBoxFlowControl.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "None", "Xon/Xoff", "RTS/CTS" }));

        jLabel6.setText("Flow control");

        jLabel7.setFont(new java.awt.Font("Tahoma", 1, 18));
        jLabel7.setText("UDP settings:");

        jLabel8.setFont(new java.awt.Font("Tahoma", 1, 18));
        jLabel8.setText("Serial port settings:");

        textUDPPort.setBackground(new java.awt.Color(73, 68, 103));
        textUDPPort.setForeground(new java.awt.Color(255, 255, 255));
        textUDPPort.setText("36865");
        textUDPPort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                textUDPPortActionPerformed(evt);
            }
        });

        jLabel9.setText("UDP port");

        jLabel10.setText("Mode");

        comboBoxInterface.setBackground(new java.awt.Color(73, 68, 103));
        comboBoxInterface.setForeground(new java.awt.Color(255, 255, 255));
        comboBoxInterface.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Terminal", "Visualization" }));
        comboBoxInterface.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxInterfaceActionPerformed(evt);
            }
        });

        jLabel11.setText("Interface");

        comboBoxMode.setBackground(new java.awt.Color(73, 68, 103));
        comboBoxMode.setForeground(new java.awt.Color(255, 255, 255));
        comboBoxMode.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ASCII", "Binary" }));
        comboBoxMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxModeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelTerminal1, javax.swing.GroupLayout.DEFAULT_SIZE, 825, Short.MAX_VALUE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(textUDPPort, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 62, Short.MAX_VALUE))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(textSerialPort)
                                .addComponent(jLabel5))
                            .addGap(18, 18, 18)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(comboBoxRate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGap(18, 18, 18)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(comboBoxDataBits, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel2))
                            .addGap(18, 18, 18)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(jLabel3)
                                    .addGap(18, 18, 18)
                                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(comboBoxStopBits, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(comboBoxParity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(comboBoxFlowControl, 0, 0, Short.MAX_VALUE)
                                .addComponent(jLabel6))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(comboBoxInterface, 0, 103, Short.MAX_VALUE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(comboBoxMode, 0, 101, Short.MAX_VALUE)
                                .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, 97, Short.MAX_VALUE)))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textSerialPort))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4)
                            .addComponent(jLabel6)
                            .addComponent(jLabel11)
                            .addComponent(jLabel10))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(comboBoxRate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(comboBoxDataBits, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(comboBoxStopBits, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(comboBoxParity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(comboBoxFlowControl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(comboBoxInterface, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(comboBoxMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(21, 21, 21)
                .addComponent(jLabel7)
                .addGap(3, 3, 3)
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textUDPPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(panelTerminal1, javax.swing.GroupLayout.PREFERRED_SIZE, 401, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void textSerialPortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_textSerialPortActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_textSerialPortActionPerformed

    private void comboBoxDataBitsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxDataBitsActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_comboBoxDataBitsActionPerformed

    private void textUDPPortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_textUDPPortActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_textUDPPortActionPerformed

    private void comboBoxInterfaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxInterfaceActionPerformed
        // TODO add your handling code here:
        if(interf != comboBoxInterface.getSelectedIndex()){
            interf=comboBoxInterface.getSelectedIndex();
            parent.setInterface(interf);
        }



    }//GEN-LAST:event_comboBoxInterfaceActionPerformed

    private void comboBoxModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxModeActionPerformed
        // TODO add your handling code here:
        this.mode = getMode();

    }//GEN-LAST:event_comboBoxModeActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox comboBoxDataBits;
    private javax.swing.JComboBox comboBoxFlowControl;
    private javax.swing.JComboBox comboBoxInterface;
    private javax.swing.JComboBox comboBoxMode;
    private javax.swing.JComboBox comboBoxParity;
    private javax.swing.JComboBox comboBoxRate;
    private javax.swing.JComboBox comboBoxStopBits;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private terminal.gui.PanelTerminal panelTerminal1;
    private javax.swing.JTextField textSerialPort;
    private javax.swing.JTextField textUDPPort;
    // End of variables declaration//GEN-END:variables

    void sendDataToTerminal(char c) {

        if(mode==0){
            panelTerminal1.setNewChar(c);
        }
        else{
            panelTerminal1.setNewString(""+(int)c);
        }
        
        
    }

    public void sendData(char data, int method){
        parent.sendDataToPort(data,method);
    }


    public void sendData(String data, int method) {
        parent.sendDataToPort(data ,method);
    }

    int getChannelInterface() {
        return comboBoxInterface.getSelectedIndex();
    }

    int getMode() {
        return comboBoxMode.getSelectedIndex();
    }

    void sendDataToTerminal(String buffer) {
        if(mode == 0){
            panelTerminal1.setNewString(buffer);
        }
        else{
            panelTerminal1.setNewString(this.stringToStringNumbers(buffer));
            
        }
        
    }

    public Vector getListOfCommands(){
        return this.panelTerminal1.getListOfCommands();
    }

    void setListOfCommands(Vector vectorCommands) {
        panelTerminal1.setListOfCommands(vectorCommands);
    }

    void setConnectionParameters(ConnectionParameters connectionParameters) {
        textSerialPort.setText(connectionParameters.getPortID());
        textUDPPort.setText(""+connectionParameters.getPortDecNumber());
        
    }
    private String stringToStringNumbers(String stIn){
        String st ="";
        for(int i= 0; i< stIn.length(); i++){
            st = st+(int)stIn.charAt(i)+" ";
        }
        return st;
    }

}
