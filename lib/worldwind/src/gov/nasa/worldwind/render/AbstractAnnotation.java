/*
Copyright (C) 2001, 2006, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import com.sun.opengl.util.j2d.TextRenderer;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.pick.*;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.GL;
import java.awt.*;

/**
 * An {@link Annotation} represent a text label and its rendering attributes. Annotations must be attached either to
 * a globe <code>Position</code> ({@link GlobeAnnotation}) or a viewport <code>Point</code> (ScreenAnnotation).
 * <p/>
 * <pre>
 * GlobaAnnotation ga = new  GlobeAnnotation("Lat-Lon zero", Position.fromDegrees(0, 0, 0)));
 * ScreenAnnotation sa = new ScreenAnnotation("Message...", new Point(10,10));
 * </pre>
 * <p>
 * Each Annotation refers to an {@link AnnotationAttributes} object which defines how the text will be rendered.
 * </p>
 * Rendering attributes allow to set:
 * <ul>
 * <li>the size of the bounding rectangle into which the text will be displayed</li>
 * <li>its frame shape, border color, width and stippling pattern</li>
 * <li>the text font, size, style and color</li>
 * <li>the background color or image</li>
 * <li>how much an annotation scales and fades with distance</li>
 * </ul>
 * <pre>
 * ga.getAttributes().setTextColor(Color.WHITE);
 * ga.getAttributes().setFont(Font.decode("Arial-BOLD-24");
 * ...
 * </pre>
 * <p/>
 * Annotations are usually handled by an {@link gov.nasa.worldwind.layers.AnnotationLayer}. Although they also implement the {@link Renderable}
 * and {@link Pickable} interfaces and thus can be handled by a {@link gov.nasa.worldwind.layers.RenderableLayer} too.
 * <p/>
 * <pre>
 * AnnotationLayer layer = new AnnotationLayer();
 * layer.addAnnotation(new GlobeAnnotation("Text...", Position.fromDegrees(0, 0, 0)));
 * </pre>
 * <p/>
 * Each Annotation starts its life with a fresh attribute set that can be altered to produce the desired effect.
 * However, <code>AnnotationAttributes</code> can be set and shared between annotations allowing to control the rendering attributes
 * of many annotations from a single <code>AnnotationAttributes</code> object.
 * <p/>
 * <pre>
 * AnnotationAttributes attr = new AnnotationAttributes();
 * attr.setTextColor(Color.WHITE);
 * attr.setFont(Font.decode("Arial-BOLD-24");
 * ga.setAttributes(attr);
 * </pre>
 * <p/>
 * In the above example changing the text color of the attributes set will affect all annotations refering it. However,
 * changing the text color of one of those annotations will also affect all others since it will in fact change the
 * common attributes set.
 * <p>
 * To use an attributes object only as default values for a serie of annotations use:
 * </p>
 * <pre>
 * ga.getAttributes()setDefaults(attr);
 * </pre>
 * <p/>
 * which can also be done in the Annotation constructor:
 * <p/>
 * <pre>
 * GlobeAnnotation ga = new GlobeAnnotation(text, position, attr);
 * </pre>
 * <p/>
 * Finer control over attributes inheritence can be achieved using default or fallback attributes set.
 * <p>
 * Most attributes can be set to a 'use default' value which is minus one for numeric values and <code>null</code> for attributes
 * refering objects (colors, dimensions, insets..). In such a case the value of an attribute will be that of the
 * default attribute set. New annotations have all their attributes set to use default values.
 * </p>
 * <p/>
 * Each <code>AnnotationAttributes</code> object points to a default static attributes set which is the fallback source for
 * attributes with  <code>null</code> or <code>-1</code> values. This default attributes set can be set to any attributes object other than the
 * static one.
 * <p/>
 * <pre>
 * AnnotationAttributes geoFeature = new AnnotationAttributes();
 * geoFeature.setFrameShape(FrameFactory.SHAPE_ELLIPSE);
 * geoFeature.setInsets(new Insets(12, 12, 12, 12));
 * <p/>
 * AnnotationAttributes waterBody = new AnnotationAttributes();
 * waterBody.setTextColor(Color.BLUE);
 * waterBoby.setDefaults(geoFeature);
 * <p/>
 * AnnotationAttributes mountain = new AnnotationAttributes();
 * mountain.setTextColor(Color.GREEN);
 * mountain.setDefaults(geoFeature);
 * <p/>
 * layer.addAnnotation(new GlobeAnnotation("Spirit Lake", Position.fromDegrees(46.26, -122.15), waterBody);
 * layer.addAnnotation(new GlobeAnnotation("Mt St-Helens", Position.fromDegrees(46.20, -122.19), mountain);
 * </pre>
 * <p/>
 * In the above example all geographic features have an ellipse shape, water bodies and mountains use that attributes
 * set has defaults and have their own text colors. They are in turn used as defaults by the two annotations. Mount
 * Saint Helens attributes could be changed without affecting other mountains. However, changes on the geoFeatures
 * attributes would affect all mountains and lakes.
 *
 * @author Patrick Murris
 * @version $Id$
 * @see AnnotationAttributes
 * @see AnnotationRenderer
 */
