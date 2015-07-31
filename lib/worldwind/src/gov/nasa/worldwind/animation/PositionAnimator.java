/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.animation;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.*;

/**
 * @author jym
 * @version $Id: PositionAnimator.java 12631 2009-09-22 02:23:09Z jterhorst $
 */
public class PositionAnimator extends BasicAnimator
{
    
    protected Position begin;
    protected Position end;
    protected final PropertyAccessor.PositionAccessor propertyAccessor;

    public PositionAnimator(
        Interpolator interpolator,
        Position begin,
        Position end,
        PropertyAccessor.PositionAccessor propertyAccessor)
    {
        super(interpolator);
        if (interpolator == null)
        {
           this.interpolator = new ScheduledInterpolator(10000);
        }
        if (begin == null || end == null)
        {
           String message = Logging.getMessage("nullValue.PositionIsNull");
           Logging.logger().severe(message);
           throw new IllegalArgumentException(message);
        }
        if (propertyAccessor == null)
        {
           String message = Logging.getMessage("nullValue.ViewPropertyAccessorIsNull");
           Logging.logger().severe(message);
           throw new IllegalArgumentException(message);
        }

        this.begin = begin;
        this.end = end;
        this.propertyAccessor = propertyAccessor;
    }
    
    public void setBegin(Position begin)
    {
        this.begin = begin;
    }

    public void setEnd(Position end)
    {
        this.end = end;
    }

    public Position getBegin()
    {
        return this.begin;
    }

    public Position getEnd()
    {
        return this.end;
    }

    public PropertyAccessor.PositionAccessor getPropertyAccessor()
    {
        return this.propertyAccessor;
    }

    protected void setImpl(double interpolant)
    {
        Position newValue = this.nextPosition(interpolant);
        if (newValue == null)
           return;

        boolean success = this.propertyAccessor.setPosition(newValue);
        if (!success)
        {
           this.flagLastStateInvalid();
        }
        if (interpolant >= 1)
        {
            this.stop();
        }
    }

    protected Position nextPosition(double interpolant)
    {


        Angle azimuth = LatLon.greatCircleAzimuth(this.begin, this.end);
        Angle distance = LatLon.greatCircleDistance(this.begin, this.end);
        Angle pathLength = Angle.fromDegrees(distance.degrees * interpolant);
        LatLon nextLatLon = LatLon.greatCircleEndPosition(this.begin, azimuth, pathLength);
        double nextElevation = (1 - interpolant) * this.begin.getElevation() +  interpolant * this.end.getElevation();

        return new Position(nextLatLon, nextElevation); 
    }
}
