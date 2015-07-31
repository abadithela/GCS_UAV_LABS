/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import com.sun.opengl.util.texture.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.data.ImageIOReader;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.GL;
import java.awt.image.*;

/**
 * TODO: this may be a general use class. Simplify it as a subclass of BasicWWTexture, then look for a general use pattern.
 *
 * @author dcollins
 * @version $Id: AnnotationTexture.java 10542 2009-04-27 17:28:30Z dcollins $
 */
public class AnnotationTexture implements Cacheable
{
    private Object source;
    private int[] sourceDimensions;

    public AnnotationTexture(Object source)
    {
        if (source == null)
        {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        this.source = source;
    }

    public Object getSource()
    {
        return this.source;
    }

    public int[] getSourceDimensions()
    {
        if (this.sourceDimensions == null)
        {
            this.sourceDimensions = this.getImageDimensionsFromSource();
        }

        return this.sourceDimensions;
    }

    public Texture getTexture(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        Texture t = this.getTexture(dc.getTextureCache());
        if (t == null)
        {
            t = this.initializeTexture(dc);
        }

        return t;
    }

    public boolean bind(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        Texture t = this.getTexture(dc.getTextureCache());
        if (t == null)
        {
            t = this.initializeTexture(dc);
            if (t != null)
            {
                return true; // texture was bound during initialization.
            }
        }

        if (t != null)
        {
            t.bind();
        }

        return t != null;
    }

    public void applyTransform(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        Texture t = this.getTexture(dc.getTextureCache());
        if (t == null)
        {
            t = this.initializeTexture(dc);
        }

        int[] sourceDimensions = this.getSourceDimensions();

        GL gl = dc.getGL();

        // If the original image was not a power of two, then it will have been stretched in each dimension to a
        // power of two. We scale the texture here to obtain the original aspect ratio and size on screen.
        gl.glScaled(t.getWidth() / (double) sourceDimensions[0], t.getHeight() / (double) sourceDimensions[1], 1);

        // If the texture's origin is in the upper left hand corner, we scale and translate it so the origin is
        // in the lower left hand corner.
        if (!t.getMustFlipVertically())
        {
            gl.glScaled(1, -1, 1);
            gl.glTranslated(0, -1, 0);
        }

        gl.glScaled(1 / (double) t.getWidth(), 1 / (double) t.getHeight(), 1);
    }

    public long getSizeInBytes()
    {
        return this.getSizeOfSource();
    }

    protected Texture getTexture(TextureCache cache)
    {
        return cache.get(this.source);
    }

    protected void setTexture(TextureCache cache, Texture t)
    {
        cache.put(this.source, t);
    }

    protected Texture initializeTexture(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        Texture t = this.getTexture(dc.getTextureCache());
        if (t != null)
            return t;

        BufferedImage image = this.getImageFromSource();
        if (image == null)
            return null;

        // Save the original image dimensions.
        this.sourceDimensions = new int[] {image.getWidth(), image.getHeight()};

        // Create a copy of the original image with power of two dimensions. Because an Annotation's image may be
        // repeatable, we must scale the image to the new dimensions so the new image contains no empty space.
        // When the image is used for rendering, we must then compensate for this scaling, and return the image
        // to its original aspect ratio and screen size.
        BufferedImage potImage = ImageUtil.convertToPowerOfTwoImage(image, true);

        try
        {
            t = TextureIO.newTexture(potImage, true);
        }
        catch (Exception e)
        {
            Logging.logger().log(
                java.util.logging.Level.SEVERE, "layers.TextureLayer.ExceptionAttemptingToReadTextureFile", e);
            return null;
        }

        this.setTexture(dc.getTextureCache(), t);
        t.bind();

        GL gl = dc.getGL();
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);

        return t;
    }

    protected BufferedImage getImageFromSource()
    {
        try
        {
            BufferedImage image;
            if (this.source instanceof BufferedImage)
            {
                image = (BufferedImage) this.source;
            }
            else
            {
                ImageIOReader reader = new ImageIOReader();
                image = reader.read(this.source);
            }

            return image;
        }
        catch (java.io.IOException e)
        {
            String msg = Logging.getMessage("generic.IOExceptionDuringTextureInitialization");
            Logging.logger().log(java.util.logging.Level.SEVERE, msg, e);

            return null;
        }
    }

    protected int[] getImageDimensionsFromSource()
    {
        try
        {
            int[] dimensions;
            if (this.source instanceof BufferedImage)
            {
                BufferedImage image = (BufferedImage) this.source;
                dimensions = new int[] {image.getWidth(), image.getHeight()};
            }
            else
            {
                AVList values = new AVListImpl();
                ImageIOReader reader = new ImageIOReader();
                reader.readMetadata(this.source, values);

                Integer width = AVListImpl.getIntegerValue(values, AVKey.WIDTH);
                Integer height = AVListImpl.getIntegerValue(values, AVKey.HEIGHT);
                dimensions = new int[] {width, height};
            }

            return dimensions;
        }
        catch (java.io.IOException e)
        {
            String msg = Logging.getMessage("generic.IOExceptionDuringTextureInitialization");
            Logging.logger().log(java.util.logging.Level.SEVERE, msg, e);

            return null;
        }
    }

    protected long getSizeOfSource()
    {
        if (this.source instanceof BufferedImage)
        {
            return ImageUtil.computeSizeInBytes((BufferedImage) this.source);
        }
        else
        {
            // The source is a reference to an image. This may be a File, a String path, or a URL. In any case,
            // we treat each reference as having equivalent size for the purposes of caching.
            return 1;
        }
    }
}
