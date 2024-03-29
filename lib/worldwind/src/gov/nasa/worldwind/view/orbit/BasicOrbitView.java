/*
Copyright (C) 2001, 2007 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.view.orbit;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.view.*;

import javax.media.opengl.GL;

/**
 * @author dcollins
 * @version $Id: BasicOrbitView.java 10301 2009-04-17 20:21:46Z dcollins $
 */
public class BasicOrbitView extends BasicView implements OrbitView
{

    protected Position center = Position.ZERO;
    protected double zoom;
    private boolean viewOutOfFocus;
    // Stateless helper classes.
    protected final OrbitViewCollisionSupport collisionSupport = new OrbitViewCollisionSupport();

    public BasicOrbitView()
    {

        this.viewInputHandler = new OrbitViewInputHandler();
        this.viewLimits = new BasicOrbitViewLimits();
        this.collisionSupport.setCollisionThreshold(COLLISION_THRESHOLD);
        this.collisionSupport.setNumIterations(COLLISION_NUM_ITERATIONS);
        loadConfigurationValues();
    }

    private void loadConfigurationValues()
    {
        Double initLat = Configuration.getDoubleValue(AVKey.INITIAL_LATITUDE);
        Double initLon = Configuration.getDoubleValue(AVKey.INITIAL_LONGITUDE);
        double initElev = this.center.getElevation();
        // Set center latitude and longitude. Do not change center elevation.
        if (initLat != null && initLon != null)
            setCenterPosition(Position.fromDegrees(initLat, initLon, initElev));
        // Set only center latitude. Do not change center longitude or center elevation.
        else if (initLat != null)
            setCenterPosition(Position.fromDegrees(initLat, this.center.getLongitude().degrees, initElev));
        // Set only center longitude. Do not center latitude or center elevation.
        else if (initLon != null)
            setCenterPosition(Position.fromDegrees(this.center.getLatitude().degrees, initLon, initElev));

        Double initHeading = Configuration.getDoubleValue(AVKey.INITIAL_HEADING);
        if (initHeading != null)
            setHeading(Angle.fromDegrees(initHeading));

        Double initPitch = Configuration.getDoubleValue(AVKey.INITIAL_PITCH);
        if (initPitch != null)
            setPitch(Angle.fromDegrees(initPitch));

        Double initAltitude = Configuration.getDoubleValue(AVKey.INITIAL_ALTITUDE);
        if (initAltitude != null)
            setZoom(initAltitude);

        Double initFov = Configuration.getDoubleValue(AVKey.FOV);
        if (initFov != null)
            setFieldOfView(Angle.fromDegrees(initFov));

        this.setViewOutOfFocus(true);
    }

    // TODO
    // Separate control should be provided over whether the View will detect collisions
    // which reports through hadCollisions() and flagHadCollisions(),
    // and whether the View will resolve collisions itself,
    // something along the lines of isResolveCollisions.
    // At the same time, flagHadCollisions() should be made part of the public View interface.
    protected void flagHadCollisions()
    {
        this.hadCollisions = true;
    }

    public void stopMovementOnCenter()
    {
        firePropertyChange(CENTER_STOPPED, null, null);
    }