public abstract class AbstractAnnotation extends AVListImpl implements Annotation
{
    private boolean alwaysOnTop;
    private boolean pickEnabled;
    protected String text;
    protected AnnotationAttributes attributes;
    // Child annotation properties.
    protected java.util.List<Annotation> childList;
    protected AnnotationLayoutManager layoutManager;
    // Picking components.
    private PickSupport pickSupport;
    protected Object delegateOwner;
    // Properties used or computed in each rendering pass.
    private static java.nio.DoubleBuffer vertexBuffer;
    protected java.util.Map<Object, String> wrappedTextMap;
    protected java.util.Map<Object, java.awt.Rectangle> textBoundsMap;

    protected AbstractAnnotation()
    {
        this.alwaysOnTop = false;
        this.pickEnabled = true;
        this.attributes = new AnnotationAttributes();
        this.childList = new java.util.ArrayList<Annotation>();
        this.layoutManager = new AnnotationNullLayout();
        // Cached text computations.
        this.wrappedTextMap = new java.util.HashMap<Object, String>();
        this.textBoundsMap = new java.util.HashMap<Object, java.awt.Rectangle>();

        if (!WorldWind.getMemoryCacheSet().containsCache(AnnotationTexture.class.getName()))
        {
            long size = Configuration.getLongValue(AVKey.ANNOTATION_IMAGE_SOURCE_CACHE_SIZE, 3000000L);
            MemoryCache cache = new BasicMemoryCache((long) (0.85 * size), size);
            cache.setName("Annotation Images");
            WorldWind.getMemoryCacheSet().addCache(AnnotationTexture.class.getName(), cache);
        }
    }

    public boolean isAlwaysOnTop()
    {
        return alwaysOnTop;
    }

    public void setAlwaysOnTop(boolean alwaysOnTop)
    {
        this.alwaysOnTop = alwaysOnTop;
    }

    public boolean isPickEnabled()
    {
        return this.pickEnabled;
    }

    public void setPickEnabled(boolean enable)
    {
        this.pickEnabled = enable;
    }

    public String getText()
    {
        return this.text;
    }

