/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.Movable;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.*;

import java.awt.*;
import java.util.Map;

/**
 * @author tag
 * @version $Id$
 */
public class UserFacingIcon extends AVListImpl implements WWIcon, Movable
{
    //    private final String iconPath;
    private Position iconPosition; // may be null because placement may be relative
    private Dimension iconSize; // may be null to indicate "use native image size"
    private boolean isHighlighted = false;
    private boolean isVisible = true;
    private double highlightScale = 1.2; // TODO: make configurable
    private String toolTipText;
    private Font toolTipFont;
    private Vec4 toolTipOffset;
    private boolean showToolTip = false;
    private boolean alwaysOnTop = false;
    private java.awt.Color textColor;
    private double backgroundScale;

    protected BasicWWTexture imageTexture;
    protected BasicWWTexture backgroundTexture;

    public UserFacingIcon(String iconPath, Position iconPosition)
    {
        if (iconPath == null)
        {
            String message = Logging.getMessage("nullValue.IconFilePath");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.imageTexture = new BasicWWTexture(iconPath, true);
        this.imageTexture.setUseAnisotropy(false);
        this.iconPosition = iconPosition;
    }

    public UserFacingIcon(Object imageSource, Position iconPosition)
    {
        if (imageSource == null)
        {
            String message = Logging.getMessage("nullValue.IconFilePath");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.imageTexture = new BasicWWTexture(imageSource, true);
        this.imageTexture.setUseAnisotropy(false);
        this.iconPosition = iconPosition;
    }

    public BasicWWTexture getImageTexture()
    {
        return imageTexture;
    }

    public BasicWWTexture getBackgroundTexture()
    {
        return backgroundTexture;
    }

    public Object getImageSource()
    {
        return this.getImageTexture() != null ? this.getImageTexture().getImageSource() : null;
    }

    public void setImageSource(Object imageSource)
    {
        this.imageTexture = new BasicWWTexture(imageSource, true);
        this.imageTexture.setUseAnisotropy(false);
    }

    public String getPath()
    {
        return this.getImageSource() instanceof String ? (String) this.getImageSource() : null;
    }

    public Position getPosition()
    {
        return this.iconPosition;
    }

    public void setPosition(Position iconPosition)
    {
        this.iconPosition = iconPosition;
    }

    public boolean isHighlighted()
    {
        return isHighlighted;
    }

    public void setHighlighted(boolean highlighted)
    {
        isHighlighted = highlighted;
    }

    /**
     * {@inheritDoc}
     *
     * @return the icon's highlight scale. The default scale is 1.2.
     */
    public double getHighlightScale()
    {
        return highlightScale;
    }

    public void setHighlightScale(double highlightScale)
    {
        this.highlightScale = highlightScale;
    }

    public Dimension getSize()
    {
        return this.iconSize;
    }

    public void setSize(Dimension size)
    {
        this.iconSize = size;
    }

    public boolean isVisible()
    {
        return isVisible;
    }

    public void setVisible(boolean visible)
    {
        isVisible = visible;
    }

    public String getToolTipText()
    {
        return toolTipText;
    }

    public void setToolTipText(String toolTipText)
    {
        this.toolTipText = toolTipText;
    }

    public Font getToolTipFont()
    {
        return toolTipFont;
    }

    public void setToolTipFont(Font toolTipFont)
    {
        this.toolTipFont = toolTipFont;
    }

    public Vec4 getToolTipOffset()
    {
        return toolTipOffset;
    }

    public void setToolTipOffset(Vec4 toolTipOffset)
    {
        this.toolTipOffset = toolTipOffset;
    }

    public boolean isShowToolTip()
    {
        return showToolTip;
    }

    public void setShowToolTip(boolean showToolTip)
    {
        this.showToolTip = showToolTip;
    }

    public Color getToolTipTextColor()
    {
        return textColor;
    }

    public void setToolTipTextColor(Color textColor)
    {
        this.textColor = textColor;
    }

    public boolean isAlwaysOnTop()
    {
        return alwaysOnTop;
    }

    public void setAlwaysOnTop(boolean alwaysOnTop)
    {
        this.alwaysOnTop = alwaysOnTop;
    }

    public Object getBackgroundImage()
    {
        return this.getBackgroundTexture() != null ? this.getBackgroundTexture().getImageSource() : null;
    }

    public void setBackgroundImage(Object background)
    {
        if (background != null)
        {
            this.backgroundTexture = new BasicWWTexture(background, true);
            this.backgroundTexture.setUseAnisotropy(false);
        }
        else
            this.backgroundTexture = null;
    }

    public double getBackgroundScale()
    {
        return backgroundScale;
    }

    public void setBackgroundScale(double backgroundScale)
    {
        this.backgroundScale = backgroundScale;
    }

    public void move(Position position)
    {
        if (position == null)
        {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.iconPosition = this.iconPosition.add(position);
    }

    public void moveTo(Position position)
    {
        if (position == null)
        {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.iconPosition = position;
    }

    public Position getReferencePosition()
    {
        return this.iconPosition;
    }

    public String toString()
    {
        return this.getImageSource() != null ? this.getImageSource().toString() : this.getClass().getName();
    }

    /**
     * Returns an XML state document String describing the public attributes of this UserFacingIcon.
     *
     * @return XML state document string describing this UserFacingIcon.
     */
    public String getRestorableState()
    {
        RestorableSupport restorableSupport = RestorableSupport.newRestorableSupport();
        // Creating a new RestorableSupport failed. RestorableSupport logged the problem, so just return null.
        if (restorableSupport == null)
            return null;

        // Save the imagePath property only when the imageSource property is a simple String path. If the imageSource
        // property is a BufferedImage (or some other object), we make no effort to save that state. We save under
        // the name "imagePath" to denote that it is a special case of "imageSource".
        if (getPath() != null)
            restorableSupport.addStateValueAsString("imagePath", getPath(), true);

        // Save the iconPosition property only if all parts (latitude, longitude, and elevation) can be saved.
        // We will not save a partial iconPosition (for example, just the elevation).
        if (this.iconPosition != null
            && this.iconPosition.getLatitude() != null
            && this.iconPosition.getLongitude() != null)
        {
            RestorableSupport.StateObject positionStateObj = restorableSupport.addStateObject("position");
            if (positionStateObj != null)
            {
                restorableSupport.addStateValueAsDouble(positionStateObj, "latitude",
                    this.iconPosition.getLatitude().degrees);
                restorableSupport.addStateValueAsDouble(positionStateObj, "longitude",
                    this.iconPosition.getLongitude().degrees);
                restorableSupport.addStateValueAsDouble(positionStateObj, "elevation",
                    this.iconPosition.getElevation());
            }
        }

        if (this.iconSize != null)
        {
            RestorableSupport.StateObject sizeStateObj = restorableSupport.addStateObject("size");
            if (sizeStateObj != null)
            {
                restorableSupport.addStateValueAsDouble(sizeStateObj, "width", this.iconSize.getWidth());
                restorableSupport.addStateValueAsDouble(sizeStateObj, "height", this.iconSize.getHeight());
            }
        }

        if (this.toolTipText != null)
            restorableSupport.addStateValueAsString("toolTipText", this.toolTipText, true);

        // Save the name, style, and size of the font. These will be used to restore the font using the
        // constructor: new Font(name, style, size).
        if (this.toolTipFont != null)
        {
            RestorableSupport.StateObject toolTipFontStateObj = restorableSupport.addStateObject("toolTipFont");
            if (toolTipFontStateObj != null)
            {
                restorableSupport.addStateValueAsString(toolTipFontStateObj, "name", this.toolTipFont.getName());
                restorableSupport.addStateValueAsInteger(toolTipFontStateObj, "style", this.toolTipFont.getStyle());
                restorableSupport.addStateValueAsInteger(toolTipFontStateObj, "size", this.toolTipFont.getSize());
            }
        }

        if (this.textColor != null)
        {
            String encodedColor = RestorableSupport.encodeColor(this.textColor);
            if (encodedColor != null)
                restorableSupport.addStateValueAsString("toolTipTextColor", encodedColor);
        }

        // Save the backgroundImage property only when it is a simple String path. If the backgroundImage property is a
        // BufferedImage (or some other object), we make no effort to save that state. We save under the name
        // "backgroundImagePath" to denote that it is a special case of "backgroundImage".
        if (this.getBackgroundTexture() != null && this.getBackgroundTexture().getImageSource() instanceof String)
        {
            String backgroundImagePath = (String) this.getBackgroundTexture().getImageSource();
            restorableSupport.addStateValueAsString("backgroundImagePath", backgroundImagePath, true);
        }

        restorableSupport.addStateValueAsBoolean("highlighted", this.isHighlighted);
        restorableSupport.addStateValueAsDouble("highlightScale", this.highlightScale);
        restorableSupport.addStateValueAsBoolean("visible", this.isVisible);
        restorableSupport.addStateValueAsBoolean("showToolTip", this.showToolTip);
        restorableSupport.addStateValueAsBoolean("alwaysOnTop", this.alwaysOnTop);
        restorableSupport.addStateValueAsDouble("backgroundScale", this.backgroundScale);

        RestorableSupport.StateObject so = restorableSupport.addStateObject(null, "avlist");
        for (Map.Entry<String, Object> avp : this.getEntries())
        {
            restorableSupport.addStateValueAsString(so, avp.getKey(),
                avp.getValue() != null ? avp.getValue().toString() : "");
        }

        return restorableSupport.getStateAsXml();
    }

    /**
     * Restores publicly settable attribute values found in the specified XML state document String. The document
     * specified by <code>stateInXml</code> must be a well formed XML document String, or this will throw an
     * IllegalArgumentException. Unknown structures in <code>stateInXml</code> are benign, because they will simply be
     * ignored.
     *
     * @param stateInXml an XML document String describing a UserFacingIcon.
     *
     * @throws IllegalArgumentException If <code>stateInXml</code> is null, or if <code>stateInXml</code> is not a well
     *                                  formed XML document String.
     */
    public void restoreState(String stateInXml)
    {
        if (stateInXml == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        RestorableSupport restorableSupport;
        try
        {
            restorableSupport = RestorableSupport.parse(stateInXml);
        }
        catch (Exception e)
        {
            // Parsing the document specified by stateInXml failed.
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", stateInXml);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message, e);
        }

        // The imagePath property should exist only if the imageSource property was a simple String path.
        // If the imageSource property was a BufferedImage (or some other object), it should not exist in the
        // state document. We save under the name "imagePath" to denote that it is a special case of "imageSource".
        String s = restorableSupport.getStateValueAsString("imagePath");
        if (s != null)
            this.setImageSource(s);

        // Restore the position property only if all parts are available.
        // We will not restore a partial position (for example, just the elevation).
        RestorableSupport.StateObject so = restorableSupport.getStateObject("position");
        if (so != null)
        {
            Double lat = restorableSupport.getStateValueAsDouble(so, "latitude");
            Double lon = restorableSupport.getStateValueAsDouble(so, "longitude");
            Double elev = restorableSupport.getStateValueAsDouble(so, "elevation");
            if (lat != null && lon != null && elev != null)
                this.setPosition(Position.fromDegrees(lat, lon, elev));
        }

        // Restore the size property only if all parts are available.
        // We will not restore a partial size (for example, just the width).
        so = restorableSupport.getStateObject("size");
        if (so != null)
        {
            Double width = restorableSupport.getStateValueAsDouble(so, "width");
            Double height = restorableSupport.getStateValueAsDouble(so, "height");
            if (width != null && height != null)
                this.setSize(new Dimension(width.intValue(), height.intValue()));
        }

        s = restorableSupport.getStateValueAsString("toolTipText");
        if (s != null)
            this.setToolTipText(s);

        // Restore the toolTipFont property only if all parts are available.
        // We will not restore a partial toolTipFont (for example, just the size).
        so = restorableSupport.getStateObject("toolTipFont");
        if (so != null)
        {
            // The "font name" of toolTipFont.
            String name = restorableSupport.getStateValueAsString(so, "name");
            // The style attributes.
            Integer style = restorableSupport.getStateValueAsInteger(so, "style");
            // The simple font size.
            Integer size = restorableSupport.getStateValueAsInteger(so, "size");
            if (name != null && style != null && size != null)
                this.setToolTipFont(new Font(name, style, size));
        }

        s = restorableSupport.getStateValueAsString("toolTipTextColor");
        if (s != null)
        {
            Color color = RestorableSupport.decodeColor(s);
            if (color != null)
                this.setToolTipTextColor(color);
        }

        // The backgroundImagePath property should exist only if the backgroundImage property was a simple String path.
        // If the backgroundImage property was a BufferedImage (or some other object), it should not exist in the
        // state document. We save under the name "backgroundImagePath" to denote that it is a special case of
        // "backgroundImage".
        s = restorableSupport.getStateValueAsString("backgroundImagePath");
        if (s != null)
            this.setBackgroundImage(s);

        Boolean b = restorableSupport.getStateValueAsBoolean("highlighted");
        if (b != null)
            this.setHighlighted(b);

        Double d = restorableSupport.getStateValueAsDouble("highlightScale");
        if (d != null)
            this.setHighlightScale(d);

        b = restorableSupport.getStateValueAsBoolean("visible");
        if (b != null)
            this.setVisible(b);

        b = restorableSupport.getStateValueAsBoolean("showToolTip");
        if (b != null)
            this.setShowToolTip(b);

        b = restorableSupport.getStateValueAsBoolean("alwaysOnTop");
        if (b != null)
            this.setAlwaysOnTop(b);

        d = restorableSupport.getStateValueAsDouble("backgroundScale");
        if (d != null)
            this.setBackgroundScale(d);

        so = restorableSupport.getStateObject(null, "avlist");
        if (so != null)
        {
            RestorableSupport.StateObject[] avpairs = restorableSupport.getAllStateObjects(so, "");
            if (avpairs != null)
            {
                for (RestorableSupport.StateObject avp : avpairs)
                {
                    if (avp != null)
                        this.setValue(avp.getName(), avp.getValue());
                }
            }
        }
    }
}
