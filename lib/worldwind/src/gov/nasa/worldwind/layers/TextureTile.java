/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers;

import com.sun.opengl.util.texture.*;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.*;
import java.util.*;

/**
 * This class manages the conversion and timing of image data to a JOGL Texture, and provides an interface for binding
 * the texture and applying any texture transforms to align the texture and texture coordinates.
 * <p/>
 *
 * @author tag
 * @version $Id: TextureTile.java 12641 2009-09-24 08:45:41Z dcollins $
 */
public class TextureTile extends Tile implements SurfaceTile
{
    private volatile TextureData textureData; // if non-null, then must be converted to a Texture
    private TextureTile fallbackTile = null; // holds texture to use if own texture not available
    private Vec4 centroid; // Cartesian coordinate of lat/lon center
    private Extent extent = null; // bounding volume
    private Object globeStateKey;
    private boolean hasMipmapData = false;
    private long updateTime = 0;

    /**
     * Returns the memory cache used to cache tiles for this class and its subclasses, initializing the cache if it
     * doesn't yet exist.
     *
     * @return the memory cache associated with the tile.
     */
    public static synchronized MemoryCache getMemoryCache()
    {
        if (!WorldWind.getMemoryCacheSet().containsCache(TextureTile.class.getName()))
        {
            long size = Configuration.getLongValue(AVKey.TEXTURE_IMAGE_CACHE_SIZE, 3000000L);
            MemoryCache cache = new BasicMemoryCache((long) (0.85 * size), size);
            cache.setName("Texture Tiles");
            WorldWind.getMemoryCacheSet().addCache(TextureTile.class.getName(), cache);
        }

        return WorldWind.getMemoryCacheSet().getCache(TextureTile.class.getName());
    }

    public TextureTile(Sector sector)
    {
        super(sector);
    }

    public TextureTile(Sector sector, Level level, int row, int col)
    {
        super(sector, level, row, col);
    }

    @Override
    public final long getSizeInBytes()
    {
        long size = super.getSizeInBytes();

        if (this.textureData != null)
            size += this.textureData.getEstimatedMemorySize();

        return size;
    }

    public List<? extends LatLon> getCorners()
    {
        ArrayList<LatLon> list = new ArrayList<LatLon>(4);
        for (LatLon ll : this.getSector())
        {
            list.add(ll);
        }

        return list;
    }

    public TextureTile getFallbackTile()
    {
        return this.fallbackTile;
    }

    public void setFallbackTile(TextureTile fallbackTile)
    {
        this.fallbackTile = fallbackTile;
    }

    /**
     * Returns the texture data most recently specified for the tile. New texture data is typically specified when a new
     * image is read, either initially or in response to image expiration.
     * <p/>
     * If texture data is non-null, a new texture is created from the texture data when the tile is next bound or
     * otherwise initialized. The texture data field is then set to null. Subsequently setting texture data to be
     * non-null causes a new texture to be created when the tile is next bound or initialized.
     *
     * @return the texture data, which may be null.
     */
    public TextureData getTextureData()
    {
        return this.textureData;
    }

    /**
     * Specifies new texture data for the tile. New texture data is typically specified when a new image is read, either
     * initially or in response to image expiration.
     * <p/>
     * If texture data is non-null, a new texture is created from the texture data when the tile is next bound or
     * otherwise initialized. The texture data field is then set to null. Subsequently setting texture data to be
     * non-null causes a new texture to be created when the tile is next bound or initialized.
     * <p/>
     * When a texture is created from the texture data, the texture data field is set to null to indicate that the data
     * has been converted to a texture and its resources may be released.
     *
     * @param textureData the texture data, which may be null.
     */
    public void setTextureData(TextureData textureData)
    {
        this.textureData = textureData;
        if (textureData.getMipmapData() != null)
            this.hasMipmapData = true;
    }

