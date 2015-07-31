/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author David Escobar Sanabria
 * University of Minnesota
 * Department of Aerospace engineering
 * UAV resarch Group
 */
public class DataPacket implements Serializable {

    private String dataString = "";
    private int[] dataIntArray = null;
    private double[] dataArray = null;
    private double time = 0;
    private double ias_ref = 0;
    private double altRef = 0;
    private double distance2waypoint = 0;
    private double p = 0;
    private double q = 0;
    private double r = 0;
    private double altitude = 0;
    private double IAS = 0;
    private double psi = 0;
    private double theta = 0;
    private double phi = 0;
    private double rudder = 0;
    private double elevator = 0;
    private double throttle = 0;
    private double aileron = 0;
    private double cpuload;
    private double longitud = 0;
    private double latitud = 0;
    private double flight_mode = 0;
    //Added Accelerometer data. 6 channels --- 16bits each
    private double accelLF = 0;
    private double accelLR = 0;
    private double accelCF = 0;
    private double accelCR = 0;
    private double accelRF = 0;
    private double accelRR = 0;
    
    private static final int customDataCount = 10;
    private final double[] customScopes;
    private static final int customParameterCount = 10;
    private final double[] customParameters;
    private static final int customWaypointsDownstreamCount = 5;
    private final Waypoint[] customWaypointsDownstream;
    
    
    private double gpsSatellites=0;
    private String message="";
    private boolean messagaAvaliable=false;

    private static final int headerSize = 3;
    private static final int basePayloadSize = 46;
    private  static final int customScopesSize = Float.BYTES * customDataCount;    
    private static final int customParameterSize = Float.BYTES * customParameterCount;
    private static final int customWaypointSize = 13 * customWaypointsDownstreamCount;
    private static final int checksumSize = 2;
    private static final int frameSize = headerSize + basePayloadSize 
            + customScopesSize + customParameterSize + customWaypointSize
            + checksumSize; 
    
    // FLIGHT MODES:
    /**
     * 
     */
    public static final int MODE_MANUAL = 0;
    /**
     * 
     */
    public static final int MODE_AHD = 1;
    /**
     * 
     */
    public static final int MODE_IHD = 2;
    /**
     * 
     */
    public static final int MODE_WPN = 3;
    private boolean checkSum = false;


    /**
     * 
     */
    public DataPacket() {
        dataArray = new double[20+customDataCount+customParameterCount+3*customWaypointsDownstreamCount];
        customScopes = new double[customDataCount];
        customParameters = new double[customParameterCount];
        customWaypointsDownstream = new Waypoint[customWaypointsDownstreamCount];
    }

    /**
     *
     * @return
     */
    public double getIAS() {
        return IAS;
    }

    /**
     *
     * @param IAS
     */
    public void setIAS(int IAS) {
        this.IAS = IAS;
    }

    /**
     *
     * @return
     */
    public double getAileron() {
        return aileron;
    }

    /**
     *
     * @param aileron
     */
    public void setAileron(int aileron) {
        this.aileron = aileron;
    }

    /**
     *
     * @return
     */
    public double getAltitude() {
        return altitude;
    }

    /**
     *
     * @param altitude
     */
    public void setAltitude(int altitude) {
        this.altitude = altitude;
    }

    /**
     *
     * @return
     */
    public double getDistance2waypoint() {
        return distance2waypoint;
    }

    /**
     *
     * @param distance2waypoint
     */
    public void setDistance2waypoint(int distance2waypoint) {
        this.distance2waypoint = distance2waypoint;
    }

    /**
     *
     * @return
     */
    public double getIas_ref() {
        return ias_ref;
    }

    /**
     *
     * @param ias_ref
     */
    public void setIas_ref(int ias_ref) {
        this.ias_ref = ias_ref;
    }

    /**
     *
     * @return
     */
    public int[] getDataIntArray() {
        return dataIntArray;
    }

    public double getCpuload() {
        return cpuload;
    }

    public void setCpuload(double cpuload) {
        this.cpuload = cpuload;
    }

    /**
     *
     * @param dataIntArray
     */
    public void setDataIntArray(int[] dataIntArray) {
        this.dataIntArray = dataIntArray;

        if (dataIntArray[2] == (int)'M') {
            message = bytesToString(dataIntArray, 3, 102) ;
            messagaAvaliable = true;


        }else{
            doCheckSum();
            if (checkSum) {
                intArrayToValues(dataIntArray);
            }
        }




    }

    /**
     *
     * @return
     */
    public String getDataString() {
        return dataString;
    }

