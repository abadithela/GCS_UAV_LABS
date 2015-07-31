/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package des.map;

/**
 *
 * @author dernehl
 */
public interface PanelDrawingMapListener {
    void centerChanged();
    void zoomChanged();
    void imageSaved(int currentZoom, int totalImagesAtThisZoom, int numSavedImagesAtThisZoom);
    void newWaypoint(double latitude, double longitude);
    void allImagesSaved();
}