    public Texture getTexture(TextureCache tc)
    {
        if (tc == null)
        {
            String message = Logging.getMessage("nullValue.TextureCacheIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        return tc.get(this.getTileKey());
    }

    public boolean isTextureInMemory(TextureCache tc)
    {
        if (tc == null)
        {
            String message = Logging.getMessage("nullValue.TextureCacheIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        return this.getTexture(tc) != null || this.getTextureData() != null;
    }

    public boolean isTextureExpired()
    {
        return this.isTextureExpired(this.getLevel().getExpiryTime());
    }

    public boolean isTextureExpired(long expiryTime)
    {
        return this.updateTime > 0 && this.updateTime < expiryTime;
    }

    public void setTexture(TextureCache tc, Texture texture)
    {
        if (tc == null)
        {
            String message = Logging.getMessage("nullValue.TextureCacheIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        tc.put(this.getTileKey(), texture);
        this.updateTime = System.currentTimeMillis();

        // No more need for texture data; allow garbage collector and memory cache to reclaim it.
        // This also signals that new texture data has been converted.
        this.textureData = null;
        this.updateMemoryCache();
    }

    public Vec4 getCentroidPoint(Globe globe)
    {
        if (globe == null)
        {
            String msg = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (this.centroid == null)
        {
            LatLon c = this.getSector().getCentroid();
            this.centroid = globe.computePointFromPosition(c.getLatitude(), c.getLongitude(), 0);
        }

        return this.centroid;
    }

    public Extent getExtent(DrawContext dc)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (this.extent == null || !this.isExtentValid(dc))
        {
            this.extent = dc.getGlobe().computeBoundingCylinder(dc.getVerticalExaggeration(), this.getSector());
            this.globeStateKey = dc.getGlobe().getStateKey(dc);
            this.centroid = null;
        }

        return this.extent;
    }

    private boolean isExtentValid(DrawContext dc)
    {
        return !(dc.getGlobe() == null || this.globeStateKey == null)
            && this.globeStateKey.equals(dc.getGlobe().getStateKey(dc));
    }

    public TextureTile[] createSubTiles(Level nextLevel)
    {
        if (nextLevel == null)
        {
            String msg = Logging.getMessage("nullValue.LevelIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        Angle p0 = this.getSector().getMinLatitude();
        Angle p2 = this.getSector().getMaxLatitude();
        Angle p1 = Angle.midAngle(p0, p2);

        Angle t0 = this.getSector().getMinLongitude();
        Angle t2 = this.getSector().getMaxLongitude();
        Angle t1 = Angle.midAngle(t0, t2);

        String nextLevelCacheName = nextLevel.getCacheName();
        int nextLevelNum = nextLevel.getLevelNumber();
        int row = this.getRow();
        int col = this.getColumn();

        TextureTile[] subTiles = new TextureTile[4];

        TileKey key = new TileKey(nextLevelNum, 2 * row, 2 * col, nextLevelCacheName);
        TextureTile subTile = this.getTileFromMemoryCache(key);
        if (subTile != null)
            subTiles[0] = subTile;
        else
            subTiles[0] = new TextureTile(new Sector(p0, p1, t0, t1), nextLevel, 2 * row, 2 * col);

        key = new TileKey(nextLevelNum, 2 * row, 2 * col + 1, nextLevelCacheName);
        subTile = this.getTileFromMemoryCache(key);
        if (subTile != null)
            subTiles[1] = subTile;
        else
            subTiles[1] = new TextureTile(new Sector(p0, p1, t1, t2), nextLevel, 2 * row, 2 * col + 1);

        key = new TileKey(nextLevelNum, 2 * row + 1, 2 * col, nextLevelCacheName);
        subTile = this.getTileFromMemoryCache(key);
        if (subTile != null)
            subTiles[2] = subTile;
        else
            subTiles[2] = new TextureTile(new Sector(p1, p2, t0, t1), nextLevel, 2 * row + 1, 2 * col);

        key = new TileKey(nextLevelNum, 2 * row + 1, 2 * col + 1, nextLevelCacheName);
        subTile = this.getTileFromMemoryCache(key);
        if (subTile != null)
            subTiles[3] = subTile;
        else
            subTiles[3] = new TextureTile(new Sector(p1, p2, t1, t2), nextLevel, 2 * row + 1, 2 * col + 1);

        return subTiles;
    }

    private TextureTile getTileFromMemoryCache(TileKey tileKey)
    {
        return (TextureTile) getMemoryCache().getObject(tileKey);
    }

    private void updateMemoryCache()
    {
        if (this.getTileFromMemoryCache(this.getTileKey()) != null)
            getMemoryCache().add(this.getTileKey(), this);
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
        // Return texture if found and there is no new texture data
        if (t != null && this.getTextureData() == null)
            return t;

        if (this.getTextureData() == null) // texture not in cache yet texture data is null, can't initialize
        {
            String msg = Logging.getMessage("nullValue.TextureDataIsNull");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        try
        {
            t = TextureIO.newTexture(this.getTextureData());
        }
        catch (Exception e)
        {
            Logging.logger().log(
                java.util.logging.Level.SEVERE, "layers.TextureLayer.ExceptionAttemptingToReadTextureFile", e);
            return null;
        }

        this.setTexture(dc.getTextureCache(), t);
        t.bind();

        this.setTextureParameters(dc, t);

        return t;
    }

    protected void setTextureParameters(DrawContext dc, Texture t)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        GL gl = dc.getGL();

        // Use a mipmap minification filter when either of the following is true:
        // a. The texture has mipmap data. This is typically true for formats with embedded mipmaps, such as DDS.
        // b. The texture is setup to have GL automatically generate mipmaps. This is typically true when a texture is
        //    loaded from a standard image type, such as PNG or JPEG, and the caller instructed JOGL to generate
        //     mipmaps.
        // Additionally, the texture must be in the latitude range (-80, 80). We do this to prevent seams that appear
        // between textures near the poles.
        //
        // TODO: remove the latitude range restriction if a better tessellator fixes the problem.

        boolean useMipmapFilter = (this.hasMipmapData || t.isUsingAutoMipmapGeneration())
            && this.getSector().getMaxLatitude().degrees < 80d && this.getSector().getMinLatitude().degrees > -80;

        // Set the texture minification filter. If the texture qualifies for mipmaps, apply a minification filter that
        // will access the mipmap data using the highest quality algorithm. If the anisotropic texture filter is
        // available, we will enable it. This will sharpen the appearance of the mipmap filter when the textured
        // surface is at a high slope to the eye.
        if (useMipmapFilter)
        {
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);

            // If the maximum degree of anisotropy is 2.0 or greater, then we know this graphics context supports
            // the anisotropic texture filter.
            double maxAnisotropy = dc.getGLRuntimeCapabilities().getMaxTextureAnisotropy();
            if (dc.getGLRuntimeCapabilities().isUseAnisotropicTextureFilter() && maxAnisotropy >= 2.0)
            {
                gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT, (float) maxAnisotropy);
            }
        }
        // If the texture does not qualify for mipmaps, then apply a linear minification filter.
        else
        {
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        }

        // Set the texture magnification filter to a linear filter. This will blur the texture as the eye gets very
        // near, but this is still a better choice than nearest neighbor filtering.
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);

        // Set the S and T wrapping modes to clamp to the texture edge. This way no border pixels will be sampled by
        // either the minification or magnification filters.
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
    }

    public boolean bind(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        // Reinitialize texture if new texture data
        if (this.getTextureData() != null)
        {
            Texture t = this.initializeTexture(dc);
            if (t != null)
                return true; // texture was bound during initialization.
        }

        Texture t = this.getTexture(dc.getTextureCache());

        if (t == null && this.getFallbackTile() != null)
        {
            TextureTile resourceTile = this.getFallbackTile();
            t = resourceTile.getTexture(dc.getTextureCache());
            if (t == null)
            {
                t = resourceTile.initializeTexture(dc);
                if (t != null)
                    return true; // texture was bound during initialization.
            }
        }

        if (t != null)
            t.bind();

        return t != null;
    }

    public void applyInternalTransform(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        Texture t;
        if (this.getTextureData() != null) // Reinitialize if new texture data
            t = this.initializeTexture(dc);
        else
            t = this.getTexture(dc.getTextureCache()); // Use the tile's texture if available

        if (t != null)
        {
            if (t.getMustFlipVertically())
            {
                GL gl = GLContext.getCurrent().getGL();
                gl.glMatrixMode(GL.GL_TEXTURE);
                gl.glLoadIdentity();
                gl.glScaled(1, -1, 1);
                gl.glTranslated(0, -1, 0);
            }
            return;
        }

        // Use the tile's fallback texture if its primary texture is not available.
        TextureTile resourceTile = this.getFallbackTile();
        if (resourceTile == null) // no fallback specified
            return;

        t = resourceTile.getTexture(dc.getTextureCache());
        if (t == null && resourceTile.getTextureData() != null)
            t = resourceTile.initializeTexture(dc);

        if (t == null) // was not able to initialize the fallback texture
            return;

        // Apply necessary transforms to the fallback texture.
        GL gl = GLContext.getCurrent().getGL();
        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glLoadIdentity();

        if (t.getMustFlipVertically())
        {
            gl.glScaled(1, -1, 1);
            gl.glTranslated(0, -1, 0);
        }

        this.applyResourceTextureTransform(dc);
    }

    private void applyResourceTextureTransform(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (this.getLevel() == null)
            return;

        int levelDelta = this.getLevelNumber() - this.getFallbackTile().getLevelNumber();
        if (levelDelta <= 0)
            return;

        double twoToTheN = Math.pow(2, levelDelta);
        double oneOverTwoToTheN = 1 / twoToTheN;

        double sShift = oneOverTwoToTheN * (this.getColumn() % twoToTheN);
        double tShift = oneOverTwoToTheN * (this.getRow() % twoToTheN);

        dc.getGL().glTranslated(sShift, tShift, 0);
        dc.getGL().glScaled(oneOverTwoToTheN, oneOverTwoToTheN, 1);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final TextureTile tile = (TextureTile) o;

        return !(this.getTileKey() != null ? !this.getTileKey().equals(tile.getTileKey()) : tile.getTileKey() != null);
    }

    @Override
    public int hashCode()
    {
        return (this.getTileKey() != null ? this.getTileKey().hashCode() : 0);
    }

    @Override
    public String toString()
    {
        return this.getSector().toString();
    }
}