    /**
     *
     * @param dataString
     */
    public  void setDataString(String dataString) {
        this.dataString = dataString;

         message = dataString ;
         messagaAvaliable = true;

        /*stringToIntArray(dataString);
        doCheckSum();

        intArrayToValues(dataIntArray);*/


    }

    /**
     *
     * @return
     */
    public double getElevator() {
        return elevator;
    }

    /**
     *
     * @param elevator
     */
    public void setElevator(int elevator) {
        this.elevator = elevator;
    }

    /**
     *
     * @return
     */
    public double getFlight_mode() {
        return flight_mode;
    }

    /**
     *
     * @param flight_mode
     */
    public void setFlight_mode(int flight_mode) {
        this.flight_mode = flight_mode;
    }

    /**
     *
     * @return
     */
    public double getLatitud() {
        return latitud;
    }

    /**
     *
     * @param latitud
     */
    public void setLatitud(int latitud) {
        this.latitud = latitud;
    }

    /**
     *
     * @return
     */
    public double getLongitud() {
        return longitud;
    }

    /**
     *
     * @param longitud
     */
    public void setLongitud(int longitud) {
        this.longitud = longitud;
    }

    /**
     *
     * @return
     */
    public double getP() {
        return p;
    }

    /**
     *
     * @param p
     */
    public void setP(int p) {
        this.p = p;
    }

    /**
     *
     * @return
     */
    public double getPhi() {
        return phi;
    }

    /**
     *
     * @param phi
     */
    public void setPhi(int phi) {
        this.phi = phi;
    }

    /**
     *
     * @return
     */
    public double getPsi() {
        return psi;
    }

    /**
     *
     * @param psi
     */
    public void setPsi(int psi) {
        this.psi = psi;
    }

    /**
     *
     * @return
     */
    public double getQ() {
        return q;
    }

    /**
     *
     * @param q
     */
    public void setQ(int q) {
        this.q = q;
    }

    /**
     *
     * @return
     */
    public double getR() {
        return r;
    }

    /**
     *
     * @param r
     */
    public void setR(int r) {
        this.r = r;
    }

    /**
     *
     * @return
     */
    public double getRudder() {
        return rudder;
    }

    /**
     *
     * @param rudder
     */
    public void setRudder(int rudder) {
        this.rudder = rudder;
    }

    /**
     *
     * @return
     */
     public static int getDataPacketSize() {
        return frameSize;
    }
    
    static public int getMessagePacketSize() {
        return 103;
    }

    /**
     *
     * @return
     */
    public double getTheta() {
        return theta;
    }

    /**
     *
     * @param theta
     */
    public void setTheta(int theta) {
        this.theta = theta;
    }

    /**
     *
     * @return
     */
    public double getThrottle() {
        return throttle;
    }

    /**
     *
     * @param throttle
     */
    public void setThrottle(int throttle) {
        this.throttle = throttle;
    }

    //Apurva Badithela: Added accelerometer data:
    
    public double[] getAccel(){
        double [] accel = {accelLF, accelLR,accelCF, accelCR, accelRF, accelRR};
        return accel;        
    }
    
    public void setAccel(double [] accel){
    this.accelLF = accel[0];
    this.accelLR = accel[1];
    this.accelCF = accel[2];
    this.accelCR = accel[3];
    this.accelRF = accel[4];
    this.accelRR= accel[5];    
    }
    
    /**
     *
     * @return
     */
    public double getTime() {
        return time;
    }

    public double getAltRef() {
        return altRef;
    }


    

    /**
     *
     * @param time
     */
    public void setTime(int time) {
        this.time = time;
    }

    public double getGpsSatellites() {
        return gpsSatellites;
    }

    public void setGpsSatellites(double gpsSatellites) {
        this.gpsSatellites = gpsSatellites;
    }

    public double[] getCustomData(){
        return customScopes;
    }
    
    public double[] getCustomParameters(){
        return customParameters;
    }
    
    public Waypoint[] getCustomWaypoints(){
        return customWaypointsDownstream;
    }
    


