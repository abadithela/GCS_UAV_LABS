/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.geom;

import gov.nasa.worldwind.util.Logging;

/**
 * @author tag
 * @version $Id: Position.java 8912 2009-02-19 18:09:10Z tgaskins $
 */
public class Position extends LatLon
{
    public static final Position ZERO = new Position(Angle.ZERO, Angle.ZERO, 0d);

    public final double elevation;

    public static Position fromRadians(double latitude, double longitude, double elevation)
    {
        return new Position(Angle.fromRadians(latitude), Angle.fromRadians(longitude), elevation);
    }

    public static Position fromDegrees(double latitude, double longitude, double elevation)
    {
        return new Position(Angle.fromDegrees(latitude), Angle.fromDegrees(longitude), elevation);
    }

    public Position(Angle latitude, Angle longitude, double elevation)
    {
        super(latitude, longitude);
        this.elevation = elevation;
    }

    public Position(LatLon latLon, double elevation)
    {
        super(latLon);
        this.elevation = elevation;
    }

    /**
     * Obtains the elevation of this position
     *
     * @return this position's elevation
     */
    public final double getElevation()
    {
        return this.elevation;
    }

    public final LatLon getLatLon()
    {
        return new LatLon(this);
    }

    public Position add(Position that)
    {
        Angle lat = Angle.normalizedLatitude(this.latitude.add(that.latitude));
        Angle lon = Angle.normalizedLongitude(this.longitude.add(that.longitude));

        return new Position(lat, lon, this.elevation + that.elevation);
    }

    public Position subtract(Position that)
    {
        Angle lat = Angle.normalizedLatitude(this.latitude.subtract(that.latitude));
        Angle lon = Angle.normalizedLongitude(this.longitude.subtract(that.longitude));

        return new Position(lat, lon, this.elevation - that.elevation);
    }

    public static Position interpolate(double amount, Position value1, Position value2)
    {
        if (value1 == null || value2 == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (amount < 0)
            return value1;
        else if (amount > 1)
            return value2;

        LatLon latLon = LatLon.interpolate(amount,value1,value2);

        return new Position(latLon, amount * value2.getElevation() + (1 - amount) * value1.getElevation());
    }

    public static boolean positionsCrossDateLine(Iterable<? extends Position> positions)
    {
        if (positions == null)
        {
            String msg = Logging.getMessage("nullValue.PositionsListIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Position pos = null;
        for (Position posNext : positions)
        {
            if (pos != null)
            {
                // A segment cross the line if end pos have different longitude signs
                // and are more than 180 degress longitude apart
                if (Math.signum(pos.getLongitude().degrees) != Math.signum(posNext.getLongitude().degrees))
                {
                    double delta = Math.abs(pos.getLongitude().degrees - posNext.getLongitude().degrees);
                    if (delta > 180 && delta < 360)
                        return true;
                }
            }
            pos = posNext;
        }

        return false;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        Position position = (Position) o;

        //noinspection RedundantIfStatement
        if (Double.compare(position.elevation, elevation) != 0)
            return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        long temp;
        temp = elevation != +0.0d ? Double.doubleToLongBits(elevation) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public String toString()
    {
        return "(" + this.latitude.toString() + ", " + this.longitude.toString() + ", " + this.elevation + ")";
    }
}
