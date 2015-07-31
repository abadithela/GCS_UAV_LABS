/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package data;

/**
 *
 * @author dernehl
 */
public class Waypoint {
    
    public Waypoint(final double latitude, final double longitude, final int altitude){
        lat = latitude;
        lon = longitude;
        alt = altitude;
    }

    Waypoint() {   
        id = -1;
    }
    
    public int getID(){
        return id;
    }
    
    public double getLatitude(){
        return lat;
    }
    
    public double getLongitude(){
        return lon;
    }
    
    public float getAltitude(){
        return alt;
    }
    
    public void setLatitude(final double latitude){
        lat = latitude;
    }
    
    public void setLongitude(final double longitude){
        lon = longitude;
    }
    
    public void setAltitude(final float altitude){
        alt = altitude;
    }
    
    public void setID(final int id){
        this.id = id;
    }
    
    private double lat;
    private double lon;
    private float alt;
    private int id;
    
}
