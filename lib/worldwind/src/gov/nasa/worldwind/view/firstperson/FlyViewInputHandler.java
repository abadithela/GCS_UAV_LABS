/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.view.firstperson;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.animation.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.view.*;
import gov.nasa.worldwind.view.orbit.OrbitViewPropertyAccessor;

import java.awt.event.*;

/**
 * @author jym
 * @version $Id: FlyViewInputHandler.java 12632 2009-09-22 02:33:48Z jterhorst $
 */
public class FlyViewInputHandler extends BasicViewInputHandler
{
    public class ResetPitchActionListener extends ViewInputActionHandler
    {
        public boolean inputActionPerformed(AbstractViewInputHandler inputHandler, java.awt.event.KeyEvent event,
            ViewInputAttributes.ActionAttributes viewAction)
        {
            java.util.List keyList = viewAction.getKeyActions();
            for (Object k : keyList)
            {
                ViewInputAttributes.ActionAttributes.KeyAction keyAction =
                    (ViewInputAttributes.ActionAttributes.KeyAction) k;
                if (event.getKeyCode() == keyAction.keyCode)
                {
                    onResetPitch(viewAction);
                    return true;
                }
            }
            return false;
        }

    }


    AnimationController uiAnimControl = new AnimationController();
    AnimationController gotoAnimControl = new AnimationController();

    private static final String VIEW_ANIM_HEADING = "ViewAnimHeading";
    private static final String VIEW_ANIM_PITCH = "ViewAnimPitch";
    private static final String VIEW_ANIM_POSITION = "ViewAnimPosition";
    private static final String VIEW_ANIM_PAN = "ViewAnimPan";
    private static final String VIEW_ANIM_APP = "ViewAnimApp";

    private static final String ACTION_RESET_PITCH = "ResetPitch";

    protected static final double DEFAULT_MOUSE_ROTATE_MIN_VALUE = 0.014; // Speed in degrees per mouse movement
    protected static final double DEFAULT_MOUSE_ROTATE_MAX_VALUE = 0.018; // Speed in degrees per mouse movement


    // Keyboard/Action calibration values for extensible view/navigation support
    //private static final double DEFAULT_KEY_HORIZONTAL_TRANSLATE_MIN_VALUE = 10;
    protected static final double DEFAULT_KEY_TRANSLATE_SMOOTHING_VALUE = .9;
    protected static final double DEFAULT_KEY_HORIZONTAL_TRANSLATE_MAX_VALUE = 1000000.0;
    protected static final double DEFAULT_KEY_HORIZONTAL_TRANSLATE_MIN_VALUE = 100;

    protected static final double DEFAULT_KEY_HORIZONTAL_TRANSLATE_MIN_VALUE_SLOW = 1;
    protected static final double DEFAULT_KEY_HORIZONTAL_TRANSLATE_MAX_VALUE_SLOW = 100000.0;

    protected static double DEFAULT_MOUSE_HORIZONTAL_TRANSLATE_MIN_VALUE = 5;
    protected static double DEFAULT_MOUSE_HORIZONTAL_TRANSLATE_MAX_VALUE = 50000.0;
    protected static final double DEFAULT_MOUSE_VERTICAL_TRANSLATE_MIN_VALUE = 1; // Speed in log-meters per mouse movement
    protected static final double DEFAULT_MOUSE_VERTICAL_TRANSLATE_MAX_VALUE = 30000; // Speed in log-meters per mouse movement

    protected static final double DEFAULT_KEY_VERTICAL_TRANSLATE_MIN_VALUE = 5;
    protected static final double DEFAULT_KEY_VERTICAL_TRANSLATE_MAX_VALUE = 5000;

    protected static final double DEFAULT_MOUSE_WHEEL_VERTICAL_TRANSLATE_VALUE_MIN_OSX = 10; // Speed in log-meters per wheel movement
    protected static final double DEFAULT_MOUSE_WHEEL_VERTICAL_TRANSLATE_VALUE_MAX_OSX = 900000; // Speed in log-meters per wheel movement
    protected static final double DEFAULT_MOUSE_WHEEL_VERTICAL_TRANSLATE_VALUE_MIN = 100; // Speed in log-meters per wheel movement
    protected static final double DEFAULT_MOUSE_WHEEL_VERTICAL_TRANSLATE_VALUE_MAX = 100000; // Speed in log-meters per wheel movement