    private void intArrayToValues(int[] data) {

        if (data[0] == (int) 'U' && data[1] == (int) 'U' && data[2] == (int) 'T') {
            messagaAvaliable=false;
            
            //time=data[3]*16777216+data[4]*65536+data[5]*256+data[6];
            time = ((double) (((((((byte) data[3] << 8) | data[4]) << 8) | data[5]) << 8) | data[6]) * 1.0e-04);
            dataArray[0] = time;

            //distance2waypoint=256*data[7]+data[8];

            //distance2waypoint = (double) ((((byte) data[ 7]) << 8) | data[8]) * (2.441480758e-03);
            //dataArray[1] = distance2waypoint;

            altRef = (double) ((((byte) data[ 7]) << 8) | data[8]) * (2.441480758e-03);
            dataArray[1] = altRef;

            //ias_ref=256*data[9]+data[10];
            ias_ref = (double) ((((byte) data[ 9]) << 8) | data[10]) * (2.441480758e-03);

            dataArray[2] = ias_ref;
            //p=256*data[11]+data[12];
            p = (double) ((((byte) data[11]) << 8) | data[12]) * 1.065264436e-04;
            dataArray[3] = p;
            //q=256*data[13]+data[14];
            q = (double) ((((byte) data[13]) << 8) | data[14]) * 1.065264436e-04;
            dataArray[4] = q;
            //r=256*data[15]+data[16];
            r = (double) ((((byte) data[15]) << 8) | data[16]) * 1.065264436e-04;
            dataArray[5] = r;
            //altitude=(10000*(256*data[17]+data[18]))/65535;
            altitude = (double) ((((byte) data[17]) << 8) | data[18]) * 3.0517578125e-01;
            dataArray[6] = altitude;
            //IAS=(80*(256*data[19]+data[20]))/65535;
            IAS = (double) ((((byte) data[19]) << 8) | data[20]) * 2.4414062500e-03;
            dataArray[7] = IAS;
            //psi=256*data[21]+data[22];
            psi = (double) ((((byte) data[21]) << 8) | data[22]) * 5.4931640625e-03;
            dataArray[8] = psi;

            theta = (double) ((((byte) data[23]) << 8) | data[24]) * 2.7465820313e-03;
            dataArray[9] = theta;

            //phi=(256*data[25]+data[26])/1000;
            phi = (double) ((((byte) data[25]) << 8) | data[26]) * 5.4931640625e-03;
            dataArray[10] = phi;


            //rudder=256*data[27]+data[28];
            aileron = (double) ((((byte) data[27]) << 8) | data[28]) * (1.220740379e-03);
            dataArray[11] = aileron;

            //elevator=256*data[29]+data[30];
            elevator = (double) ((((byte) data[29]) << 8) | data[30]) * (1.220740379e-03);
            dataArray[12] = elevator;
            //throttle=256*data[31]+data[32];
            throttle = (double) ((((byte) data[31]) << 8) | data[32]) * (3.051850947599719e-05);
            dataArray[13] = throttle;

            //aileron=256*data[33]+data[34];
            rudder = (double) ((((byte) data[33]) << 8) | data[34]) * (1.220740379e-03);
            dataArray[14] = rudder;

            cpuload = (double) ((((byte) data[35]) << 8) | data[36]);
            dataArray[15] = cpuload;

            //longitud=data[37]*16777216+data[38]*65536+data[39]*256+data[40];
            longitud = (double) (((((((byte) data[37] << 8) | data[38]) << 8) | data[39]) << 8) | data[40]) * 1.0e-07;
            dataArray[16] = longitud;

            //latitud=data[41]*16777216+data[42]*65536+data[43]*256+data[44];
            latitud = (double) (((((((byte) data[41] << 8) | data[42]) << 8) | data[43]) << 8) | data[44]) * 1.0e-07;
            dataArray[17] = latitud;

            

            //flight_mode=256*data[45]+data[46];
            flight_mode = (double) ((((byte) data[45]) << 8) | data[46]);
            dataArray[18] = flight_mode;

            //nextwaypoint=256*data[47]+data[48];
            gpsSatellites = (double) ((((byte) data[47]) << 8) | data[48]);
            dataArray[19] = gpsSatellites;
            
            accelLF = (double)((((byte)data[49])<<8)|data[50]);
            dataArray[20]= accelLF;
            
            accelLR = (double)((((byte)data[51])<<8)|data[52]);
            dataArray[21]= accelLR;
            
            accelCF = (double)((((byte)data[53])<<8)|data[54]);
            dataArray[22]= accelCF;
            
            accelCR = (double)((((byte)data[55])<<8)|data[56]);
            dataArray[23]= accelCR;
            
            accelRF = (double)((((byte)data[57])<<8)|data[58]);
            dataArray[24]= accelRF;
            
            accelRR = (double)((((byte)data[59])<<8)|data[60]);
            dataArray[25]= accelRR;
            int dataOffset = 49;
            int dataArrayOffset = 20;
            for(int i = 0; i < customDataCount; ++i)
            {   
                int value = 0;
                value |= data[dataOffset + 4*i] << 24;
                value |= data[dataOffset + 4*i+1] << 16;
                value |= data[dataOffset + 4*i+2] << 8;
                value |= data[dataOffset + 4*i+3];                
                customScopes[i] = Float.intBitsToFloat(value);                             
                dataArray[dataArrayOffset+i] = customScopes[i];
            }
            dataOffset += customScopesSize;
            dataArrayOffset += customScopesSize/Float.BYTES;
            for(int i = 0; i < customParameterCount; ++i)
            {   
                int value = 0;
                value |= data[dataOffset + 4*i] << 24;
                value |= data[dataOffset + 4*i+1] << 16;
                value |= data[dataOffset + 4*i+2] << 8;
                value |= data[dataOffset + 4*i+3];                 
                customParameters[i] = Float.intBitsToFloat(value); 
                dataArray[dataArrayOffset+i] = customParameters[i];
            }           
            dataOffset += customParameterSize;
            dataArrayOffset += customParameterSize/Float.BYTES;
            
            for(int i = 0; i < customWaypointsDownstreamCount; ++i){
                int id = data[dataOffset];                
                dataOffset++;
                customWaypointsDownstream[i] = new Waypoint();
                customWaypointsDownstream[i].setID(id);
                for(int j = 0; j < 3; ++j){
                    int value = 0;                    
                    value |= data[dataOffset + 4*j] << 24;
                    value |= data[dataOffset + 4*j+1] << 16;
                    value |= data[dataOffset + 4*j+2] << 8;
                    value |= data[dataOffset + 4*j+3];    
                    double dValue;
                    if(j < 2){
                        dValue= (double)value / 1e7;                      
                    } else {
                        dValue = Float.intBitsToFloat(value);                  
                    }
                    dataArray[dataArrayOffset+3*i+j] = dValue;
                    switch(j){
                        case 0:                            
                            customWaypointsDownstream[i].setLongitude(dValue);
                            break;
                        case 1:
                            customWaypointsDownstream[i].setLatitude(dValue);
                            break;
                        case 2:
                            customWaypointsDownstream[i].setAltitude((float) dValue);
                            break;
                        default:
                            assert(false);
                    } 
                }              
                dataOffset += 12;
            }                        
        }

        else{
            message=bytesToString(data,3,data.length-1);
            messagaAvaliable=true;
            
        }



    }

