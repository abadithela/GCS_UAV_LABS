/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples.util;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.*;

import javax.media.opengl.GL;

/**
 * @author dcollins
 * @version $Id: ImageAnnotation.java 10616 2009-04-28 23:28:57Z dcollins $
 */
public class ImageAnnotation extends ScreenAnnotation
{
    protected boolean fitSizeToImage;
    protected boolean useImageAspectRatio;
    protected boolean enableSmoothing;
    protected boolean useMipmaps;
    // Tool tip state.
    protected boolean showToolTip;
    protected String toolTipText;
    protected java.awt.Point toolTipPoint;

    public ImageAnnotation(Object imageSource)
    {
        super("", new java.awt.Point());

        this.fitSizeToImage = true;
        this.useImageAspectRatio = true;
        this.enableSmoothing = true;
        this.useMipmaps = true;

        this.setImageSource(imageSource);
        this.setupAnnotationAttributes(this);
    }

    public ImageAnnotation()
    {
        this(null);
    }

    public boolean isFitSizeToImage()
    {
        return this.fitSizeToImage;
    }

    public void setFitSizeToImage(boolean fitSizeToImage)
    {
        this.fitSizeToImage = fitSizeToImage;
    }

    public boolean isUseImageAspectRatio()
    {
        return this.useImageAspectRatio;
    }

    public void setUseImageAspectRatio(boolean useImageAspectRatio)
    {
        this.useImageAspectRatio = useImageAspectRatio;
    }

    public boolean isEnableSmoothing()
    {
        return this.enableSmoothing;
    }

    public void setEnableSmoothing(boolean enable)
    {
        this.enableSmoothing = enable;
    }

    public boolean isUseMipmaps()
    {
        return this.useMipmaps;
    }

    public void setUseMipmaps(boolean useMipmaps)
    {
        this.useMipmaps = useMipmaps;
    }

    public Object getImageSource()
    {
        return this.getAttributes().getImageSource();
    }

    public void setImageSource(Object source)
    {
        this.getAttributes().setImageSource(source);
    }

    public boolean isShowToolTip()
    {
        return this.showToolTip;
    }

    public void setShowToolTip(boolean show)
    {
        this.showToolTip = show;
    }

    public String getToolTipText()
    {
        return this.toolTipText;
    }

    public void setToolTipText(String toolTipText)
    {
        this.toolTipText = toolTipText;
    }

    public java.awt.Point getToolTipPoint()
    {
        return toolTipPoint;
    }

    public void setToolTipPoint(java.awt.Point toolTipPoint)
    {
        this.toolTipPoint = toolTipPoint;
    }

    //**************************************************************//
    //********************  Preferred Size  ************************//
    //**************************************************************//

    public java.awt.Dimension getPreferredSize(DrawContext dc)
    {
        java.awt.Dimension imageSize = this.getImageDimensions();
        java.awt.Dimension insetSize = null;

        // Optionally set the annotation's inset size to the image size.
        if (this.isFitSizeToImage())
        {
            insetSize = imageSize;
        }

        // Fallback to the superclass preferred size.
        if (insetSize == null)
        {
            insetSize = super.getPreferredSize(dc);

            // Optionally set the annotation's aspect ratio to that of the image. We'll use the superclass width, and
            // override it's height to match the image's aspect ration.
            if (this.isUseImageAspectRatio() && imageSize != null)
            {
                double aspect = imageSize.getHeight() / imageSize.getWidth();
                insetSize = new java.awt.Dimension(insetSize.width, (int) Math.round(aspect * insetSize.width));
            }
        }

        java.awt.Insets insets = this.getAttributes().getInsets();
        return new java.awt.Dimension(
            insetSize.width + (insets.left + insets.right),
            insetSize.height + (insets.top + insets.bottom));
    }

    protected java.awt.Dimension getImageDimensions(AnnotationTexture texture)
    {
        if (texture == null)
            return null;

        int[] imageDimensions = texture.getSourceDimensions();
        if (imageDimensions == null)
            return null;

        return new java.awt.Dimension(imageDimensions[0], imageDimensions[1]);
    }

