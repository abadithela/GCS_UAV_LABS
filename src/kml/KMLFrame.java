/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package kml;

/**
 *
 * @author david
 */
public class KMLFrame {

    private String KMLString ="";

    public static final String linePart1="" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n"+
                "<kml xmlns=\"http://www.opengis.net/kml/2.2\"> \n"+
                    "<Document> \n"+
                        "<name>Generated KML</name> \n"+
                            "<Style id=\"yellowLineGreenPoly\"> \n"+
                                "<LineStyle> \n"+
                                "<color>7f00ffff</color> \n"+
                                "<width>4</width> \n"+
                                      "</LineStyle> \n"+
                                      "<PolyStyle> \n"+
                                        "<color>7f00ff00</color> \n"+
                                      "</PolyStyle> \n"+
                                    "</Style> \n"+
                                    "<Placemark> \n"+
                                      "<name>Absolute Extruded</name> \n"+
                                      "<visibility>1</visibility> \n"+
                                      "<description>Transparent green wall with yellow outlines</description> \n"+
                                      "<styleUrl>#yellowLineGreenPoly</styleUrl> \n"+
                                      "<LineString> \n"+
                                        "<extrude>1</extrude> \n"+
                                        "<tessellate>1</tessellate> \n"+
                                        "<altitudeMode>absolute</altitudeMode> \n"+
                                        "<coordinates> \n";
    public String linePart2="";

    public static final String linePart3= " " +
            "</coordinates> \n"+
           "</LineString> \n"+
        "</Placemark> \n"+
     "</Document> \n"+
    "</kml> ";

    
    public void setPoint(double lon,double lat, double alt){
        linePart2=linePart2+lon+","+lat+","+alt+"\n";
    }

    public  String getKMLLineString(double lat, double lon, double alt) {

        setPoint(lat, lon, alt);
        return KMLString=linePart1+linePart2+linePart3;

    }
    
}