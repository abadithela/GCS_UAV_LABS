/*
Copyright (C) 2001, 2009 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.retrieve;

/**
 * Stores progress information.
 *
 * @author Patrick Murris
 * @version $Id: Progress.java 9862 2009-04-01 18:01:17Z patrickmurris $
 */
public class Progress
{
    private long startTime;         // from System.currentTimeMillis
    private long lastUpdateTime;    // from System.currentTimeMillis
    private long totalSize;
    private long currentSize;
    private int totalCount;
    private int currentCount;

    public Progress()
    {
        this.startTime = System.currentTimeMillis();
    }

    public long getStartTime()
    {
        return this.startTime;
    }

    public void setStartTime(long time)
    {
        this.startTime = time;
    }

    public long getLastUpdateTime()
    {
        return this.lastUpdateTime;
    }

    public void setLastUpdateTime(long time)
    {
        this.lastUpdateTime = time;
    }

    public long getTotalSize()
    {
        return this.totalSize;
    }

    public void setTotalSize(long size)
    {
        this.totalSize = size;
    }

    public long getCurrentSize()
    {
        return this.currentSize;
    }

    public void setCurrentSize(long size)
    {
        this.currentSize = size;
    }

    public int getTotalCount()
    {
        return this.totalCount;
    }

    public void setTotalCount(int count)
    {
        this.totalCount = count;
    }

    public int getCurrentCount()
    {
        return this.currentCount;
    }

    public void setCurrentCount(int count)
    {
        this.currentCount = count;
    }
}