    // Reset Heading
    private static final ViewInputAttributes.ActionAttributes.KeyAction
        DEFAULT_RESET_PITCH_KEY_ACT =
        new ViewInputAttributes.ActionAttributes.KeyAction(
            KeyEvent.VK_P, ViewInputAttributes.ActionAttributes.KeyAction.KA_DIR_X, 1);
    public static final ViewInputAttributes.ActionAttributes.KeyAction[] resetPitchEvents =
    {
        DEFAULT_RESET_PITCH_KEY_ACT
    };

    double speed = 10.0;



    public FlyViewInputHandler()
    {
        // Mouse Button Horizontal Translate Events
        // Button 1
        this.getAttributes().setValues(ViewInputAttributes.DEVICE_MOUSE,
            ViewInputAttributes.VIEW_HORIZONTAL_TRANSLATE,
            DEFAULT_MOUSE_HORIZONTAL_TRANSLATE_MIN_VALUE, DEFAULT_MOUSE_HORIZONTAL_TRANSLATE_MAX_VALUE);
        this.getAttributes().setActionTrigger(ViewInputAttributes.DEVICE_MOUSE,
            ViewInputAttributes.VIEW_HORIZONTAL_TRANSLATE,
            ViewInputAttributes.ActionAttributes.ActionTrigger.ON_KEY_DOWN);
            


        // Mouse Button Rotate Events
        // Button 1 + SHIFT
        this.getAttributes().setValues(ViewInputAttributes.DEVICE_MOUSE, ViewInputAttributes.VIEW_ROTATE_SHIFT,
            DEFAULT_MOUSE_ROTATE_MIN_VALUE,
                    DEFAULT_MOUSE_ROTATE_MAX_VALUE);
         this.getAttributes().setActionTrigger(ViewInputAttributes.DEVICE_MOUSE,
             ViewInputAttributes.VIEW_ROTATE_SHIFT,
            ViewInputAttributes.ActionAttributes.ActionTrigger.ON_KEY_DOWN);
        // Button 3
        this.getAttributes().setValues(ViewInputAttributes.DEVICE_MOUSE, ViewInputAttributes.VIEW_ROTATE,
            DEFAULT_MOUSE_ROTATE_MIN_VALUE,
            DEFAULT_MOUSE_ROTATE_MAX_VALUE);
        this.getAttributes().setActionTrigger(ViewInputAttributes.DEVICE_MOUSE,
            ViewInputAttributes.VIEW_ROTATE,
            ViewInputAttributes.ActionAttributes.ActionTrigger.ON_KEY_DOWN);


        // Mouse Vertical Translate
        // Button 2
        this.getAttributes().setValues(ViewInputAttributes.DEVICE_MOUSE,
            ViewInputAttributes.VIEW_VERTICAL_TRANSLATE, DEFAULT_MOUSE_VERTICAL_TRANSLATE_MIN_VALUE,
                DEFAULT_MOUSE_VERTICAL_TRANSLATE_MAX_VALUE);
        this.getAttributes().setActionTrigger(ViewInputAttributes.DEVICE_MOUSE,
            ViewInputAttributes.VIEW_VERTICAL_TRANSLATE,
            ViewInputAttributes.ActionAttributes.ActionTrigger.ON_KEY_DOWN);
        // Button 1 + CTRL
        this.getAttributes().setValues(ViewInputAttributes.DEVICE_MOUSE,
            ViewInputAttributes.VIEW_VERTICAL_TRANSLATE_CTRL, DEFAULT_MOUSE_VERTICAL_TRANSLATE_MIN_VALUE,
                DEFAULT_MOUSE_VERTICAL_TRANSLATE_MAX_VALUE);
        this.getAttributes().setActionTrigger(ViewInputAttributes.DEVICE_MOUSE,
            ViewInputAttributes.VIEW_VERTICAL_TRANSLATE_CTRL,
            ViewInputAttributes.ActionAttributes.ActionTrigger.ON_KEY_DOWN);

        // Arrow keys rotate


        // Arrow Keys horizontal translate
        this.getAttributes().setValues(ViewInputAttributes.DEVICE_KEYBOARD,
            ViewInputAttributes.VIEW_HORIZONTAL_TRANS_KEYS,
            DEFAULT_KEY_HORIZONTAL_TRANSLATE_MIN_VALUE,
            DEFAULT_KEY_HORIZONTAL_TRANSLATE_MAX_VALUE);
        this.getAttributes().getActionAttributes(ViewInputAttributes.DEVICE_KEYBOARD,
            ViewInputAttributes.VIEW_HORIZONTAL_TRANS_KEYS).setSmoothingValue(DEFAULT_KEY_TRANSLATE_SMOOTHING_VALUE);

        this.getAttributes().setValues(ViewInputAttributes.DEVICE_KEYBOARD,
            ViewInputAttributes.VIEW_HORIZONTAL_TRANSLATE_SLOW,
            DEFAULT_KEY_HORIZONTAL_TRANSLATE_MIN_VALUE_SLOW, DEFAULT_KEY_HORIZONTAL_TRANSLATE_MAX_VALUE_SLOW);
        /*
        this.getAttributes().setActionTrigger(ViewInputAttributes.DEVICE_KEYBOARD,
            ViewInputAttributes.VIEW_HORIZONTAL_TRANSLATE_SLOW,
            ViewInputAttributes.ActionAttributes.ActionTrigger.ON_KEY_DOWN);
        */

        // +- Keys vertical translate
        this.getAttributes().setValues(ViewInputAttributes.DEVICE_KEYBOARD,
            ViewInputAttributes.VIEW_VERTICAL_TRANS_KEYS,
            DEFAULT_KEY_VERTICAL_TRANSLATE_MIN_VALUE, DEFAULT_KEY_VERTICAL_TRANSLATE_MAX_VALUE);
        // Arrow keys vertical translate
        this.getAttributes().setValues(ViewInputAttributes.DEVICE_KEYBOARD,
            ViewInputAttributes.VIEW_VERTICAL_TRANS_KEYS_META,
            DEFAULT_KEY_VERTICAL_TRANSLATE_MIN_VALUE, DEFAULT_KEY_VERTICAL_TRANSLATE_MAX_VALUE);
        this.getAttributes().setValues(ViewInputAttributes.DEVICE_KEYBOARD,
            ViewInputAttributes.VIEW_VERTICAL_TRANS_KEYS_CTRL,
            DEFAULT_KEY_VERTICAL_TRANSLATE_MIN_VALUE, DEFAULT_KEY_VERTICAL_TRANSLATE_MAX_VALUE);


        // Mouse Wheel vertical translate
        if (Configuration.isMacOS())
        {
            this.getAttributes().setValues(ViewInputAttributes.DEVICE_MOUSE_WHEEL,
                ViewInputAttributes.VIEW_VERTICAL_TRANSLATE,
                DEFAULT_MOUSE_WHEEL_VERTICAL_TRANSLATE_VALUE_MIN_OSX,
                DEFAULT_MOUSE_WHEEL_VERTICAL_TRANSLATE_VALUE_MAX_OSX);
        }
        else
        {
             this.getAttributes().setValues(ViewInputAttributes.DEVICE_MOUSE_WHEEL,
                ViewInputAttributes.VIEW_VERTICAL_TRANSLATE,
                DEFAULT_MOUSE_WHEEL_VERTICAL_TRANSLATE_VALUE_MIN,
                DEFAULT_MOUSE_WHEEL_VERTICAL_TRANSLATE_VALUE_MAX);    
        }

        // P Key Reset Pitch
        this.getAttributes().addAction(ViewInputAttributes.DEVICE_KEYBOARD,
            ViewInputAttributes.ActionAttributes.NO_MODIFIER, ACTION_RESET_PITCH,
            new ViewInputAttributes.ActionAttributes(resetPitchEvents,
                ViewInputAttributes.ActionAttributes.ActionTrigger.ON_PRESS, 0,
                0.1,0.1,false,0.1));
        // Reset Pitch
        ViewInputAttributes.ActionAttributes actionAttrs =
            this.getAttributes().getActionMap(ViewInputAttributes.DEVICE_KEYBOARD).getActionAttributes(
                ACTION_RESET_PITCH);
        actionAttrs.setActionListener(new ResetPitchActionListener());

    }

