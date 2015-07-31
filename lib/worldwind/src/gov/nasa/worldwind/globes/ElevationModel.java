/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.globes;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.geom.*;

import java.util.List;

/**
 * <p/>
 * Provides the elevations a {@link Globe} or other object holding elevations.
 * <p/>
 * An <code>ElevationModel</code> often approximates elevations at multiple levels of spatial resolution. For any given
 * viewing position the model determines an appropriate target resolution. That target resolution may not be immediately
 * achievable, however, because the corresponding elevation data might not be locally available and must be retrieved
 * from a remote location. When this is the case, the <code>Elevations</code> object returned for a sector holds the
 * resolution achievable with the data currently available. That resolution may not be the same as the target
 * resolution. The achieved resolution is made available in the interface.
 * <p/>
 *
 * @author Tom Gaskins
 * @version $Id: ElevationModel.java 11537 2009-06-10 05:55:40Z tgaskins $
 */
public interface ElevationModel extends WWObject, Restorable
{
    /**
     * Returns the elevation model's name, as specified in the most recent call to {@link #setName}.
     *
     * @return the elevation model's name.
     */
    String getName();

    /**
     * Set the elevation model's name. The name is a convenience attribute typically used to identify the elevation
     * model in user interfaces. By default, an elevation model has no name.
     *
     * @param name the name to assign to the elevation model.
     */
    void setName(String name);

    /**
     * Indicates whether the elevation model is allowed to retrieve data from the network. Some elevation models have no
     * need to retrieve data from the network. This state is meaningless for such elevation models.
     *
     * @return <code>true</code> if the elevation model is enabled to retrieve network data, else <code>false</code>.
     */
    boolean isNetworkRetrievalEnabled();

    /**
     * Controls whether the elevation model is allowed to retrieve data from the network. Some elevation models have no
     * need for data from the network. This state may be set but is meaningless for such elevation models.
     *
     * @param networkRetrievalEnabled <code>true</code> if network retrieval is allowed, else <code>false</code>.
     */
    void setNetworkRetrievalEnabled(boolean networkRetrievalEnabled);

    /**
     * Returns the current expiry time.
     *
     * @return the current expiry time.
     */
    long getExpiryTime();

    /**
     * Specifies the time of the elevation model's most recent dataset update. If greater than zero, the model ignores
     * and eliminates any previously cached data older than the time specfied, and requests new information from the
     * data source. If zero, the model uses any expiry times intrinsic to the model, typically initialized at model
     * construction. The default expiry time is 0, thereby enabling a model's intrinsic expiration criteria.
     *
     * @param expiryTime the expiry time of any cached data, expressed as a number of milliseconds beyond the epoch.
     *
     * @see System#currentTimeMillis() for a description of milliseconds beyond the epoch.
     */
    void setExpiryTime(long expiryTime);

    /**
     * Specifies the value used to identify missing data in an elevation model. Data entries with this value are treated
     * as undefined. The sentinel value is often specified by the metadata of the data set, in which case the elevation
     * model will define that value to be the missing-data sentinel. When ti's not specified in the metadata, the
     * application may specify it via this method.
     *
     * @param flag a reference to the sentinel value. The default value is null, indicating that there is no
     *             missing-data sentinel and all data entry values are considered valid.
     */
    void setMissingDataSignal(double flag);

    /**
     * Returns the current missing-data sentinel.
     *
     * @return the missing-data sentinel, or null if no sentinel has been specified by either the application or the
     *         data set.
     */
    double getMissingDataSignal();

    /**
     * Indicates whether the elevation model corresponds to a specified sector.
     *
     * @param sector the sector in question.
     *
     * @return 0 if the elevation model fully contains the sector, 1 if the elevation model intersects the sector but
     *         does not fully contain it, or -1 if the elevation does not intersect the elevation model.
     */
    int intersects(Sector sector);

    /**
     * Indicates whether a specified location is within the elevation model's domain.
     *
     * @param latitude  the latitude of the location in question.
     * @param longitude the longitude of the location in question.
     *
     * @return true if the location is within the elevation model's domain, otherwise false.
     */
    boolean contains(Angle latitude, Angle longitude);

    /**
     * Returns the maximum elevation contained in the elevevation model. When the elevation model is associated with a
     * globe, this value is the elevation of the highest point on the globe.
     *
     * @return The maximum elevation of the elevation model.
     */
    double getMaxElevation();

