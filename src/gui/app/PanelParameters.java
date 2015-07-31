/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gui.app;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author dernehl
 */
public class PanelParameters extends javax.swing.JPanel {

    private final JLabel[] watchDescriptions;
    private final JLabel[] watchValues;
    private final JCheckBox[] watchValuesScoped;
    private final JLabel[] paramDescriptions;
    private JTextField[] paramValues;
    private JButton sendParam;
    private final int watchCount = 10;
    private final int paramCount = 10;
    private final int textWidth = 50;
    private final float[] oldParams;
    private ArrayList<Integer> paramsToSend = new ArrayList<Integer>();
    private final String[] watchNames;    
    private LineChart<Number, Number> chart;
    private final JPanel chartPanel = new JPanel();
    private final JFXPanel chartPanelfx = new JFXPanel();
    private boolean FXinited = false;
    private final int GRAPH_SLIDING_WINDOW_TIME = 30;    
    private NumberAxis xAxis;
    private NumberAxis yAxis;
    private boolean javaFXsupported = false;
    private double newestTimeAllowed = (double) 0;    
        
    
    private void checkJavaVersion(){
        javaFXsupported = false;
        if(System.getProperty("java.version").startsWith("1.8")){
            javaFXsupported = true;
        }
    }
    
    /**
     * Creates new form PanelParameters
     */
    public PanelParameters() {
        initComponents();       
        checkJavaVersion();

        oldParams = new float[paramCount];
        
        watchDescriptions = new JLabel[watchCount];
        watchValues = new JLabel[watchCount];
        watchValuesScoped = new JCheckBox[watchCount];
        watchNames = new String[watchCount];
        
        paramDescriptions = new JLabel[paramCount];
        paramValues = new JTextField[paramCount];
        String[] paramNames = new String[paramCount];     
        
        try{            
            BufferedReader spec = new BufferedReader(new InputStreamReader(new FileInputStream("signals.txt")));
            for(int i = 0; i < watchCount; i++) {
                paramNames[i] = spec.readLine();
            }
            for(int i = 0; i < watchCount; i++) {
                watchNames[i] = spec.readLine();
            }            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PanelParameters.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PanelParameters.class.getName()).log(Level.SEVERE, null, ex);
        }

        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        this.setLayout(layout);
        for(int i = 0; i < watchCount; ++i)
        {            
            watchValuesScoped[i] = new JCheckBox();            
            watchValues[i] = new JLabel();
            watchValues[i].setText("Value " + (1 + i));
            watchDescriptions[i] = new JLabel();             
            if(watchNames[i] == null || watchNames[i].isEmpty() ||
                    watchNames[i].compareTo("Unused Scope") == 0){
                watchDescriptions[i].setText("Watch " + (1 + i) + ":");
                watchValuesScoped[i].setSelected(false);
            } else {
                watchDescriptions[i].setText(watchNames[i]);   
                watchValuesScoped[i].setSelected(true);
            }            
            watchValues[i].setMinimumSize(new Dimension(40,15));
            watchValues[i].setPreferredSize(new Dimension(40,15));
            watchValues[i].setMaximumSize(new Dimension(40,15));
            watchValues[i].setHorizontalTextPosition(JLabel.RIGHT);

            watchValuesScoped[i].addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                    updateChartSelection();
                }
            });
            
            c.gridx = 0;
            c.gridy = i;
            c.anchor = GridBagConstraints.LINE_START;
            c.ipadx = 10;
            c.fill = GridBagConstraints.NONE;
            this.add(watchDescriptions[i], c);            
            c.gridx  = 1;
            c.anchor = GridBagConstraints.LINE_END;
            this.add(watchValues[i], c);
            c.gridx = 2;
            this.add(watchValuesScoped[i],c);
                       
        }
        
        for(int i = 0; i < paramCount; ++i)
        {            
            paramValues[i] = new JTextField();
            //paramValues[i].setText("0.0");
            paramValues[i].setText("0.0");
            paramDescriptions[i] = new JLabel();            
            if(paramNames[i] == null || paramNames[i].isEmpty()){
                paramDescriptions[i].setText("Param " + (1 + i) + ":");
            } else {
                paramDescriptions[i].setText(paramNames[i]);
            }
            paramValues[i].setMinimumSize(new Dimension(textWidth,20));
            paramValues[i].setPreferredSize(new Dimension(textWidth,20));
            paramValues[i].setMaximumSize(new Dimension(textWidth,20));
            paramValues[i].setHorizontalAlignment(JTextField.RIGHT);
            paramValues[i].setBackground(Color.ORANGE);
            paramValues[i].addKeyListener(new java.awt.event.KeyListener() {

                public void keyTyped(KeyEvent e) {
                    
                }

                public void keyPressed(KeyEvent e) {
                    
                }

                public void keyReleased(KeyEvent e) {                    
                    JTextField field = (JTextField) e.getSource();
                    field.setBackground(Color.ORANGE);
                    Integer thisField = getIdFromTextField(field);
                    if(!paramsToSend.contains(thisField)){
                        paramsToSend.add(thisField);
                    }
                }
            });
            c.gridx = 0;
            c.gridy = watchCount + i;
            c.anchor = GridBagConstraints.LINE_START;
            c.ipadx = 10;
            c.fill = GridBagConstraints.NONE;
            this.add(paramDescriptions[i], c);            
            c.gridx  = 1;
            c.anchor = GridBagConstraints.LINE_END;                        
            this.add(paramValues[i], c);
        }       
        

        
        sendParam = new JButton();
        sendParam.setText("Send Data");
        sendParam.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendParam.setEnabled(false);               
                for(Integer i : paramsToSend)
                {
                    if(i == -1)
                        continue;
                    Float val = Float.parseFloat(paramValues[i].getText());
                    oldParams[i] = val;
                    kernel.Kernel.getInstance().sendParameter((byte)(1+i), val);                    
                }                    
                paramsToSend.clear();
                sendParam.setEnabled(true);
            }
        });
        c.gridx = 1;
        c.gridy = watchCount + paramCount;
        this.add(sendParam, c);
              

        Platform.runLater(new Runnable() {
            public void run() {
                initFX(); 
            }
        });
               
        revalidate();     
        updateChartSelection();
    }
    
    private void addSeriesToChart(){
        chart.getData().clear();
        for(int i = 0; i < watchCount; ++i)
        {         
            if(watchValuesScoped[i].isSelected()){
                XYChart.Series<Number, Number> series = new XYChart.Series();
                if(watchNames[i] == null || watchNames[i].isEmpty() ||
                        watchNames[i].compareTo("Put a Name for Logging here") == 0){
                    series.setName("Series " + (1+i));  
                } else {
                    series.setName(watchNames[i]);     
                }                       
                chart.getData().add(series); 
            }
        }
    }
    
    
    private void initFX(){
        if(!javaFXsupported){
            return;
        }
        xAxis = new NumberAxis();
        yAxis = new NumberAxis();
        xAxis.setLabel("time [s]");
        yAxis.setLabel("");
        chart = new LineChart<Number, Number>(xAxis, yAxis);
        chart.setCreateSymbols(false);
        addSeriesToChart();
        
        final int chartWidth = 650; //500;
        final int chartHeight = 400; //350;
        Scene scene = new Scene(chart, chartWidth, chartHeight);               
        chartPanelfx.setScene(scene);
        chartPanel.add(chartPanelfx);
        chartPanel.setPreferredSize(new Dimension(chartWidth,chartHeight));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 3;
        c.gridy = 0;        
        c.gridheight = watchCount + paramCount;
        this.add(chartPanel, c);
        
        FXinited = true;
        updateChartSelection();
    }
    
    private int getIdFromTextField(JTextField reference){
        for(int i = 0; i < paramCount; ++i){
            if(reference == paramValues[i])
                return i;
        }            
        return -1;
    }
    
    private long lastChartUpdate = System.currentTimeMillis();
    
    synchronized void setValues(final double[] values, final double time)
    {
        if(!FXinited)
            return;        
        Platform.runLater(new Runnable() {
            public synchronized void run() { 
                    int chartIndex = 0;
                    for(int i = 0; i < watchCount; ++i)
                    {
                        synchronized(chart.getData()){
                            watchValues[i].setText(Double.toString(values[i]));
                            
                            if(watchValuesScoped[i].isSelected()){
                                newestTimeAllowed = Math.max(newestTimeAllowed, time);                                                                                      
                            
                            
                                while(chart.getData().get(chartIndex).getData().size() > (GRAPH_SLIDING_WINDOW_TIME + 10)*50 ){
                                    //redo the chart
                                    chart.getData().get(chartIndex).getData().remove(0);                                
                                }
                                chart.getData().get(chartIndex).getData().add( 
                                        new XYChart.Data( time, values[i]));  
                            
                                chartIndex++;
                            }
                            
                        }
                    }    
                
                    long curTime = System.currentTimeMillis();
                    if(curTime - lastChartUpdate > 1000){
                        chartPanel.revalidate();
                        lastChartUpdate = curTime;             
                        xAxis.setAutoRanging(false);
                        xAxis.setLowerBound(Math.floor(Math.max(0, newestTimeAllowed - GRAPH_SLIDING_WINDOW_TIME - 5)));
                        xAxis.setUpperBound(Math.ceil(newestTimeAllowed + 5));                            
                    }                    
                                                            
                }
            
        });
        

    }
    
    void setParams(double[] values)
    {
        for(int i = 0; i < paramCount; ++i)
        {
            float oldValue = oldParams[i];
            float newValue = (float) values[i];
            if(oldValue != newValue)
            {
                paramValues[i].setText(Float.toString(newValue));                
                oldParams[i] = newValue;
            }
            boolean userChangedButNotSent = Float.parseFloat(paramValues[i].getText()) != oldValue;
            if(paramValues[i].getBackground() != Color.GREEN && !userChangedButNotSent)
                paramValues[i].setBackground(Color.GREEN);
        }
    }         
    
    private synchronized void updateChartSelection(){
        if(!FXinited)
            return;
        if(!javaFXsupported)
            return;
        Platform.runLater(new Runnable() {
            public synchronized void run() {                        
                addSeriesToChart();               
            }
        });

    }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
