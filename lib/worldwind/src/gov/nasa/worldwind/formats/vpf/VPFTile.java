/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.formats.vpf;

import gov.nasa.worldwind.util.Logging;

/**
 * @author dcollins
 * @version $Id: VPFTile.java 11978 2009-06-29 14:32:28Z dcollins $
 */
public class VPFTile
{
    private int id;
    private String name;
    private VPFBoundingBox bounds;

    public VPFTile(int id, String name, VPFBoundingBox bounds)
    {
        if (name == null)
        {
            String message = Logging.getMessage("nullValue.NameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (bounds == null)
        {
            String message = Logging.getMessage("nullValue.BoundingBoxIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.id = id;
        this.name = name;
        this.bounds = bounds;
    }

    public int getId()
    {
        return this.id;
    }

    public String getName()
    {
        return this.name;
    }

    public VPFBoundingBox getBounds()
    {
        return this.bounds;
    }

    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        VPFTile vpfTile = (VPFTile) o;

        if (id != vpfTile.id)
            return false;
        if (bounds != null ? !bounds.equals(vpfTile.bounds) : vpfTile.bounds != null)
            return false;
        if (name != null ? !name.equals(vpfTile.name) : vpfTile.name != null)
            return false;

        return true;
    }

    public int hashCode()
    {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (bounds != null ? bounds.hashCode() : 0);
        return result;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(this.id);
        sb.append(": ");
        sb.append(this.name);
        return sb.toString();
    }
}