    /**
     * Returns the minimum elevation contained in the elevevation model. When associated with a globe, this value is the
     * elevation of the lowest point on the globe. It may be negative, indicating a value below mean surface level. (Sea
     * level in the case of Earth.)
     *
     * @return The minimum elevation of the model.
     */
    double getMinElevation();

    /**
     * Returns the minimum and maximum elevations at a specified location.
     *
     * @param latitude  the latitude of the location in question.
     * @param longitude the longitude of the location in question.
     *
     * @return A two-element <code>double</code> array indicating the minimum and maximum elevations at the specified
     *         location, respectively. These values are the global minimum and maximum if the local minimum and maximum
     *         values are currently unknown.
     */
    double[] getExtremeElevations(Angle latitude, Angle longitude);

    /**
     * Returns the minimum and maximum elevations within a specified sector of the elevation model.
     *
     * @param sector the sector in question.
     *
     * @return A two-element <code>double</code> array indicating the sector's minimum and maximum elevations,
     *         respectively. These elements are the global minimum and maximum if the local minimum and maximum values
     *         are currently unknown.
     */
    double[] getExtremeElevations(Sector sector);

    /**
     * Indicates the best resolution attainable for a specified sector.
     *
     * @param sector the sector in question. If null, the elevation model's best overall resolution is returned. This is
     *               the best attainable at <em>some</> locations but not necessarily at all locations.
     *
     * @return the best resolution attainable for the specified sector, in radians, or {@link Double#MAX_VALUE} if the
     *         sector does not intersect the elevation model.
     */
    double getBestResolution(Sector sector);

    /**
     * Returns the detail hint associated with the specified sector. If the elevation model does not have any detail
     * hint for the sector, this will return zero.
     *
     * @param sector the sector in question.
     *
     * @return The detail hint corresponding to the specified sector.
     *
     * @throws IllegalArgumentException if <code>sector</code> is null.
     */
    double getDetailHint(Sector sector);

    /**
     * Returns the elevation at a specified location, or an unspecified value, typically zero, if an elevation cannot be
     * determined.
     *
     * @param latitude  the latitude of the location in question.
     * @param longitude the longitude of the location in question.
     *
     * @return The elevation corresponding to the specified location, or an elevation-model specific value if the
     *         elevation does not contain a value for the location or a value cannot be determined. (The value returned
     *         is typically zero, although the elevation model may choose to return some other value.)
     *
     * @see #setMissingDataSignal(double)
     */
    double getElevation(Angle latitude, Angle longitude);

    /**
     * Returns the elevation at a specified location, but without mapping missing data to the elevation model's missing
     * data replacement value. In the case of missing data values, the missing data signal is returned rather than the
     * missing data replacement value. *
     *
     * @param latitude  the latitude of the location for which to return the elevation.
     * @param longitude the longitude of the location for which to return the elevation.
     *
     * @return the elevation at the specified location, or the elevation model's missing data signal. If no data is
     *         currently available for the location, the elevation model's global minimum elevation at that location is
     *         returned.
     */
    double getUnmappedElevation(Angle latitude, Angle longitude);

    /**
     * Returns the elevations of a collection of locations.
     * <p/>
     * The missing-data sentinel value, if previously specified, is returned for locations at which an elevation cannot
     * be determined. if a missing-data value has not been specified, an elevation-model specific value, typically zero,
     * is returned.
     *
     * @param sector           the sector in question.
     * @param latlons          the locations to return elevations for. A value of zero is returned if a location is
     *                         null.
     * @param targetResolution the desired horizontal resolution, in radians, of the raster or other elevation sample
     *                         from which elevations are drawn. (To compute radians from a distance, divide the distance
     *                         by the radius of the globe, ensuring that both the distance and the radius are in the
     *                         same units.)
     * @param buffer           an array in which to place the returned elevations. The array must be pre-allocated and
     *                         contain at least as many elements as the list of locations.
     *
     * @return the resolution achieved, in radians, or {@link Double.MAX_VALUE} if individual elevations cannot be
     *         determined for any of the locations.
     *
     * @see #setMissingValueSentinel(Double)
     */
    @SuppressWarnings({"JavadocReference"})
    double getElevations(Sector sector, List<? extends LatLon> latlons, double targetResolution, double[] buffer);

    void composeElevations(Sector sector, List<? extends LatLon> latlons, int tileWidth, double[] buffer)
        throws Exception;
}