    protected void onMoveTo(Position focalPosition, ViewInputAttributes.DeviceAttributes deviceAttributes,
        ViewInputAttributes.ActionAttributes actionAttribs)
    {
        BasicFlyView view = (BasicFlyView) this.getView();
        if (view == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        // We're treating a speed parameter as smoothing here. A greater speed results in greater smoothing and
        // slower response. Therefore the min speed used at lower altitudes ought to be *greater* than the max
        // speed used at higher altitudes.
        double smoothing = this.getScaleValueElevation(deviceAttributes, actionAttribs);
        if (!actionAttribs.isEnableSmoothing())
            smoothing = 0.0;


        Vec4 currentLookAtPt = view.getCenterPoint();
        if (currentLookAtPt == null)
        {
            currentLookAtPt = view.

                getGlobe().computePointFromPosition(focalPosition);
        }

        Vec4 currentEyePt = view.getEyePoint();
        double distanceToSurface = currentEyePt.distanceTo3(currentLookAtPt);
        Vec4 lookDirection = currentEyePt.subtract3(currentLookAtPt).normalize3();
        Vec4 newLookAtPt = view.getGlobe().computePointFromPosition(focalPosition);
        Vec4 flyToPoint = newLookAtPt.add3(lookDirection.multiply3(distanceToSurface));

        Position newPosition = view.getGlobe().computePositionFromPoint(flyToPoint);


        ViewUtil.ViewState viewCoords = view.getViewState(newPosition, focalPosition);

        this.uiAnimControl.clear();
        this.gotoAnimControl.clear();
        this.gotoAnimControl.put(VIEW_ANIM_HEADING,
            new RotateToAngleAnimator(
                view.getHeading(), viewCoords.getHeading(), smoothing,
                ViewPropertyAccessor.createHeadingAccessor(view)));
        this.gotoAnimControl.put(VIEW_ANIM_PITCH,
            new RotateToAngleAnimator(
                view.getPitch(), viewCoords.getPitch(), smoothing,
                ViewPropertyAccessor.createPitchAccessor(view)));

        double elevation = ((FlyViewLimits)
                view.getViewPropertyLimits()).limitEyeElevation(
                newPosition, view.getGlobe());
        if (elevation != newPosition.getElevation()) {
            newPosition = new Position(newPosition.getLatLon(), elevation);
        }
        this.gotoAnimControl.put(VIEW_ANIM_POSITION,
            new MoveToPositionAnimator(
                view.getEyePosition(), newPosition, smoothing,
                ViewPropertyAccessor.createEyePositionAccessor(view)));

        view.firePropertyChange(AVKey.VIEW, null, view);
    }

    protected void onMoveTo(Position focalPosition, ViewInputAttributes.ActionAttributes actionAttribs)
    {

    }


    protected void onHorizontalTranslateAbs(Angle latitudeChange, Angle longitudeChange,
        ViewInputAttributes.ActionAttributes actionAttribs)
    {

    }

    protected void onHorizontalTranslateRel(double forwardInput, double sideInput,
        double totalForwardInput, double totalSideInput,
        ViewInputAttributes.DeviceAttributes deviceAttributes,
        ViewInputAttributes.ActionAttributes actionAttributes)
    {
        Angle forwardChange;
        Angle sideChange;

        gotoAnimControl.clear();
        if (actionAttributes.getMouseActions() != null)
        {
            forwardChange = Angle.fromDegrees(-totalForwardInput
                 * getScaleValueElevation(deviceAttributes, actionAttributes));
            sideChange = Angle.fromDegrees(totalSideInput
                 * getScaleValueElevation(deviceAttributes, actionAttributes));
        }
        else
        {
            forwardChange = Angle.fromDegrees(
                forwardInput*speed * getScaleValueElevation(deviceAttributes, actionAttributes));
            sideChange = Angle.fromDegrees(
                sideInput*speed * getScaleValueElevation(deviceAttributes, actionAttributes));
        }
        onHorizontalTranslateRel(forwardChange, sideChange, actionAttributes);
    }

    protected void onHorizontalTranslateRel(Angle forwardChange, Angle sideChange,
        ViewInputAttributes.ActionAttributes actionAttribs)
    {
        View view = this.getView();
        if (view == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (forwardChange.equals(Angle.ZERO) && sideChange.equals(Angle.ZERO))
        {
            return;
        }

        if (view instanceof BasicFlyView)
        {

            Vec4 forward = view.getForwardVector();
            Vec4 up = view.getUpVector();
            Vec4 side = forward.transformBy3(Matrix.fromAxisAngle(Angle.fromDegrees(90), up));

            forward = forward.multiply3(forwardChange.getDegrees());
            side = side.multiply3(sideChange.getDegrees());
            Vec4 eyePoint = view.getEyePoint();
            eyePoint = eyePoint.add3(forward.add3(side));
            Position newPosition = view.getGlobe().computePositionFromPoint(eyePoint);

            this.setEyePosition( this.uiAnimControl, view, newPosition, actionAttribs);
            view.firePropertyChange(AVKey.VIEW, null, view);
        }
    }

    protected void onResetHeading(ViewInputAttributes.ActionAttributes actionAttribs)
    {

        View view = this.getView();
        if (view == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }
        double smoothing = actionAttribs.getSmoothingValue();
        if (!(actionAttribs.isEnableSmoothing() && this.isEnableSmoothing()))
            smoothing = 0.0;
        this.gotoAnimControl.put(VIEW_ANIM_HEADING,
            new RotateToAngleAnimator(
                view.getHeading(), Angle.ZERO, smoothing,
                ViewPropertyAccessor.createHeadingAccessor(view)));
        view.firePropertyChange(AVKey.VIEW, null, view);

    }

    protected void onResetPitch(ViewInputAttributes.ActionAttributes actionAttribs)
    {

        View view = this.getView();
        if (view == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }
        double smoothing = actionAttribs.getSmoothingValue();
        if (!(actionAttribs.isEnableSmoothing() && this.isEnableSmoothing()))
            smoothing = 0.0;
        this.gotoAnimControl.put(VIEW_ANIM_PITCH,
            new RotateToAngleAnimator(
                view.getPitch(), Angle.POS90, smoothing,
                ViewPropertyAccessor.createPitchAccessor(view)));
        view.firePropertyChange(AVKey.VIEW, null, view);
    }

    protected void onResetHeadingAndPitch(ViewInputAttributes.ActionAttributes actionAttribs)
    {

        View view = this.getView();
        if (view == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }
        double smoothing = .95;
        this.gotoAnimControl.put(VIEW_ANIM_HEADING,
            new RotateToAngleAnimator(
                view.getHeading(), Angle.ZERO, smoothing,
                ViewPropertyAccessor.createHeadingAccessor(view)));
        this.gotoAnimControl.put(VIEW_ANIM_PITCH,
            new RotateToAngleAnimator(
                view.getPitch(), Angle.POS90, smoothing,
                ViewPropertyAccessor.createPitchAccessor(view)));
        view.firePropertyChange(AVKey.VIEW, null, view);
    }

    protected void onRotateView(double headingInput, double pitchInput,
        double totalHeadingInput, double totalPitchInput,
        ViewInputAttributes.DeviceAttributes deviceAttributes,
        ViewInputAttributes.ActionAttributes actionAttributes)
    {

        Angle headingChange;
        Angle pitchChange;
        gotoAnimControl.clear();
        headingChange = Angle.fromDegrees(
            totalHeadingInput * getScaleValueElevation(deviceAttributes, actionAttributes));
        pitchChange = Angle.fromDegrees(
            totalPitchInput * getScaleValueElevation(deviceAttributes, actionAttributes));
        onRotateView(headingChange, pitchChange, actionAttributes);
    }

    protected void onRotateView(Angle headingChange, Angle pitchChange,
        ViewInputAttributes.ActionAttributes actionAttribs)
    {

        View view = this.getView();
        if (view == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (view instanceof BasicFlyView)
        {
            BasicFlyView flyView = (BasicFlyView) view;
            this.setPitch(flyView, this.uiAnimControl, flyView.getPitch().add(pitchChange),
                actionAttribs);
            this.setHeading(flyView, this.uiAnimControl, flyView.getHeading().add(headingChange),
                actionAttribs);
            view.firePropertyChange(AVKey.VIEW, null, view);
        }
    }

    protected void onVerticalTranslate(double translateChange, double totalTranslateChange,
        ViewInputAttributes.DeviceAttributes deviceAttributes,
        ViewInputAttributes.ActionAttributes actionAttribs)
    {
        this.gotoAnimControl.clear();
        double elevChange = -(totalTranslateChange * getScaleValueElevation(deviceAttributes, actionAttribs));
        View view = this.getView();
        Position position = view.getEyePosition();
        Position newPos = new Position(position.getLatLon(), position.getElevation()+(elevChange));
        this.setEyePosition(uiAnimControl, view, newPos, actionAttribs);


        view.firePropertyChange(AVKey.VIEW, null, view);
    }

    protected void onVerticalTranslate(double translateChange, 
        ViewInputAttributes.ActionAttributes actionAttribs)
    {

    }

    public void apply()
    {
        super.apply();

        View view = this.getView();
        if (view == null)
        {
            return;
        }
        if (this.gotoAnimControl.stepAnimators())
        {
            view.firePropertyChange(AVKey.VIEW, null, view);
        }
        if (this.uiAnimControl.stepAnimators())
        {
            view.firePropertyChange(AVKey.VIEW, null, view);
        }
    }

    protected void handleViewStopped()
    {
        this.uiAnimControl.clear();
        this.gotoAnimControl.clear();

    }

    protected void handleOrbitViewCenterStopped()
    {
        this.uiAnimControl.clear();
        this.gotoAnimControl.clear();

    }

    protected void setHeading(View view,
        AnimationController animControl,
        Angle heading, ViewInputAttributes.ActionAttributes attrib)
    {
        double smoothing = attrib.getSmoothingValue();
        if (!(attrib.isEnableSmoothing() && this.isEnableSmoothing()))
            smoothing = 0.0;

        AngleAnimator angleAnimator = new RotateToAngleAnimator(
            view.getHeading(), heading, smoothing,
            ViewPropertyAccessor.createHeadingAccessor(view));
        animControl.put(VIEW_ANIM_HEADING, angleAnimator);

    }

    protected void setPitch(View view,
        AnimationController animControl,
        Angle pitch, ViewInputAttributes.ActionAttributes attrib)
    {
        double smoothing = attrib.getSmoothingValue();
        if (!(attrib.isEnableSmoothing() && this.isEnableSmoothing()))
            smoothing = 0.0;

        AngleAnimator angleAnimator = new RotateToAngleAnimator(
            view.getPitch(), pitch, smoothing,
            ViewPropertyAccessor.createPitchAccessor(view));
        animControl.put(VIEW_ANIM_PITCH, angleAnimator);

    }

    

    protected void setEyePosition(AnimationController animControl, View view, Position position, ViewInputAttributes.ActionAttributes attrib)
    {

        MoveToPositionAnimator posAnimator = (MoveToPositionAnimator)
            animControl.get(VIEW_ANIM_POSITION);

        double smoothing = attrib.getSmoothingValue();
        if (!(attrib.isEnableSmoothing() && this.isEnableSmoothing()))
            smoothing = 0.0;

        if (smoothing != 0.0)
        {

            double elevation = ((FlyViewLimits)
                view.getViewPropertyLimits()).limitEyeElevation(
                position, view.getGlobe());
            if (elevation != position.getElevation()) {
                position = new Position(position.getLatLon(), elevation);
            }
            if (posAnimator == null)
            {
                posAnimator = new MoveToPositionAnimator(
                    view.getEyePosition(), position, smoothing,
                    OrbitViewPropertyAccessor.createEyePositionAccessor(view));
                animControl.put(VIEW_ANIM_POSITION, posAnimator);
            }
            else
            {
                posAnimator.setEnd(position);
                posAnimator.start();
            }
        }
        view.firePropertyChange(AVKey.VIEW, null, view);
    }

    public void goTo(Position lookAtPos, double distance)
    {


        Globe globe = this.getView().getGlobe();
        BasicFlyView view = (BasicFlyView) this.getView();

        Position lookFromPos = new Position(lookAtPos.getLatLon(),
            globe.getElevation( lookAtPos.getLatitude(), lookAtPos.getLongitude())+distance);

        
        // TODO: scale on mid-altitude?
        final long MIN_LENGTH_MILLIS = 4000;
        final long MAX_LENGTH_MILLIS = 16000;
        long timeToMove = AnimationSupport.getScaledTimeMillisecs(
            view.getEyePosition(), lookFromPos,
            MIN_LENGTH_MILLIS, MAX_LENGTH_MILLIS);
        FlyToFlyViewAnimator panAnimator = FlyToFlyViewAnimator.createFlyToFlyViewAnimator(view,
            view.getEyePosition(), lookFromPos,
            view.getHeading(), Angle.ZERO,
            view.getPitch(), Angle.ZERO,
            view.getEyePosition().getElevation(), lookFromPos.getElevation(),
            timeToMove, false);


        this.gotoAnimControl.put(VIEW_ANIM_PAN, panAnimator);

        this.getView().firePropertyChange(AVKey.VIEW, null, this.getView());
    }

    
    public void lookAt(Position lookAtPos, long timeToMove)
    {
        BasicFlyView view = (BasicFlyView) this.getView();
        Vec4 lookDirection;
        double distanceToSurface;
        Vec4 currentLookAtPt = view.getCenterPoint();
        Position newPosition;
        if (currentLookAtPt == null)
        {
            view.getGlobe().computePointFromPosition(lookAtPos);
            double elevAtLookAtPos = view.getGlobe().getElevation(
               lookAtPos.getLatitude(), lookAtPos.getLongitude());
            newPosition = new Position(lookAtPos, elevAtLookAtPos+10000);
        }
        else
        {
            Vec4 currentEyePt = view.getEyePoint();
            distanceToSurface = currentEyePt.distanceTo3(currentLookAtPt);
            lookDirection = currentLookAtPt.subtract3(currentEyePt).normalize3();
             Vec4 newLookAtPt = view.getGlobe().computePointFromPosition(lookAtPos);
            Vec4 flyToPoint = newLookAtPt.add3(lookDirection.multiply3(-distanceToSurface));
            newPosition = view.getGlobe().computePositionFromPoint(flyToPoint);
        }

        ViewUtil.ViewState viewCoords = view.getViewState(newPosition, lookAtPos);
        
        FlyToFlyViewAnimator panAnimator = FlyToFlyViewAnimator.createFlyToFlyViewAnimator(view,
            view.getEyePosition(), newPosition,
            view.getHeading(), viewCoords.getHeading(),
            view.getPitch(), viewCoords.getPitch(),
            view.getEyePosition().getElevation(), viewCoords.getPosition().getElevation(),
            timeToMove, true);


        this.gotoAnimControl.put(VIEW_ANIM_PAN, panAnimator);
        this.getView().firePropertyChange(AVKey.VIEW, null, this.getView());

        view.firePropertyChange(AVKey.VIEW, null, view);
    }


    public void stopAnimators()
    {
        this.uiAnimControl.clear();
        this.gotoAnimControl.clear();    
    }

    public boolean isAnimating()
    {
        return (this.uiAnimControl.hasActiveAnimation() || this.gotoAnimControl.hasActiveAnimation());
    }

    public void addAnimator(Animator animator)
    {
        this.gotoAnimControl.put(VIEW_ANIM_APP, animator);
    }

}
