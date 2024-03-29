/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.view;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.animation.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;

/**
 * @author jym
 * @version $Id: ViewUtil.java 12773 2009-11-07 18:43:48Z dcollins $
 */
public class ViewUtil
{

    public static class ViewState
    {
        private Position position;
        private Angle heading;
        private Angle pitch;
        private Angle roll;

        public ViewState(Position position, Angle heading, Angle pitch, Angle roll)
        {
            this.position = position;
            this.heading = heading;
            this.pitch = pitch;
            this.roll = roll;
        }

        public Position getPosition()
        {
            return (position);
        }

        public void setPosition(Position position)
        {
            this.position = position;
        }

        public Angle getRoll()
        {
            return (roll);
        }

        public void setRoll(Angle roll)
        {
            this.roll = roll;
        }

        public Angle getPitch()
        {
            return (pitch);
        }

        public void setPitch(Angle pitch)
        {
            this.pitch = pitch;
        }

        public Angle getHeading()
        {
            return (heading);
        }

        public void setHeading(Angle heading)
        {
            this.heading = heading;
        }
    }

    

    public static AngleAnimator createHeadingAnimator(View view, Angle begin, Angle end)
    {
        if (begin == null || end == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        final long MIN_LENGTH_MILLIS = 500;
        final long MAX_LENGTH_MILLIS = 3000;
        long lengthMillis = AnimationSupport.getScaledTimeMillisecs(
            begin, end, Angle.POS180,
            MIN_LENGTH_MILLIS, MAX_LENGTH_MILLIS);

        return new AngleAnimator(new ScheduledInterpolator(lengthMillis),
            begin, end, new ViewPropertyAccessor.HeadingAccessor(view));
    }

    public static AngleAnimator createPitchAnimator(View view, Angle begin, Angle end)
    {
        if (begin == null || end == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        final long MIN_LENGTH_MILLIS = 500;
        final long MAX_LENGTH_MILLIS = 3000;
        long lengthMillis = AnimationSupport.getScaledTimeMillisecs(
            begin, end, Angle.POS180,
            MIN_LENGTH_MILLIS, MAX_LENGTH_MILLIS);

        return new AngleAnimator(new ScheduledInterpolator(lengthMillis),
            begin, end, new ViewPropertyAccessor.PitchAccessor(view));
    }

    public static CompoundAnimator createHeadingPitchAnimator(View view, Angle beginHeading, Angle endHeading,
        Angle beginPitch, Angle endPitch)
    {
        if (beginHeading == null || endHeading == null || beginPitch == null || endPitch == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        final long MIN_LENGTH_MILLIS = 500;
        final long MAX_LENGTH_MILLIS = 3000;
        long headingLengthMillis = AnimationSupport.getScaledTimeMillisecs(
            beginHeading, endHeading, Angle.POS180,
            MIN_LENGTH_MILLIS, MAX_LENGTH_MILLIS);
        long pitchLengthMillis = AnimationSupport.getScaledTimeMillisecs(
            beginPitch, endPitch, Angle.POS90,
            MIN_LENGTH_MILLIS, MAX_LENGTH_MILLIS / 2L);
        long lengthMillis = headingLengthMillis + pitchLengthMillis;

        AngleAnimator headingAnimator = createHeadingAnimator(view, beginHeading, endHeading);
        AngleAnimator pitchAnimator = createPitchAnimator(view, beginPitch, endPitch);
        CompoundAnimator headingPitchAnimator = new CompoundAnimator(new ScheduledInterpolator(lengthMillis),
            headingAnimator, pitchAnimator);

        return (headingPitchAnimator);
    }

    public static PositionAnimator createEyePositionAnimator(
        View view, long timeToMove, Position begin, Position end)
    {
        return new PositionAnimator(new ScheduledInterpolator(timeToMove),
            begin, end, ViewPropertyAccessor.createEyePositionAccessor(view));
    }

    public static Point subtract(Point a, Point b)
    {
        if (a == null || b == null)
            return null;
        return new Point((int) (a.getX() - b.getX()), (int) (a.getY() - b.getY()));
    }

    public static Matrix computeTransformMatrix(Globe globe, Position position, Angle heading, Angle pitch, Angle roll)
    {

        if (heading == null)
        {
            String message = Logging.getMessage("nullValue.HeadingIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (pitch == null)
        {
            String message = Logging.getMessage("nullValue.PitchIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Matrix transform = Matrix.IDENTITY;

        transform = transform.multiply(Matrix.fromRotationX(pitch.multiply(-1.0)));
        transform = transform.multiply(Matrix.fromRotationZ(heading));
        transform = transform.multiply(Matrix.fromRotationY(roll));
        transform = transform.multiply(computePositionTransform(globe, position));
        return transform;
    }

    protected static Matrix computePositionTransform(Globe globe, Position center)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (center == null)
        {
            String message = Logging.getMessage("nullValue.CenterIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // The view eye position will be the same as the center position.
        // This is only the case without any zoom, heading, and pitch.
        Vec4 eyePoint = globe.computePointFromPosition(center);
        // The view forward direction will be colinear with the
        // geoid surface normal at the center position.
        Vec4 normal = globe.computeSurfaceNormalAtLocation(center.getLatitude(), center.getLongitude());
        Vec4 lookAtPoint = eyePoint.subtract3(normal);
        // The up direction will be pointing towards the north pole.
        Vec4 north = globe.computeNorthPointingTangentAtLocation(center.getLatitude(), center.getLongitude());
        // Creates a viewing matrix looking from eyePoint towards lookAtPoint,
        // with the given up direction. The forward, right, and up vectors
        // contained in the matrix are guaranteed to be orthogonal. This means
        // that the Matrix's up may not be equivalent to the specified up vector
        // here (though it will point in the same general direction).
        // In this case, the forward direction would not be affected.
        return Matrix.fromViewLookAt(eyePoint, lookAtPoint, north);
    }

    public static Matrix computeModelViewMatrix(Globe globe, Vec4 eyePoint, Vec4 centerPoint, Vec4 up)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (eyePoint == null)
        {
            String message = "nullValue.EyePointIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (centerPoint == null)
        {
            String message = "nullValue.CenterPointIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (up == null)
        {
            String message = "nullValue.UpIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Matrix modelview = Matrix.fromViewLookAt(eyePoint, centerPoint, up);
        return (modelview);
    }

    public static Vec4 getUpVector(Globe globe, Vec4 eyePoint, Vec4 lookAtPoint)
    {
        Vec4 up = globe.computeSurfaceNormalAtPoint(lookAtPoint);
        return up;
    }

    public static ViewState computeViewState(Globe globe, Vec4 eyePoint, Vec4 centerPoint, Vec4 up)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (eyePoint == null)
        {
            String message = "nullValue.EyePointIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (centerPoint == null)
        {
            String message = "nullValue.CenterPointIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (up == null)
        {
            up = ViewUtil.getUpVector(globe, eyePoint, centerPoint);
        }

        Matrix modelview = Matrix.fromViewLookAt(eyePoint, centerPoint, up);
        return ViewUtil.computeModelCoordinates(globe, modelview, centerPoint,
            eyePoint);
    }

    public static ViewState computeModelCoordinates(Globe globe, Matrix modelTransform, Vec4 centerPoint,
        Vec4 eyePoint)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (modelTransform == null)
        {
            String message = "nullValue.ModelTransformIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Compute the center position.
        Position centerPos = globe.computePositionFromPoint(centerPoint);
        // Compute the center position transform.
        Matrix centerTransform = ViewUtil.computePositionTransform(globe, centerPos);
        Matrix centerTransformInv = centerTransform.getInverse();
        if (centerTransformInv == null)
        {
            String message = Logging.getMessage("generic.NoninvertibleMatrix");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        // Compute the heading-pitch-zoom transform.
        Matrix hpzTransform = modelTransform.multiply(centerTransformInv);
        // Extract the heading, pitch, and zoom values from the transform.
        Angle heading = ViewUtil.computeHeading(hpzTransform);
        Angle pitch = ViewUtil.computePitch(hpzTransform);
        if (heading == null || pitch == null)
            return null;
        Position viewPosition = globe.computePositionFromPoint(eyePoint);
        return new ViewState(viewPosition, heading, pitch, Angle.ZERO);
    }

    public static Angle computeHeading(Matrix headingPitchZoomTransform)
    {
        if (headingPitchZoomTransform == null)
        {
            String message = "nullValue.HeadingPitchZoomTransformTransformIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return headingPitchZoomTransform.getRotationZ();
    }

    public static Angle computePitch(Matrix transform)
    {
        if (transform == null)
        {
            String message = "nullValue.HeadingPitchZoomTransformTransformIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Angle a = transform.getRotationX();
        if (a != null)
            a = a.multiply(-1.0);
        return a;
    }

    protected static Angle computeRoll(Matrix transform)
    {
        if (transform == null)
        {
            String message = "nullValue.HeadingPitchZoomTransformTransformIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return transform.getRotationY();
    }

    protected static Position computePosition(Globe globe, Matrix transform)
    {
        if (transform == null)
        {
            String message = "nullValue.HeadingPitchZoomTransformTransformIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        Vec4 v = transform.getTranslation();
        Position p = globe.computePositionFromPoint(v);

        return p != null ? p : Position.ZERO;
    }

    public static boolean validateViewState(ViewState viewState)
    {
        return (viewState != null
            && viewState.position != null
            && viewState.position.getLatitude().degrees >= -90
            && viewState.position.getLatitude().degrees <= 90
            && viewState.heading != null
            && viewState.pitch != null
            && viewState.pitch.degrees >= 0
            && viewState.pitch.degrees <= 90);
    }

    public static Position normalizedEyePosition(Position unnormalizedPosition)
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

    public static Angle normalizedRoll(Angle unnormalizedRoll)
    {
        if (unnormalizedRoll == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double degrees = unnormalizedRoll.degrees;
        double roll = degrees % 360;
        return Angle.fromDegrees(roll > 180 ? roll - 360 : (roll < -180 ? 360 + roll : roll));
    }

    public static Line computeRayFromScreenPoint(View view, double x, double y,
        Matrix modelview, Matrix projection, java.awt.Rectangle viewport)
    {
        if (modelview == null || projection == null)
        {
            String message = Logging.getMessage("nullValue.MatrixIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (viewport == null)
        {
            String message = Logging.getMessage("nullValue.RectangleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Compute a ray originating from the view, and passing through the screen point (x, y).
        //
        // Taken from the "OpenGL Technical FAQ & Troubleshooting Guide",
        // section 20.010 "How can I know which primitive a user has selected with the mouse?"
        //
        // http://www.opengl.org/resources/faq/technical/selection.htm#sele0010

        Matrix modelViewInv = modelview.getInverse();
        if (modelViewInv == null)
            return null;

        Vec4 eye = Vec4.UNIT_W.transformBy4(modelViewInv);
        if (eye == null)
            return null;

        double yInGLCoords = viewport.height - y - 1;
        Vec4 a = view.unProject(new Vec4(x, yInGLCoords, 0, 0));
        Vec4 b = view.unProject(new Vec4(x, yInGLCoords, 1, 0));
        if (a == null || b == null)
            return null;

        return new Line(eye, b.subtract3(a).normalize3());
    }

    public static double computePixelSizeAtDistance(double distance, Angle fieldOfView, java.awt.Rectangle viewport)
    {
        if (fieldOfView == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (viewport == null)
        {
            String message = Logging.getMessage("nullValue.RectangleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // If the viewport width is zero, than replace it with 1, which effectively ignores the viewport width.
        double viewportWidth = viewport.getWidth();
        double pixelSizeScale = 2 * fieldOfView.tanHalfAngle() / (viewportWidth <= 0 ? 1d : viewportWidth);

        return Math.abs(distance) * pixelSizeScale;
    }

    public static double computeHorizonDistance(Globe globe, double elevation)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (elevation <= 0)
            return 0;

        double radius = globe.getMaximumRadius();
        return Math.sqrt(elevation * (2 * radius + elevation));
    }

    public static double computeElevationAboveSurface(DrawContext dc, Position position)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        Globe globe = dc.getGlobe();
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (position == null)
        {
            String message = Logging.getMessage("nullValue.Vec4IsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Position surfacePosition = null;
        // Look for the surface geometry point at 'position'.
        Vec4 pointOnGlobe = dc.getPointOnGlobe(position.getLatitude(), position.getLongitude());
        if (pointOnGlobe != null)
            surfacePosition = globe.computePositionFromPoint(pointOnGlobe);
        // Fallback to using globe elevation values.
        if (surfacePosition == null)
            surfacePosition = new Position(
                position,
                globe.getElevation(position.getLatitude(), position.getLongitude()) * dc.getVerticalExaggeration());

        return position.getElevation() - surfacePosition.getElevation();
    }
}