    private void stringToIntArray(String st) {
        //System.out.println("Data lenght = "+st.length());
        dataIntArray = new int[st.length()];
        for (int i = 0; i < st.length(); i++) {
            dataIntArray[i] = (int) st.charAt(i);
        }


    }
    
    private byte[] hmac_sha1(byte[] key, byte[] data){
        String algorithmName = "HmacSHA2";
        SecretKeySpec keyspec = new SecretKeySpec(key, algorithmName);
        Mac mac = null;
        try {
            mac = Mac.getInstance(algorithmName);
            if(mac != null)
                mac.init(keyspec);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(DataPacket.class.getName()).log(Level.SEVERE, null, ex);
        }catch (InvalidKeyException ex) {
            Logger.getLogger(DataPacket.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(mac != null)            
            return mac.doFinal(data);  
        else
            return null;
    }

    private void doCheckSum() {
        long sum = 0;
        //System.out.println("len "+dataIntArray.length);
        for (int i = 3; i < frameSize - 2; i++) {
            sum = sum + dataIntArray[i];
           
        }
        //long sum2 = 256*dataIntArray[frameSize - 2] + dataIntArray[frameSize - 1];
        long sum2 = (long) ((((byte) dataIntArray[frameSize-2]) << 8) | dataIntArray[frameSize-1]) ;
        if (sum2 == sum) {
            checkSum = true;
        } else {
            checkSum = false;
            System.out.println("sum= " + sum + " sum2= " + sum2 + " , lenght= " + dataIntArray.length);
        }
    }

    /**
     * 
     * @return
     */
    public boolean isCheckSum() {
        return checkSum;

    }

    /**
     *
     * @return
     */
    public double[] getDataArray() {
        return dataArray;
    }

    /**
     *
     * @param dataArray
     */
    public void setDataArray(double[] dataArray) {
        this.dataArray = dataArray;
    }

    private String bytesToString(int[] data, int pos_i, int pos_f) {
        String st="";
        for(int i=pos_i; i<=pos_f;i++){
            st=st+(char)data[i];
        }
        return st;
    }

    public boolean isMessagaAvaliable() {
        return messagaAvaliable;
    }

    public String getMessage() {
        messagaAvaliable = false ; 
        return message;

    }



    

}