    protected java.awt.Dimension getImageDimensions()
    {
        Object imageSource = this.getAttributes().getImageSource();
        if (imageSource == null)
            return null;

        AnnotationTexture texture = this.getAnnotationTexture(imageSource);
        if (texture == null)
            return null;

        return this.getImageDimensions(texture);
    }

    //**************************************************************//
    //********************  Rendering  *****************************//
    //**************************************************************//

    public void drawContent(DrawContext dc, int width, int height, double opacity, Position pickPosition)
    {
        super.drawContent(dc, width, height, opacity, pickPosition);
        this.drawToolTip(dc);
    }

    protected void drawToolTip(DrawContext dc)
    {
        if (dc.isPickingMode())
            return;

        if (!this.isShowToolTip())
            return;

        String text = this.getToolTipText();
        if (text == null)
            return;

        java.awt.Point point = this.getToolTipPoint();
        if (point == null)
            return;

        this.doDrawToolTip(dc, text, point.x, point.y);
    }

    protected void doDrawToolTip(DrawContext dc, String text, int x, int y)
    {
        ToolTip toolTip = new ToolTip(text, x, y);
        dc.addOrderedRenderable(toolTip);
    }

    protected void applyBackgroundTextureState(DrawContext dc, int width, int height, double opacity,
        AnnotationTexture texture)
    {
        super.applyBackgroundTextureState(dc, width, height, opacity, texture);

        // Setup the texture filters to correspond to the smoothing and mipmap settings.
        int minFilter = this.isEnableSmoothing() ?
            (this.isUseMipmaps() ? GL.GL_LINEAR_MIPMAP_LINEAR : GL.GL_LINEAR) : GL.GL_NEAREST;
        int magFilter = this.isEnableSmoothing() ?
            GL.GL_LINEAR : GL.GL_NEAREST;

        GL gl = dc.getGL();
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, minFilter);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, magFilter);
    }

    protected void applyBackgroundTextureTransform(DrawContext dc, int width, int height, AnnotationTexture texture)
    {
        this.adjustTextureToSize(dc, width, height, texture);
        super.applyBackgroundTextureTransform(dc, width, height, texture);
    }

    protected void adjustTextureToSize(DrawContext dc, int width, int height, AnnotationTexture texture)
    {
        GL gl = dc.getGL();

        java.awt.Dimension size = this.getImageDimensions(texture);
        double scaleX = size.getWidth() / (double) width;
        double scaleY = size.getHeight() / (double) height;

        gl.glScaled(scaleX, scaleY, 1);
    }
    
    //**************************************************************//
    //********************  Utilities  *****************************//
    //**************************************************************//

    protected void setupAnnotationAttributes(Annotation annotation)
    {
        java.awt.Color transparentBlack = new java.awt.Color(0, 0, 0, 0);

        AnnotationAttributes defaultAttribs = new AnnotationAttributes();
        defaultAttribs.setAdjustWidthToText(Annotation.SIZE_FIXED);
        defaultAttribs.setBackgroundColor(transparentBlack);
        defaultAttribs.setBorderColor(transparentBlack);
        defaultAttribs.setBorderWidth(0);
        defaultAttribs.setCornerRadius(0);
        defaultAttribs.setDrawOffset(new java.awt.Point(0, 0));
        defaultAttribs.setHighlightScale(1);
        defaultAttribs.setInsets(new java.awt.Insets(0, 0, 0, 0));
        defaultAttribs.setImageScale(1);
        defaultAttribs.setImageOffset(new java.awt.Point(0, 0));
        defaultAttribs.setImageOpacity(1);
        defaultAttribs.setImageRepeat(Annotation.IMAGE_REPEAT_NONE);
        defaultAttribs.setLeader(FrameFactory.LEADER_NONE);
        defaultAttribs.setSize(new java.awt.Dimension(0, 0));

        annotation.setPickEnabled(false);
        annotation.getAttributes().setDefaults(defaultAttribs);
    }
}
