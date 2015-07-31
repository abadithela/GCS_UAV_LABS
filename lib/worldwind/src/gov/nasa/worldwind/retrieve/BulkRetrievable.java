/*
Copyright (C) 2001, 2009 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.retrieve;

import gov.nasa.worldwind.NamedObject;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.geom.Sector;

/**
 * Can be retrieved with a bulk download thread.
 *
 * @author Patrick Murris
 * @version $Id: BulkRetrievable.java 12546 2009-09-03 05:36:57Z tgaskins $
 */
public interface BulkRetrievable extends NamedObject
{
    BulkRetrievalThread makeLocal(Sector sector, double resolution);

    long getEstimatedMissingDataSize(Sector sector, double resolution);

    long getEstimatedMissingDataSize(Sector sector, double resolution, FileStore fileStore);

    BulkRetrievalThread makeLocal(Sector sector, double resolution, FileStore fileStore);
}
