/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples.util;

import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.Logging;

import javax.media.opengl.GL;

/**
 * @author dcollins
 * @version $Id: ButtonAnnotation.java 10616 2009-04-28 23:28:57Z dcollins $
 */
public class ButtonAnnotation extends ImageAnnotation implements SelectListener
{
    protected boolean enabled;
    protected boolean pressed;
    protected String actionCommand;
    protected double disabledOpacity;
    protected Object pressedMask;
    // Event listeners.
    protected javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();

    public ButtonAnnotation(Object imageSource, Object pressedMaskSource)
    {
        super(imageSource);
        this.enabled = true;
        this.disabledOpacity = 0.6;
        this.pressedMask = pressedMaskSource;
    }

    public ButtonAnnotation(Object imageSource)
    {
        this(imageSource, null);
    }

    public ButtonAnnotation()
    {
        this(null);
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public boolean isPressed()
    {
        return this.pressed;
    }

    public void setPressed(boolean pressed)
    {
        this.pressed = pressed;
    }

    public String getActionCommand()
    {
        return this.actionCommand;
    }

    public void setActionCommand(String actionCommand)
    {
        this.actionCommand = actionCommand;
    }

    public double getDisabledOpacity()
    {
        return this.disabledOpacity;
    }

    public void setDisabledOpacity(double opacity)
    {
        if (opacity < 0 || opacity > 1)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "opacity < 0 or opacity > 1");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.disabledOpacity = opacity;
    }

    public Object getPressedMaskSource()
    {
        return this.pressedMask;
    }

    public void setPressedMaskSource(Object source)
    {
        this.pressedMask = source;
    }

    public java.awt.event.ActionListener[] getActionListeners()
    {
        return this.listenerList.getListeners(java.awt.event.ActionListener.class);
    }

    public void addActionListener(java.awt.event.ActionListener listener)
    {
        this.listenerList.add(java.awt.event.ActionListener.class, listener);
    }

    public void removeActionListener(java.awt.event.ActionListener listener)
    {
        this.listenerList.remove(java.awt.event.ActionListener.class, listener);
    }

    protected void setupAnnotationAttributes(Annotation annotation)
    {
        super.setupAnnotationAttributes(annotation);
        
        annotation.setPickEnabled(true);
    }

    //**************************************************************//
    //********************  Select Listener  ***********************//
    //**************************************************************//

    @SuppressWarnings({"StringEquality"})
    public void selected(SelectEvent e)
    {
        if (e == null)
            return;

        // Ignore hover and rollover events. We're only interested in mouse pressed and mouse clicked events.
        if (e.getEventAction() == SelectEvent.HOVER || e.getEventAction() == SelectEvent.ROLLOVER)
            return;

        if (!this.isEnabled())
            return;

        Object topObject = e.getTopObject();
        if (topObject == this)
        {
            this.setPressed(this.isButtonPressed(e));

            if (this.isButtonTrigger(e))
            {
                this.onButtonPressed(e);
            }
        }
    }

    @SuppressWarnings({"StringEquality"})
    protected boolean isButtonPressed(SelectEvent e)
    {
        return e.getEventAction() == SelectEvent.LEFT_PRESS;
    }

    @SuppressWarnings({"StringEquality"})
    protected boolean isButtonTrigger(SelectEvent e)
    {
        return e.getEventAction() == SelectEvent.LEFT_CLICK;
    }

    protected void onButtonPressed(SelectEvent e)
    {
        java.awt.event.MouseEvent mouseEvent = e.getMouseEvent();
        this.fireActionPerformed(mouseEvent.getID(), mouseEvent.getWhen(), mouseEvent.getModifiers());
    }

    //**************************************************************//
    //********************  Action Listener  ***********************//
    //**************************************************************//

    protected void fireActionPerformed(int id, long when, int modifiers)
    {
        java.awt.event.ActionEvent event = null;
        // Guaranteed to return a non-null array
        Object[] listeners = this.listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2)
        {
            if (listeners[i] == java.awt.event.ActionListener.class)
            {
                // Lazily create the event:
                if (event == null)
                {
                    event = new java.awt.event.ActionEvent(this, id, this.getActionCommand(), when, modifiers);
                }

                ((java.awt.event.ActionListener) listeners[i+1]).actionPerformed(event);
            }
        }
    }

    //**************************************************************//
    //********************  Rendering  *****************************//
    //**************************************************************//

    public void drawContent(DrawContext dc, int width, int height, double opacity, Position pickPosition)
    {
        if (!this.isEnabled())
        {
            opacity *= this.getDisabledOpacity();
        }

        super.drawContent(dc, width, height, opacity, pickPosition);
        this.drawPressedMask(dc, width, height, opacity, pickPosition);
    }

    protected void drawPressedMask(DrawContext dc, int width, int height, double opacity, Position pickPosition)
    {
        if (dc.isPickingMode())
            return;

        if (!this.isPressed())
            return;

        this.doDrawPressedMask(dc, width, height, opacity, pickPosition);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void doDrawPressedMask(DrawContext dc, int width, int height, double opacity, Position pickPosition)
    {
        Object imageSource = this.getPressedMaskSource();
        if (imageSource == null)
            return;

        AnnotationTexture texture = this.getAnnotationTexture(imageSource);
        if (texture == null)
            return;

        if (!texture.bind(dc))
            return;
        
        // Ignore any incoming opacity, since we just want to modulate our texture colors with the current framebuffer
        // colors.
        this.applyBackgroundTextureState(dc, width, height, 1, texture);
        
        // Setup the mask to modulate with the existing fragment color. This will have the effect of multiplying
        // the button depressed mask colors with the button colors.
        GL gl = dc.getGL();
        gl.glColor4f(1, 1, 1, 1);
        gl.glBlendFunc(GL.GL_ZERO, GL.GL_SRC_COLOR);

        this.doDrawBackgroundTexture(dc, width, height, 1, pickPosition, texture);
    }
}