    public void copyViewState(View view)
    {
        this.globe = view.getGlobe();
        Vec4 center = view.getCenterPoint();
        if (center == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        setOrientation(view.getEyePosition(), globe.computePositionFromPoint(center));
    }

    public Position getCenterPosition()
    {
        return this.center;
    }

    public Vec4 getCenterPoint()
    {
        return(globe.computePointFromPosition(
            this.center));
    }

    public void setCenterPosition(Position center)
    {
        if (center == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (center.getLatitude().degrees < -90 || center.getLatitude().degrees > 90)
        {
            String message = Logging.getMessage("generic.LatitudeOutOfRange", center.getLatitude());
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.center = normalizedCenterPosition(center);
        this.center = BasicOrbitViewLimits.limitCenterPosition(this.center, this.getOrbitViewLimits());
        resolveCollisionsWithCenterPosition();
    }


    public void setHeading(Angle heading)
    {
        if (heading == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.heading = normalizedHeading(heading);
        this.heading = BasicOrbitViewLimits.limitHeading(this.heading, this.getOrbitViewLimits());
        resolveCollisionsWithPitch();
    }



    public void setPitch(Angle pitch)
    {
        if (pitch == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.pitch = normalizedPitch(pitch);
        this.pitch = BasicOrbitViewLimits.limitPitch(this.pitch, this.getOrbitViewLimits());
        resolveCollisionsWithPitch();
    }

    public double getZoom()
    {
        return this.zoom;
    }

    public void setZoom(double zoom)
    {
        if (zoom < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", zoom);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.zoom = zoom;
        this.zoom = BasicOrbitViewLimits.limitZoom(this.zoom, this.getOrbitViewLimits());
        resolveCollisionsWithCenterPosition();
    }

    /**
     * Returns the <code>OrbitViewLimits</code> that apply to this <code>OrbitView</code>. Incoming parameters to the
     * methods setCenterPosition, setHeading, setPitch, or setZoom are be limited by the parameters defined in this
     * <code>OrbitViewLimits</code>.
     *
     * @return the <code>OrbitViewLimits</code> that apply to this <code>OrbitView</code>
     */
    public OrbitViewLimits getOrbitViewLimits()
    {
        return (OrbitViewLimits) viewLimits;
    }

    /**
     * Sets the <code>OrbitViewLimits</code> that will apply to this <code>OrbitView</code>. Incoming parameters to the
     * methods setCenterPosition, setHeading, setPitch, or setZoom will be limited by the parameters defined in
     * <code>viewLimits</code>.
     *
     * @param viewLimits the <code>OrbitViewLimits</code> that will apply to this <code>OrbitView</code>.
     * @throws IllegalArgumentException if <code>viewLimits</code> is null.
     */
    public void setOrbitViewLimits(OrbitViewLimits viewLimits)
    {
        if (viewLimits == null)
        {
            String message = Logging.getMessage("nullValue.ViewLimitsIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.viewLimits = viewLimits;
    }

    public static Position normalizedCenterPosition(Position unnormalizedPosition)
    {
        if (unnormalizedPosition == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return new Position(
                Angle.normalizedLatitude(unnormalizedPosition.getLatitude()),
                Angle.normalizedLongitude(unnormalizedPosition.getLongitude()),
                unnormalizedPosition.getElevation());
    }

    public static Angle normalizedHeading(Angle unnormalizedHeading)
    {
        if (unnormalizedHeading == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double degrees = unnormalizedHeading.degrees;
        double heading = degrees % 360;
        return Angle.fromDegrees(heading > 180 ? heading - 360 : (heading < -180 ? 360 + heading : heading));
    }

    public static Angle normalizedPitch(Angle unnormalizedPitch)
    {
        if (unnormalizedPitch == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Normalize pitch to the range [-180, 180].
        double degrees = unnormalizedPitch.degrees;
        double pitch = degrees % 360;
        return Angle.fromDegrees(pitch > 180 ? pitch - 360 : (pitch < -180 ? 360 + pitch : pitch));
    }

    private void resolveCollisionsWithCenterPosition()
    {
        if (this.dc == null)
            return;

        if (!isDetectCollisions())
            return;


        // If there is no collision, 'newCenterPosition' will be null. Otherwise it will contain a value
        // that will resolve the collision.
        double nearDistance = this.computeNearDistance(this.getCurrentEyePosition());
        Position newCenter = this.collisionSupport.computeCenterPositionToResolveCollision(this, nearDistance, this.dc);
        if (newCenter != null && newCenter.getLatitude().degrees >= -90 && newCenter.getLongitude().degrees <= 90)
        {
            this.center = newCenter;
            flagHadCollisions();
        }
    }

    protected void resolveCollisionsWithPitch()
    {
        if (this.dc == null)
            return;

        if (!isDetectCollisions())
            return;

        // Compute the near distance corresponding to the current set of values.
        // If there is no collision, 'newPitch' will be null. Otherwise it will contain a value
        // that will resolve the collision.
        double nearDistance = this.computeNearDistance(this.getCurrentEyePosition());
        Angle newPitch = this.collisionSupport.computePitchToResolveCollision(this, nearDistance, this.dc);
        if (newPitch != null && newPitch.degrees <= 90 && newPitch.degrees >= 0)
        {
            this.pitch = newPitch;
            flagHadCollisions();
        }
    }

    /**
     * computes and sets the center of rotation for heading and pitch changes, if it is needed.
     */
    public void computeAndSetViewCenterIfNeeded()
    {
        if (this.viewOutOfFocus)
        {
            computeAndSetViewCenter();
        }
    }

    /**
     * computes and sets the center of rotation for heading and pitch changes.
     */
    public void computeAndSetViewCenter()
    {
        try
        {
            // Update the View's focus.
            if (this.canFocusOnViewportCenter())
            {
                this.focusOnViewportCenter();
                this.setViewOutOfFocus(false);
            }
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("generic.ExceptionWhileChangingView");
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            // If updating the View's focus failed, raise the flag again.
            this.setViewOutOfFocus(true);
        }
    }

    /**
     * Alerts the BasicOrbitView that the view requires the point of rotation for heading and pitch changes to be'
     * recalculate.
     * @param b true if the point of rotation needs recalculation, false if it does not.
     */
    public void setViewOutOfFocus(boolean b)
    {
        this.viewOutOfFocus = b;
    }

    /**
     * Determines if the BasicOrbitView can be focused on the viewport center {@link #focusOnViewportCenter}.
     * Focusing on the viewport center requires a non-null {@link gov.nasa.worldwind.globes.Globe},
     * a non-null {@link DrawContext}, and the viewport center is on the terrain.
     * @return true if the BasicOrbitView can focus on the viewport center.
     **/
    public boolean canFocusOnViewportCenter()
    {
        return this.dc != null
            && this.dc.getViewportCenterPosition() != null
            && this.globe != null;
    }

    /**
     * sets the point of rotation for heading and pitch changes to the surface position at the viewport center.
     **/
    public void focusOnViewportCenter()
    {
        if (this.isAnimating())
            return;
        if (this.dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }
        if (this.globe == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        Position viewportCenterPos = this.dc.getViewportCenterPosition();
        if (viewportCenterPos == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextViewportCenterIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        // We want the actual "geometric point" here, which must be adjusted for vertical exaggeration.
        Vec4 viewportCenterPoint = this.globe.computePointFromPosition(
            viewportCenterPos.getLatitude(), viewportCenterPos.getLongitude(),
            this.globe.getElevation(viewportCenterPos.getLatitude(), viewportCenterPos.getLongitude())
                    * dc.getVerticalExaggeration());

        if (viewportCenterPoint != null)
        {
            Matrix modelview = OrbitViewInputSupport.computeTransformMatrix(this.globe,
                    this.center, this.heading, this.pitch, this.zoom);
            if (modelview != null)
            {
                Matrix modelviewInv = modelview.getInverse();
                if (modelviewInv != null)
                {
                    // The change in focus must happen seamlessly; we can't move the eye or the forward vector
                    // (only the center position and zoom should change). Therefore we pick a point along the
                    // forward vector, and *near* the viewportCenterPoint, but not necessarily at the
                    // viewportCenterPoint itself.
                    Vec4 eyePoint = Vec4.UNIT_W.transformBy4(modelviewInv);
                    Vec4 forward = Vec4.UNIT_NEGATIVE_Z.transformBy4(modelviewInv);
                    double distance = eyePoint.distanceTo3(viewportCenterPoint);
                    Vec4 newCenterPoint = Vec4.fromLine3(eyePoint, distance, forward);

                    OrbitViewInputSupport.OrbitViewState modelCoords = OrbitViewInputSupport.computeOrbitViewState(
                        this.globe, modelview, newCenterPoint);
                    if (validateModelCoordinates(modelCoords))
                    {
                        setModelCoordinates(modelCoords);
                    }
                }
            }
        }
    }

    public boolean canFocusOnTerrainCenter()
    {
        return this.dc != null
            && this.dc.getSurfaceGeometry() != null
            && this.globe != null;
    }

    public void focusOnTerrainCenter()
    {
        if (this.dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }
        if (this.globe == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (this.dc.getSurfaceGeometry() == null)
        {
            return;
        }
        if (isAnimating())
            return;

        Matrix modelview = OrbitViewInputSupport.computeTransformMatrix(this.globe,
                this.center, this.heading, this.pitch, this.zoom);
        if (modelview != null)
        {
            Matrix modelviewInv = modelview.getInverse();
            if (modelviewInv != null)
            {
                // The change in focus must happen seamlessly; we can't move the eye or the forward vector
                // (only the center position and zoom should change). 
                
                Vec4 eyePoint = Vec4.UNIT_W.transformBy4(modelviewInv);
                Vec4 forward = Vec4.UNIT_NEGATIVE_Z.transformBy4(modelviewInv);
                Intersection[] intersections = this.dc.getSurfaceGeometry().intersect(new Line(eyePoint, forward));
                if (intersections != null && intersections.length > 0)
                {
                    Vec4 viewportCenterPoint = intersections[0].getIntersectionPoint();
                    OrbitViewInputSupport.OrbitViewState modelCoords = OrbitViewInputSupport.computeOrbitViewState(
                        this.globe, modelview, viewportCenterPoint);
                    if (validateModelCoordinates(modelCoords))
                    {
                        setModelCoordinates(modelCoords);
                    }
                }
            }
        }
    }

    public Position getEyePosition()
    {
        if (this.lastEyePosition == null)
            this.lastEyePosition = computeEyePositionFromModelview();
        return this.lastEyePosition;
    }

    public void setEyePosition(Position eyePosition)
    {
        if (eyePosition == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double elevation = eyePosition.getElevation();

        // Set the center lat/lon to the eye lat/lon. Set the center elevation to zero if the eye elevation is >= 0.
        // Set the center elevation to the eye elevation if the eye elevation is < 0.
        this.center = new Position(eyePosition, elevation >= 0 ? 0 : elevation);
        this.heading = Angle.ZERO;
        this.pitch = Angle.ZERO;
        // If the eye elevation is >= 0, zoom gets the eye elevation. If the eye elevation < 0, zoom gets 0.
        this.zoom = elevation >= 0 ? elevation : 0;

        resolveCollisionsWithCenterPosition();
    }

    public Vec4 getCurrentEyePoint()
    {
        if (this.globe != null)
        {
            Matrix modelview = OrbitViewInputSupport.computeTransformMatrix(this.globe, this.center,
                    this.heading, this.pitch, this.zoom);
            if (modelview != null)
            {
                Matrix modelviewInv = modelview.getInverse();
                if (modelviewInv != null)
                {
                    return Vec4.UNIT_W.transformBy4(modelviewInv);
                }
            }
        }

        return Vec4.ZERO;        
    }

    public Position getCurrentEyePosition()
    {
        if (this.globe != null)
        {
            Matrix modelview = OrbitViewInputSupport.computeTransformMatrix(this.globe, this.center,
                    this.heading, this.pitch, this.zoom);
            if (modelview != null)
            {
                Matrix modelviewInv = modelview.getInverse();
                if (modelviewInv != null)
                {
                    Vec4 eyePoint = Vec4.UNIT_W.transformBy4(modelviewInv);
                    return this.globe.computePositionFromPoint(eyePoint);
                }
            }
        }

        return Position.ZERO;
    }

    public void setOrientation(Position eyePosition, Position centerPosition)
    {
        if (eyePosition == null || centerPosition == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (this.globe == null)
        {
            
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        Vec4 newEyePoint = this.globe.computePointFromPosition(eyePosition);
        Vec4 newCenterPoint = this.globe.computePointFromPosition(centerPosition);
        if (newEyePoint == null || newCenterPoint == null)
        {
            String message = Logging.getMessage("View.ErrorSettingOrientation", eyePosition, centerPosition);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // If eye lat/lon != center lat/lon, then the surface normal at the center point will be a good value
        // for the up direction.
        Vec4 up = this.globe.computeSurfaceNormalAtPoint(newCenterPoint);
        // Otherwise, estimate the up direction by using the *current* heading with the new center position.
        Vec4 forward = newCenterPoint.subtract3(newEyePoint).normalize3();
        if (forward.cross3(up).getLength3() < 0.001)
        {
            Matrix modelview = OrbitViewInputSupport.computeTransformMatrix(
                this.globe, centerPosition, this.heading, Angle.ZERO, 1);
            if (modelview != null)
            {
                Matrix modelviewInv = modelview.getInverse();
                if (modelviewInv != null)
                {
                    up = Vec4.UNIT_Y.transformBy4(modelviewInv);
                }
            }
        }

        if (up == null)
        {
            String message = Logging.getMessage("View.ErrorSettingOrientation", eyePosition, centerPosition);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        OrbitViewInputSupport.OrbitViewState modelCoords = OrbitViewInputSupport.computeOrbitViewState(
            this.globe, newEyePoint, newCenterPoint, up);
        if (!validateModelCoordinates(modelCoords))
        {
            String message = Logging.getMessage("View.ErrorSettingOrientation", eyePosition, centerPosition);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        setModelCoordinates(modelCoords);
    }

    protected void doApply(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getGL() == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getGlobe() == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Update DrawContext and Globe references.
        this.dc = dc;
        this.globe = this.dc.getGlobe();
        //========== modelview matrix state ==========//
        // Compute the current modelview matrix.
        this.modelview = OrbitViewInputSupport.computeTransformMatrix(this.globe, this.center,
                this.heading, this.pitch, this.zoom);
        if (this.modelview == null)
            this.modelview = Matrix.IDENTITY;
        // Compute the current inverse-modelview matrix.
        this.modelviewInv = this.modelview.getInverse();
        if (this.modelviewInv == null)
            this.modelviewInv = Matrix.IDENTITY;

        //========== projection matrix state ==========//
        // Get the current OpenGL viewport state.
        int[] viewportArray = new int[4];
        this.dc.getGL().glGetIntegerv(GL.GL_VIEWPORT, viewportArray, 0);
        this.viewport = new java.awt.Rectangle(viewportArray[0], viewportArray[1], viewportArray[2], viewportArray[3]);
        // Compute the current clip plane distances.

        this.nearClipDistance = computeNearClipDistance();
        this.farClipDistance = computeFarClipDistance();
        // Compute the current viewport dimensions.
        double viewportWidth = this.viewport.getWidth() <= 0.0 ? 1.0 : this.viewport.getWidth();
        double viewportHeight = this.viewport.getHeight() <= 0.0 ? 1.0 : this.viewport.getHeight();
        // Compute the current projection matrix.
        this.projection = Matrix.fromPerspective(this.fieldOfView,
            viewportWidth, viewportHeight,
            this.nearClipDistance, this.farClipDistance);
        // Compute the current frustum.
        this.frustum = Frustum.fromPerspective(this.fieldOfView,
            (int) viewportWidth, (int) viewportHeight,
            this.nearClipDistance, this.farClipDistance);

        //========== load GL matrix state ==========//
        loadGLViewState(dc, this.modelview, this.projection);

        //========== after apply (GL matrix state) ==========//
        afterDoApply();
    }



    protected void afterDoApply()
    {
        // Clear cached computations.
        this.lastEyePosition = null;
        this.lastEyePoint = null;
        this.lastUpVector = null;
        this.lastForwardVector = null;
        this.lastFrustumInModelCoords = null;
    }

    public Vec4 project(Vec4 modelPoint)
    {
        if (modelPoint == null)
        {
            String message = Logging.getMessage("nullValue.Vec4IsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return project(modelPoint, this.modelview, this.projection, this.viewport);
    }

    public Vec4 unProject(Vec4 windowPoint)
    {
        if (windowPoint == null)
        {
            String message = Logging.getMessage("nullValue.Vec4IsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return unProject(windowPoint, this.modelview, this.projection, this.viewport);
    }

    public Line computeRayFromScreenPoint(double x, double y)
    {
        return ViewUtil.computeRayFromScreenPoint(this,
            x, y, this.modelview, this.projection, this.viewport);
    }

    public Position computePositionFromScreenPoint(double x, double y)
    {
        if (this.globe != null)
        {
            Line ray = computeRayFromScreenPoint(x, y);
            if (ray != null)
                return this.globe.getIntersectionPosition(ray);
        }

        return null;
    }

    public double computeHorizonDistance()
    {
        double horizon = 0;
        Position eyePos = computeEyePositionFromModelview();
        if (eyePos != null)
        {
            horizon = computeHorizonDistance(eyePos);
        }

        return horizon;
    }

    protected double computeHorizonDistance(Position eyePosition)
    {
        if (this.globe != null && eyePosition != null)
        {
            double elevation = eyePosition.getElevation();
            double elevationAboveSurface = ViewUtil.computeElevationAboveSurface(this.dc, eyePosition);
            return ViewUtil.computeHorizonDistance(this.globe, Math.max(elevation, elevationAboveSurface));
        }

        return 0;
    }

    protected Position computeEyePositionFromModelview()
    {
        if (this.globe != null)
        {
            Vec4 eyePoint = Vec4.UNIT_W.transformBy4(this.modelviewInv);
            return this.globe.computePositionFromPoint(eyePoint);
        }

        return Position.ZERO;
    }


    public double computePixelSizeAtDistance(double distance)
    {
        return ViewUtil.computePixelSizeAtDistance(distance, this.fieldOfView, this.viewport);
    }

    protected void setModelCoordinates(OrbitViewInputSupport.OrbitViewState modelCoords)
    {
        if (modelCoords != null)
        {
            if (modelCoords.getCenterPosition() != null)
            {
                this.center = normalizedCenterPosition(modelCoords.getCenterPosition());
                this.center = BasicOrbitViewLimits.limitCenterPosition(this.center, this.getOrbitViewLimits());
            }
            if (modelCoords.getHeading() != null)
            {
                this.heading = normalizedHeading(modelCoords.getHeading());
                this.heading = BasicOrbitViewLimits.limitHeading(this.heading, this.getOrbitViewLimits());
            }
            if (modelCoords.getPitch() != null)
            {
                this.pitch = normalizedPitch(modelCoords.getPitch());
                this.pitch = BasicOrbitViewLimits.limitPitch(this.pitch, this.getOrbitViewLimits());
            }

            this.zoom = modelCoords.getZoom();
            this.zoom = BasicOrbitViewLimits.limitZoom(this.zoom, this.getOrbitViewLimits());
        }
    }

    protected boolean validateModelCoordinates(OrbitViewInputSupport.OrbitViewState modelCoords)
    {
        return (modelCoords != null
            && modelCoords.getCenterPosition() != null
            && modelCoords.getCenterPosition().getLatitude().degrees >= -90
            && modelCoords.getCenterPosition().getLatitude().degrees <= 90
            && modelCoords.getHeading() != null
            && modelCoords.getPitch() != null
            && modelCoords.getPitch().degrees >= 0
            && modelCoords.getPitch().degrees <= 90
            && modelCoords.getZoom() >= 0);
    }

    //**************************************************************//
    //******************** Restorable State  ***********************//
    //**************************************************************//

    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doGetRestorableState(rs, context);

        if (this.getCenterPosition() != null)
        {
            RestorableSupport.StateObject so = rs.addStateObject(context, "center");
            if (so != null)
            {
                rs.addStateValueAsDouble(so, "latitude", this.getCenterPosition().getLatitude().degrees);
                rs.addStateValueAsDouble(so, "longitude", this.getCenterPosition().getLongitude().degrees);
                rs.addStateValueAsDouble(so, "elevation", this.getCenterPosition().getElevation());
            }
        }

        rs.addStateValueAsDouble(context, "zoom", this.getZoom());
    }

    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        // Invoke the legacy restore functionality. This will enable the shape to recognize state XML elements
        // from previous versions of BasicOrbitView.
        this.legacyRestoreState(rs, context);
        
        super.doRestoreState(rs, context);

        // Restore the center property only if all parts are available.
        // We will not restore a partial center (for example, just latitude).
        RestorableSupport.StateObject so = rs.getStateObject(context, "center");
        if (so != null)
        {
            Double lat = rs.getStateValueAsDouble(so, "latitude");
            Double lon = rs.getStateValueAsDouble(so, "longitude");
            Double ele = rs.getStateValueAsDouble(so, "elevation");
            if (lat != null && lon != null)
                this.setCenterPosition(Position.fromDegrees(lat, lon, (ele != null ? ele : 0)));
        }

        Double d = rs.getStateValueAsDouble(context, "zoom");
        if (d != null)
            this.setZoom(d);
    }

    /**
     * Restores state values from previous versions of the BasicObitView state XML. These values are stored or named
     * differently than the current implementation. Those values which have not changed are ignored here, and are
     * restored in {@link #doRestoreState(gov.nasa.worldwind.util.RestorableSupport, gov.nasa.worldwind.util.RestorableSupport.StateObject)}.
     *
     * @param rs      RestorableSupport object which contains the state value properties.
     * @param context active context in the RestorableSupport to read state from.
     */
    protected void legacyRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        RestorableSupport.StateObject so = rs.getStateObject(context, "orbitViewLimits");
        if (so != null)
            this.getOrbitViewLimits().restoreState(rs, so);
    }

    //**************************************************************//
    //******************** Animator Convenience Methods ************//
    //**************************************************************//

    public void goTo(Position position, double distance)
    {
        viewInputHandler.goTo(position, distance);
    }

    public void addPanToAnimator(Position beginCenterPos, Position endCenterPos,
        Angle beginHeading, Angle endHeading,
        Angle beginPitch, Angle endPitch,
        double beginZoom, double endZoom, long timeToMove, boolean endCenterOnSurface)
    {
        ((OrbitViewInputHandler) this.viewInputHandler).addPanToAnimator(
            beginCenterPos, endCenterPos, beginHeading, endHeading, beginPitch, endPitch,
            beginZoom, endZoom, timeToMove, endCenterOnSurface);
    }

    public void addPanToAnimator(Position beginCenterPos, Position endCenterPos,
        Angle beginHeading, Angle endHeading,
        Angle beginPitch, Angle endPitch,
        double beginZoom, double endZoom, boolean endCenterOnSurface)
    {

       ((OrbitViewInputHandler) this.viewInputHandler).addPanToAnimator(
            beginCenterPos, endCenterPos, beginHeading, endHeading, beginPitch, endPitch,
            beginZoom, endZoom, endCenterOnSurface);
    }

    public void addPanToAnimator(Position centerPos, Angle heading, Angle pitch, double zoom,
        long timeToMove, boolean endCenterOnSurface)
    {
        ((OrbitViewInputHandler) this.viewInputHandler).addPanToAnimator(centerPos, heading, pitch, zoom, timeToMove,
            endCenterOnSurface);
    }

    public void addPanToAnimator(Position centerPos, Angle heading, Angle pitch, double zoom,
        boolean endCenterOnSurface)
    {
        ((OrbitViewInputHandler) this.viewInputHandler).addPanToAnimator(centerPos, heading, pitch, zoom,
            endCenterOnSurface);
    }

    public void addPanToAnimator(Position centerPos, Angle heading, Angle pitch, double zoom)
    {
        ((OrbitViewInputHandler) this.viewInputHandler).addPanToAnimator(centerPos, heading, pitch, zoom);
    }

    public void addEyePositionAnimator(long timeToIterate, Position beginPosition, Position endPosition)
    {
        ((OrbitViewInputHandler) this.viewInputHandler).addEyePositionAnimator(
            timeToIterate, beginPosition, endPosition);
    }

    public void addHeadingAnimator(Angle begin, Angle end)
    {
        ((OrbitViewInputHandler) this.viewInputHandler).addHeadingAnimator(begin, end);
    }

    public void addPitchAnimator(Angle begin, Angle end)
    {
        ((OrbitViewInputHandler) this.viewInputHandler).addPitchAnimator(begin, end);
    }

    public void addHeadingPitchAnimator(Angle beginHeading, Angle endHeading, Angle beginPitch, Angle endPitch)
    {
        ((OrbitViewInputHandler) this.viewInputHandler).addHeadingPitchAnimator(
            beginHeading, endHeading, beginPitch, endPitch);
    }

    public void addZoomAnimator(double zoomStart, double zoomEnd)
    {
        ((OrbitViewInputHandler) this.viewInputHandler).addZoomAnimator(
            zoomStart, zoomEnd);
    }

    public void addFlyToZoomAnimator(Angle heading, Angle pitch, double zoomAmount)
    {
        ((OrbitViewInputHandler) this.viewInputHandler).addFlyToZoomAnimator(
            heading, pitch, zoomAmount);   
    }

    public void addCenterAnimator(Position begin, Position end, boolean smoothed)
    {
        ((OrbitViewInputHandler) this.viewInputHandler).addCenterAnimator(begin, end, smoothed);    
    }

    public void addCenterAnimator(Position begin, Position end, long lengthMillis, boolean smoothed)
    {
        ((OrbitViewInputHandler) this.viewInputHandler).addCenterAnimator(begin, end, lengthMillis, smoothed);
    }
}