    public void setText(String text)
    {
        if (text == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.text = text;
    }

    public AnnotationAttributes getAttributes()
    {
        return this.attributes;
    }

    public void setAttributes(AnnotationAttributes attributes)
    {
        if (attributes == null)
        {
            String message = Logging.getMessage("nullValue.AnnotationAttributesIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.attributes = attributes;
    }

    public java.util.List<? extends Annotation> getChildren()
    {
        return java.util.Collections.unmodifiableList(this.childList);
    }

    public void addChild(Annotation annotation)
    {
        if (annotation == null)
        {
            String message  = Logging.getMessage("nullValue.AnnotationIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.childList.add(annotation);
    }

    public boolean removeChild(Annotation annotation)
    {
        if (annotation == null)
        {
            String message  = Logging.getMessage("nullValue.AnnotationIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.childList.remove(annotation);
    }

    public void removeAllChildren()
    {
        this.childList.clear();
    }

    public AnnotationLayoutManager getLayout()
    {
        return this.layoutManager;
    }

    public void setLayout(AnnotationLayoutManager layoutManager)
    {
        if (layoutManager == null)
        {
            String message  = Logging.getMessage("nullValue.LayoutIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.layoutManager = layoutManager;
    }

    public PickSupport getPickSupport()
    {
        return this.pickSupport;
    }

    public void setPickSupport(PickSupport pickSupport)
    {
        if (pickSupport == null)
        {
            String message = Logging.getMessage("nullValue.PickSupportIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.pickSupport = pickSupport;
    }

    public Object getDelegateOwner()
    {
        return delegateOwner;
    }

    public void setDelegateOwner(Object delegateOwner)
    {
        this.delegateOwner = delegateOwner;
    }
    
    /**
     * Render the annotation. Called as a Renderable.
     *
     * @param dc the current DrawContext.
     */
    public void render(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!this.getAttributes().isVisible())
            return;

        dc.getAnnotationRenderer().render(dc, this, null, dc.getCurrentLayer());
    }

    /**
     * Pick at the annotation. Called as a Pickable.
     *
     * @param dc the current DrawContext.
     */
    public void pick(DrawContext dc, java.awt.Point pickPoint)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!this.getAttributes().isVisible())
            return;

        if (!this.isPickEnabled())
            return;

        dc.getAnnotationRenderer().pick(dc, this, null, pickPoint, null);
    }

    public void dispose()
    {
    }

    public java.awt.Dimension getPreferredSize(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Clamp the caller specified size.
        java.awt.Dimension size = new java.awt.Dimension(this.getAttributes().getSize());
        if (size.width < 1)
            size.width = 1;
        if (size.height < 0)
            size.height = 0;

        // Compute the size of this annotation's inset region.
        java.awt.Rectangle insetBounds = this.computeInsetBounds(size.width, size.height);
        java.awt.Dimension insetSize = new java.awt.Dimension(insetBounds.width, insetBounds.height);

        // Wrap the text to fit inside the annotation's inset bounds. Then adjust the inset bounds to the wrapped
        // text, depending on the annotation's attributes.
        insetSize = this.adjustSizeToText(dc, insetSize.width, insetSize.height);

        // Adjust the inset bounds to the child annotations.
        insetSize = this.adjustSizeToChildren(dc, insetSize.width, insetSize.height);

        java.awt.Insets insets = this.getAttributes().getInsets();
        return new java.awt.Dimension(
            insetSize.width + (insets.left + insets.right),
            insetSize.height + (insets.top + insets.bottom));
    }

    public void renderNow(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!this.getAttributes().isVisible())
            return;

        if (dc.isPickingMode() && !this.isPickEnabled())
            return;

        this.doRenderNow(dc);
    }

    public void draw(DrawContext dc, int width, int height, double opacity, Position pickPosition)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double finalOpacity = opacity * this.computeOpacity(dc);
        this.doDraw(dc, width, height, finalOpacity, pickPosition);
    }

    protected void drawTopLevelAnnotation(DrawContext dc, int x, int y, int width, int height, double scale,
        double opacity, Position pickPosition)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        OGLStackHandler stackHandler = new OGLStackHandler();
        this.beginDraw(dc, stackHandler);
        try
        {
            this.applyScreenTransform(dc, x, y, width, height, scale);
            this.draw(dc, width, height, opacity, pickPosition);
        }
        finally
        {
            this.endDraw(dc, stackHandler);
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void applyScreenTransform(DrawContext dc, int x, int y, int width, int height, double scale)
    {
        double finalScale = scale * this.computeScale(dc);
        java.awt.Point offset = this.getAttributes().getDrawOffset();

        GL gl = dc.getGL();
        gl.glTranslated(x, y, 0);
        gl.glScaled(finalScale, finalScale, 1);
        gl.glTranslated(offset.x, offset.y, 0);
        gl.glTranslated(-width / 2, 0, 0);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected double computeScale(DrawContext dc)
    {
        double scale = this.attributes.getScale();

        // Factor in highlight scale.
        if (this.attributes.isHighlighted())
        {
            scale *= this.attributes.getHighlightScale();
        }

        return scale;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected double computeOpacity(DrawContext dc)
    {
        double opacity = this.attributes.getOpacity();

        // Remove transparency if highlighted.
        if (this.attributes.isHighlighted())
        {
            opacity = 1;
        }

        return opacity;
    }

    //**************************************************************//
    //********************  Rendering  *****************************//
    //**************************************************************//

    protected abstract void doRenderNow(DrawContext dc);
    protected abstract Rectangle computeBounds(DrawContext dc);

    /**
     * Get the annotation bounding {@link java.awt.Rectangle} using OGL coordinates - bottom-left corner x and y
     * relative to the {@link WorldWindow} bottom-left corner, and the annotation callout width and height.
     * <p>
     * The annotation offset from it's reference point is factored in such that the callout leader shape
     * and reference point are included in the bounding rectangle.
     *
     * @param dc the current DrawContext.
     * @return the annotation bounding {@link java.awt.Rectangle} using OGL viewport coordinates.
     *
     * @throws IllegalArgumentException if <code>dc</code> is null.
     */
    public java.awt.Rectangle getBounds(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (dc.getView().getViewport() == null)
            return null;

        return this.computeBounds(dc);
    }

    /**
     * Draws an annotation with the given dimensions and opacity. Current GL state has ortho identity model
     * view active with origin at the screen point.
     *
     * @param dc           current DrawContext.
     * @param width        annotation callout width
     * @param height       annotation callout height
     * @param opacity      opacity to apply
     * @param pickPosition <code>Position</code> that will be associated with any <code>PickedObject</code>
     *                     produced during picking.
     */
    protected void doDraw(DrawContext dc, int width, int height, double opacity, Position pickPosition)
    {
        if (!this.getAttributes().isVisible())
            return;

        // If this annotation is not pickable, then do not draw any of its contents. However this annotation's children
        // may be pickable, so we still process them.
        if (!dc.isPickingMode() || this.isPickEnabled())
        {
            this.drawContent(dc, width, height, opacity, pickPosition);
        }

        this.drawChildren(dc, width, height, opacity, pickPosition);
    }

    protected void drawContent(DrawContext dc, int width, int height, double opacity, Position pickPosition)
    {
        this.drawBackground(dc, width, height, opacity, pickPosition);
        this.drawBackgroundImage(dc, width, height, opacity, pickPosition);
        this.drawBorder(dc, width, height, opacity, pickPosition);
        this.drawText(dc, width, height, opacity, pickPosition);
    }

    protected void beginDraw(DrawContext dc, OGLStackHandler stackHandler)
    {
        GL gl = dc.getGL();
        stackHandler.pushModelviewIdentity(gl);
    }

    protected void endDraw(DrawContext dc, OGLStackHandler stackHandler)
    {
        GL gl = dc.getGL();
        stackHandler.pop(gl);
    }

    //**************************************************************//
    //********************  Background Rendering  ******************//
    //**************************************************************//

    protected void drawBackground(DrawContext dc, int width, int height, double opacity, Position pickPosition)
    {
        if (dc.isPickingMode())
        {
            this.bindPickableObject(dc, pickPosition);
        }

        this.setDrawColor(dc, this.getAttributes().getBackgroundColor(), opacity,
                OGLStateSupport.COLOR_PREMULTIPLIED_ALPHA);
        this.drawCallout(dc, GL.GL_TRIANGLE_FAN, width, height, false);
    }

    //**************************************************************//
    //********************  Background Image Rendering  ************//
    //**************************************************************//

    protected void drawBackgroundImage(DrawContext dc, int width, int height, double opacity, Position pickPosition)
    {
        if (dc.isPickingMode())
            return;

        Object imageSource = this.getAttributes().getImageSource();
        if (imageSource == null)
            return;

        AnnotationTexture texture = this.getAnnotationTexture(imageSource);
        if (texture == null)
            return;

        if (!texture.bind(dc))
            return;

        this.applyBackgroundTextureState(dc, width, height, opacity, texture);
        this.doDrawBackgroundTexture(dc, width, height, opacity, pickPosition, texture);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void doDrawBackgroundTexture(DrawContext dc, int width, int height, double opacity, Position pickPosition,
        AnnotationTexture texture)
    {
        GL gl = dc.getGL();
        OGLStackHandler stackHandler = new OGLStackHandler();

        // Apply texture transform state.
        stackHandler.pushTextureIdentity(gl);
        try
        {
            // Enable texture state.
            gl.glEnable(GL.GL_TEXTURE_2D);
            this.applyBackgroundTextureTransform(dc, width, height, texture);
            // Draw the annotaiton callout with the background texture enabled.
            this.drawCallout(dc, GL.GL_TRIANGLE_FAN, width, height, true);
        }
        finally
        {
            // Disable texture state.
            gl.glDisable(GL.GL_TEXTURE_2D);
            // Restore the matrix stacks and matrix mode.
            stackHandler.pop(gl);
            gl.glMatrixMode(GL.GL_MODELVIEW);
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void applyBackgroundTextureState(DrawContext dc, int width, int height, double opacity,
        AnnotationTexture texture)
    {
        GL gl = dc.getGL();

        // Apply texture wrap state.
        String imageRepeat = this.getAttributes().getImageRepeat();
        int sWrap = (imageRepeat.equals(Annotation.IMAGE_REPEAT_X) || imageRepeat.equals(Annotation.IMAGE_REPEAT_XY)) ?
            GL.GL_REPEAT : GL.GL_CLAMP_TO_BORDER;
        int tWrap = (imageRepeat.equals(Annotation.IMAGE_REPEAT_Y) || imageRepeat.equals(Annotation.IMAGE_REPEAT_XY)) ?
            GL.GL_REPEAT : GL.GL_CLAMP_TO_BORDER;
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, sWrap);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, tWrap);

        // Apply texture environment state.
        gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);

        // Apply blending and color state.
        double imageOpacity = opacity * this.getAttributes().getImageOpacity();
        this.setDrawColor(dc, java.awt.Color.WHITE, imageOpacity, OGLStateSupport.COLOR_PREMULTIPLIED_ALPHA);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void applyBackgroundTextureTransform(DrawContext dc, int width, int height, AnnotationTexture texture)
    {
        GL gl = dc.getGL();

        // Apply texture transform state. Assume the texture mode is GL_TEXTURE.
        texture.applyTransform(dc);

        // The image scale is applied inversely, because texture coordinates inversely proprortional to the image
        // size on screen.
        double imageScale = this.getAttributes().getImageScale();
        gl.glScaled(1.0 / imageScale, 1.0 / imageScale, 1);

        // The image offset is applied inversely, because a decrease in texture coordinates will shift the texture
        // in a positive direction.
        java.awt.Point imageOffset = this.getAttributes().getImageOffset();
        if (imageOffset != null)
        {
            gl.glTranslated(-imageOffset.x, -imageOffset.y, 0);
        }

        // Move the origin to the upper left hand corner.
        gl.glScaled(1, -1, 1);
        gl.glTranslated(0, -height, 0);
    }

    protected AnnotationTexture getAnnotationTexture(Object imageSource)
    {
        if (imageSource == null)
            return null;

        AnnotationTexture texture = this.getCachedImage(imageSource);
        if (texture == null)
        {
            texture = new AnnotationTexture(imageSource);
            this.setCachedImage(imageSource, texture);
        }

        return texture;
    }

    protected AnnotationTexture getCachedImage(Object source)
    {
        return (AnnotationTexture) WorldWind.getMemoryCache(AnnotationTexture.class.getName()).getObject(source);
    }

    protected void setCachedImage(Object source, AnnotationTexture texture)
    {
        WorldWind.getMemoryCache(AnnotationTexture.class.getName()).add(source, texture);
    }
    
    //**************************************************************//
    //********************  Border Rendering  **********************//
    //**************************************************************//

    @SuppressWarnings({"UnusedDeclaration"})
    protected void drawBorder(DrawContext dc, int width, int height, double opacity, Position pickPosition)
    {
        if (this.getAttributes().getBorderWidth() <= 0)
            return;

        GL gl = dc.getGL();

        // Apply line smoothing state.
        if (dc.isPickingMode())
        {
            gl.glDisable(GL.GL_LINE_SMOOTH);
        }
        else
        {
            gl.glEnable(GL.GL_LINE_SMOOTH);
            gl.glHint(GL.GL_LINE_SMOOTH_HINT, this.getAttributes().getAntiAliasHint());
        }
        
        // Apply line stipple state.
        if (dc.isPickingMode() || (this.getAttributes().getBorderStippleFactor() <= 0))
        {
            gl.glDisable(GL.GL_LINE_STIPPLE);
        }
        else
        {
            gl.glEnable(GL.GL_LINE_STIPPLE);
            gl.glLineStipple(
                this.getAttributes().getBorderStippleFactor(),
                this.getAttributes().getBorderStipplePattern());
        }

        // Apply line width state.
        gl.glLineWidth((float) this.getAttributes().getBorderWidth());
        
        // Apply blending and color state.
        this.setDrawColor(dc, this.getAttributes().getBorderColor(), opacity,
            OGLStateSupport.COLOR_NO_PREMULTIPLIED_ALPHA);

        this.drawCallout(dc, GL.GL_LINE_STRIP, width, height, false);
    }

    //**************************************************************//
    //********************  Text Rendering  ************************//
    //**************************************************************//

    protected void drawText(DrawContext dc, int width, int height, double opacity, Position pickPosition)
    {
        AnnotationAttributes attribs = this.getAttributes();

        String text = this.getText();
        if (text == null || text.length() == 0)
            return;

        java.awt.Rectangle insetBounds = this.computeInsetBounds(width, height);

        // If we're in picking mode and the pick point does not intersect the annotation's inset bounds in screen space,
        // then exit.
        if (dc.isPickingMode())
        {
            if (dc.getPickPoint() == null)
                return;

            java.awt.Rectangle screenInsetBounds = this.transformByModelview(dc, insetBounds);
            java.awt.Point glPickPoint = this.glPointFromAWTPoint(dc, dc.getPickPoint());
            if (!screenInsetBounds.contains(glPickPoint))
                return;
        }

        // Wrap the text to the annotation's inset bounds.
        String wrappedText = this.getWrappedText(dc, insetBounds.width, insetBounds.height, text, attribs.getFont(),
            attribs.getTextAlign());
        java.awt.Rectangle wrappedTextBounds = this.getTextBounds(dc, wrappedText, attribs.getFont(),
            attribs.getTextAlign());
        int baselineOffset = (int) (wrappedTextBounds.y / 6.0); // TODO: why is baseline offset computed this way?
        
        int x = insetBounds.x;
        int y = insetBounds.y + baselineOffset + 2; // TODO: why does this y-coordinate have an additional +2?

        // Adjust the text x-coordinate according to the text alignment property.
        if (attribs.getTextAlign().equals(AVKey.CENTER))
        {
            x = (int) insetBounds.getCenterX();
        }
        else if (attribs.getTextAlign().equals(AVKey.RIGHT))
        {
            x = (int) insetBounds.getMaxX();
        }

        // Adjust the text y-coordinate to fit inside the annotation's inset region.
        if (insetBounds.height > 0)
        {
            y += insetBounds.height;
        }
        else
        {
            y += wrappedTextBounds.height;
        }

        int lineHeight = (int) wrappedTextBounds.getMinY();
        Object pickObject = (this.delegateOwner != null) ? this.delegateOwner : this;
        
        this.drawText(dc, x, y, lineHeight, opacity, pickObject, pickPosition, wrappedText);
    }

    protected void drawText(DrawContext dc, int x, int y, int lineHeight, double opacity, Object pickObject,
        Position pickPosition, String text)
    {
        boolean isHTML = MultiLineTextRenderer.containsHTML(text);
        if (isHTML)
        {
            this.drawHTML(dc, x, y, lineHeight, opacity, pickObject, pickPosition, text);
        }
        else
        {
            this.drawPlainText(dc, x, y, lineHeight, opacity, pickObject, pickPosition, text);
        }
    }

    protected void drawPlainText(DrawContext dc, int x, int y, int lineHeight, double opacity, Object pickObject,
        Position pickPosition, String text)
    {
        AnnotationAttributes attribs = this.getAttributes();
        MultiLineTextRenderer mltr = this.getMultiLineTextRenderer(dc, attribs.getFont(), attribs.getTextAlign());

        java.awt.Color textColor = this.modulateColorOpacity(attribs.getTextColor(), opacity);
        java.awt.Color backColor = this.modulateColorOpacity(attribs.getBackgroundColor(), opacity);
        mltr.setTextColor(textColor);
        mltr.setBackColor(backColor);

        if (dc.isPickingMode())
        {
            mltr.pick(text, x, y, lineHeight, dc, this.pickSupport, pickObject, pickPosition);
        }
        else
        {
            mltr.getTextRenderer().begin3DRendering();
            try
            {
                mltr.draw(text, x, y, lineHeight, attribs.getEffect());
            }
            finally
            {
                mltr.getTextRenderer().end3DRendering();
            }
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void drawHTML(DrawContext dc, int x, int y, int lineHeight, double opacity, Object pickObject,
        Position pickPosition, String text)
    {
        AnnotationAttributes attribs = this.getAttributes();
        MultiLineTextRenderer mltr = this.getMultiLineTextRenderer(dc, attribs.getFont(), attribs.getTextAlign());

        java.awt.Color textColor = this.modulateColorOpacity(attribs.getTextColor(), opacity);
        java.awt.Color backColor = this.modulateColorOpacity(attribs.getBackgroundColor(), opacity);
        mltr.setTextColor(textColor);
        mltr.setBackColor(backColor);

        if (dc.isPickingMode())
        {
            mltr.pickHTML(text, x, y, dc.getTextRendererCache(), dc, this.pickSupport, pickObject, pickPosition);
        }
        else
        {
            mltr.drawHTML(text, x, y, dc.getTextRendererCache());
        }
    }

    //**************************************************************//
    //********************  Recursive Child Rendering  *************//
    //**************************************************************//

    protected void drawChildren(DrawContext dc, int width, int height, double opacity, Position pickPosition)
    {
        if (this.childList.isEmpty())
            return;

        java.awt.Rectangle insetBounds = this.computeInsetBounds(width, height);

        this.beginDrawChildren(dc, insetBounds);
        try
        {
            this.doDrawChildren(dc, insetBounds, opacity, pickPosition);
        }
        finally
        {
            this.endDrawChildren(dc);
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void doDrawChildren(DrawContext dc, java.awt.Rectangle bounds, double opacity, Position pickPosition)
    {
        this.layoutManager.setPickSupport(this.pickSupport);
        this.layoutManager.drawAnnotations(dc, bounds, this.childList, opacity, pickPosition);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void beginDrawChildren(DrawContext dc, java.awt.Rectangle bounds)
    {
        this.layoutManager.beginDrawAnnotations(dc, bounds);
    }

    protected void endDrawChildren(DrawContext dc)
    {
        this.layoutManager.endDrawAnnotations(dc);
    }
    
    //**************************************************************//
    //********************  Rendering Support  *********************//
    //**************************************************************//

    protected void bindPickableObject(DrawContext dc, Position position)
    {
        java.awt.Color color = dc.getUniquePickColor();
        int colorCode = color.getRGB();
        Object object = (this.delegateOwner != null) ? this.delegateOwner : this;
        this.pickSupport.addPickableObject(colorCode, object, position, false);

        dc.getGL().glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
    }

    protected void drawCallout(DrawContext dc, int mode, int width, int height, boolean useTexCoords)
    {
        String shape = this.getAttributes().getFrameShape();
        if (shape == null)
            return;

        java.awt.Point offset = this.getAttributes().getDrawOffset();
        java.awt.Point leaderOffset = new java.awt.Point((width / 2) - offset.x, -offset.y);
        int leaderGapWidth = this.getAttributes().getLeaderGapWidth();
        int cornerRadius = this.getAttributes().getCornerRadius();

        java.nio.DoubleBuffer buffer = vertexBuffer;
        if (this.getAttributes().getLeader().equals(FrameFactory.LEADER_TRIANGLE))
        {
            buffer = FrameFactory.createShapeWithLeaderBuffer(shape, width, height, leaderOffset, leaderGapWidth, 
                cornerRadius, buffer);
        }
        else
        {
            buffer = FrameFactory.createShapeBuffer(shape, width, height, cornerRadius, buffer);
        }

        if (buffer != null)
            vertexBuffer = buffer;

        if (buffer == null)
            return;

        int count = buffer.remaining() / 2;

        if (useTexCoords)
        {
            FrameFactory.drawBuffer(dc, mode, count, buffer, buffer);
        }
        else
        {
            FrameFactory.drawBuffer(dc, mode, count, buffer);
        }
    }

    protected void setDrawColor(DrawContext dc, java.awt.Color color, double opacity, String colorMode)
    {
        double finalOpacity = opacity * (color.getAlpha() / 255.0);

        OGLStateSupport stateSupport = new OGLStateSupport();
        stateSupport.setEnableBlending(!dc.isPickingMode());
        stateSupport.setEnableColor(!dc.isPickingMode());
        stateSupport.setColorMode(colorMode);
        stateSupport.setColor(color, finalOpacity);
        stateSupport.apply(dc.getGL());
    }

    protected java.awt.Color modulateColorOpacity(java.awt.Color color, double opacity)
    {
        float[] compArray = new float[4];
        color.getRGBComponents(compArray);
        compArray[3] *= (float) opacity;

        return new java.awt.Color(compArray[0], compArray[1], compArray[2], compArray[3]);
    }

    protected java.awt.Rectangle transformByModelview(DrawContext dc, java.awt.Rectangle rectangle)
    {
        double[] compArray = new double[16];
        dc.getGL().glGetDoublev(GL.GL_MODELVIEW_MATRIX, compArray, 0);
        Matrix modelview = Matrix.fromArray(compArray, 0, false);

        Vec4 origin = new Vec4(rectangle.x, rectangle.y, 1);
        Vec4 size = new Vec4(rectangle.width, rectangle.height, 0);
        origin = origin.transformBy4(modelview);
        size = size.transformBy3(modelview);

        return new java.awt.Rectangle((int) origin.x, (int) origin.y, (int) size.x, (int) size.y);
    }

    protected java.awt.Point glPointFromAWTPoint(DrawContext dc, java.awt.Point awtPoint)
    {
        if (dc.getView() == null || dc.getView().getViewport() == null)
            return null;

        java.awt.Rectangle viewport = dc.getView().getViewport();
        return new java.awt.Point(awtPoint.x, viewport.height - awtPoint.y - 1);
    }

    //**************************************************************//
    //********************  Text Utilities  ************************//
    //**************************************************************//

    protected TextRenderer getTextRenderer(DrawContext dc, java.awt.Font font)
    {
        return OGLTextRenderer.getOrCreateTextRenderer(dc.getTextRendererCache(), font);
    }

    protected MultiLineTextRenderer getMultiLineTextRenderer(DrawContext dc, java.awt.Font font, String align)
    {
        TextRenderer tr = this.getTextRenderer(dc, font);

        MultiLineTextRenderer mltr = new MultiLineTextRenderer(tr);
        // Tighten lines together a bit
//        mltr.setLineSpacing(-2);
        mltr.setTextAlign(align);

        return mltr;
    }

    protected String getWrappedText(DrawContext dc, int width, int height, String text, java.awt.Font font, String align)
    {
        Object key = new TextCacheKey(width, height, text, font, align);
        String wrappedText = this.wrappedTextMap.get(key);
        if (wrappedText == null)
        {
            wrappedText = this.wrapText(dc, width, height, text, font, align);
            this.wrappedTextMap.put(key, wrappedText);
        }

        return wrappedText;
    }

    protected java.awt.Rectangle getTextBounds(DrawContext dc, String text, java.awt.Font font, String align)
    {
        Object key = new TextCacheKey(0, 0, text, font, align);
        java.awt.Rectangle bounds = this.textBoundsMap.get(key);
        if (bounds == null)
        {
            bounds = this.computeTextBounds(dc, text, font, align);
            this.textBoundsMap.put(key, bounds);
        }

        return new java.awt.Rectangle(bounds);
    }

    protected String wrapText(DrawContext dc, int width, int height, String text, java.awt.Font font, String align)
    {
        if (text.length() > 0)
        {
            MultiLineTextRenderer mltr = this.getMultiLineTextRenderer(dc, font, align);

            if (MultiLineTextRenderer.containsHTML(text))
            {
                text = MultiLineTextRenderer.processLineBreaksHTML(text);
                text = mltr.wrapHTML(text, width, height, dc.getTextRendererCache());
            }
            else
            {
                text = mltr.wrap(text, width, height);
            }
        }

        return text;
    }

    protected java.awt.Rectangle computeTextBounds(DrawContext dc, String text, java.awt.Font font, String align)
    {
        if (text.length() > 0)
        {
            MultiLineTextRenderer mltr = this.getMultiLineTextRenderer(dc, font, align);

            if (MultiLineTextRenderer.containsHTML(text))
            {
                return mltr.getBoundsHTML(text, dc.getTextRendererCache());
            }
            else
            {
                return mltr.getBounds(text);
            }
        }
        else
        {
            return new java.awt.Rectangle();
        }
    }

    protected static class TextCacheKey
    {
        private final int width;
        private final int height;
        private final String text;
        private final java.awt.Font font;
        private final String align;

        public TextCacheKey(int width, int height, String text, java.awt.Font font, String align)
        {
            this.width = width;
            this.height = height;
            this.text = text;
            this.font = font;
            this.align = align;
        }

        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || this.getClass() != o.getClass())
                return false;

            TextCacheKey that = (TextCacheKey) o;
            return (this.width == that.width)
                && (this.height == that.height)
                && (this.align.equals(that.align))
                && (this.text != null ? this.text.equals(that.text) : that.text == null)
                && (this.font != null ? this.font.equals(that.font) : that.font == null);
        }

        public int hashCode()
        {
            int result = this.width;
            result = 31 * result + this.height;
            result = 31 * result + (this.text != null ? this.text.hashCode() : 0);
            result = 31 * result + (this.font != null ? this.font.hashCode() : 0);
            result = 31 * result + (this.align != null ? this.align.hashCode() : 0);
            return result;
        }
    }

    //**************************************************************//
    //********************  Bound Computations *********************//
    //**************************************************************//

    protected java.awt.Rectangle computeInsetBounds(int width, int height)
    {
        // TODO: factor in border width?

        java.awt.Insets insets = this.getAttributes().getInsets();
        int insetWidth = width - (insets.left + insets.right);
        int insetHeight = height - (insets.bottom + insets.top);

        if (insetWidth < 0)
            insetWidth = 0;

        if (insetHeight < 0 && height > 0)
            insetHeight = 1;
        else if (insetHeight < 0)
            insetHeight = 0;

        return new java.awt.Rectangle(insets.left, insets.bottom, insetWidth, insetHeight);
    }

    protected java.awt.Rectangle computeFreeBounds(DrawContext dc, int width, int height)
    {
        AnnotationAttributes attribs = this.getAttributes();

        // Start with the inset bounds.
        java.awt.Rectangle bounds = computeInsetBounds(width, height);

        // Adjust the free bounds by the text bounds.
        String wrappedText = this.getWrappedText(dc, width, height, this.getText(), attribs.getFont(),
            attribs.getTextAlign());
        java.awt.Rectangle textBounds = this.getTextBounds(dc, wrappedText, attribs.getFont(),
            attribs.getTextAlign());
        bounds.height -= textBounds.height;

        return bounds;
    }

    protected java.awt.Dimension adjustSizeToText(DrawContext dc, int width, int height)
    {
        AnnotationAttributes attribs = this.getAttributes();

        String text = this.getWrappedText(dc, width, height, this.getText(), attribs.getFont(), attribs.getTextAlign());
        java.awt.Rectangle textBounds = this.getTextBounds(dc, text, attribs.getFont(), attribs.getTextAlign());

        // If the attributes specify to fit the annotation to the wrapped text width, then set the inset width to
        // the wrapped text width.
        if (attribs.getAdjustWidthToText().equals(Annotation.SIZE_FIT_TEXT) && text.length() > 0)
        {
            width = textBounds.width;
        }

        // If the inset height is less than or equal to zero, then override the inset height with the the wrapped
        // text height.
        if (height <= 0)
        {
            height = textBounds.height;
        }

        return new java.awt.Dimension(width, height);
    }

    protected java.awt.Dimension adjustSizeToChildren(DrawContext dc, int width, int height)
    {
        if (this.layoutManager != null)
        {
            java.awt.Dimension preferredSize = this.layoutManager.getPreferredSize(dc, this.childList);
            if (preferredSize != null)
            {
                if (width < preferredSize.width)
                    width = preferredSize.width;
                if (height < preferredSize.height)
                    height = preferredSize.height;
            }
        }

        return new java.awt.Dimension(width, height);
    }

    protected Rectangle computeBoundingRectangle(Rectangle rect, int px, int py)
    {
        if (rect.contains(px, py))
            return rect;

        int dx = 0, dy = 0, dw = 0, dh = 0;
        if (px < rect.x)
        {
            dx = px - rect.x;
            dw = -dx;
        }
        else if (px > rect.x + rect.width - 1)
        {
            dw = px - (rect.x + rect.width - 1);
        }

        if (py < rect.y)
        {
            dy = py - rect.y;
            dh = -dy;
        }
        else if (py > rect.y + rect.height - 1)
        {
            dh = py - (rect.y + rect.height - 1);
        }

        rect.setBounds(rect.x + dx, rect.y + dy, rect.width + dw, rect.height + dh);
        return rect;
    }

    //**************************************************************//
    //********************  Restorable State  **********************//
    //**************************************************************//

    /**
     * Returns an XML state document String describing the public attributes of this AbstractAnnotation.
     *
     * @return XML state document string describing this AbstractAnnotation.
     */
    public String getRestorableState()
    {
        RestorableSupport restorableSupport = null;

        // This should never be the case, but we check to be thorough.
        if (this.attributes != null)
        {
            // Allow AnnotationAttributes to define it's restorable state, if any.
            String attributesStateInXml = this.attributes.getRestorableState();
            if (attributesStateInXml != null)
            {
                try
                {
                    restorableSupport = RestorableSupport.parse(attributesStateInXml);
                }
                catch (Exception e)
                {
                    // Parsing the document specified by the superclass failed.
                    String message =
                            Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", attributesStateInXml);
                    Logging.logger().severe(message);
                }
            }
        }

        // Create our own state document from scratch.
        if (restorableSupport == null)
            restorableSupport = RestorableSupport.newRestorableSupport();
        // Creating a new RestorableSupport failed. RestorableSupport logged the problem, so just return null.
        if (restorableSupport == null)
            return null;

        // Escape the text property when saving it to preserve markup characters.
        if (this.text != null)
            restorableSupport.addStateValueAsString("text", this.text, true);

        restorableSupport.addStateValueAsBoolean("alwaysOnTop", this.alwaysOnTop);

        return restorableSupport.getStateAsXml();
    }

    /**
     * Restores publicly settable attribute values found in the specified XML state document String. The
     * document specified by <code>stateInXml</code> must be a well formed XML document String, or this will throw an
     * IllegalArgumentException. Unknown structures in <code>stateInXml</code> are benign, because they will
     * simply be ignored.
     *
     * @param stateInXml an XML document String describing an AbstractAnnotation.
     * @throws IllegalArgumentException If <code>stateInXml</code> is null, or if <code>stateInXml</code> is not
     *                                  a well formed XML document String.
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

        AnnotationAttributes attribs = this.attributes;
        // Annotation's attributes should not be null. Therefore we assign it a new one as a fallback.
        if (attribs == null)
            attribs = new AnnotationAttributes();
        // Restore any AnnotationAttributes state found in "stateInXml".
        attribs.restoreState(stateInXml);
        setAttributes(attribs);

        // No special processing is required to restore the escaped text property.
        String textState = restorableSupport.getStateValueAsString("text");
        if (textState != null)
            setText(textState);

        Boolean booleanState = restorableSupport.getStateValueAsBoolean("alwaysOnTop");
        if (booleanState != null)
            setAlwaysOnTop(booleanState);
    }
}
