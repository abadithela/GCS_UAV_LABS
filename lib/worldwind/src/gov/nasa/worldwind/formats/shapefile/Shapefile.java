/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.formats.shapefile;

import com.sun.opengl.util.BufferUtil;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.util.*;

import java.awt.geom.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/**
 * This class represent an ESRI Shapefile - *.shp.
 * See http://webhelp.esri.com/arcgisdesktop/9.3/index.cfm?id=2729&pid=2727&topicname=Shapefile_file_extensions
 *
 * @author Patrick Murris
 * @version $Id: Shapefile.java 12830 2009-11-26 15:39:49Z patrickmurris $
 */
public class Shapefile
{
    protected static final int FILE_CODE = 0x0000270A;
    protected static final int HEADER_LENGTH = 100;
    protected static final int RECORD_HEADER_LENGTH = 8;

    protected static final String INDEX_FILE_SUFFIX = ".shx";
    protected static final String ATTRIBUTE_FILE_SUFFIX = ".dbf";

    public static final String SHAPE_NULL = "gov.nasa.worldwind.formats.shapefile.Shapefile.ShapeNull";
    public static final String SHAPE_POINT = "gov.nasa.worldwind.formats.shapefile.Shapefile.ShapePoint";
    public static final String SHAPE_POLYLINE = "gov.nasa.worldwind.formats.shapefile.Shapefile.ShapePolyline";
    public static final String SHAPE_POLYGON = "gov.nasa.worldwind.formats.shapefile.Shapefile.ShapePolygon";

    protected class Header
    {
        public int fileCode = FILE_CODE;
        public int fileLength;
        public int version;
        public String shapeType;
        public Rectangle2D boundingRectangle;
    }

    private final File file;
    protected final Header header;
    protected int[] index;
    protected CompoundVecBuffer buffer;
    protected ArrayList<ShapefileRecord> records;

    /**
     * Creates a Shapefile instance from the given file refering to the shapefile main geometry
     * file with a .shp extension. Records are not loaded right away. The file(s) content will be read during
     * the first call to {@link #getRecords()}.
     *
     * @param file the shapefile geometry file *.shp.
     *
     * @throws  WWRuntimeException if the shapefile cannot be instantiated for any reason.
     */
    public Shapefile(File file)
    {
        if (file == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.file = file;
        try
        {
            this.header = readHeaderFromFile(file);
            // Delay records loading until getRecords() is called
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("SHP.ExceptionAttemptingToReadFile", file.getPath());
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            throw new WWRuntimeException(message, e);
        }
    }

    /**
     * Creates a Shapefile instance from the given input streams. Records are loaded right away.
     *
     * @param shpStream the shapefile geometry file stream - *.shp.
     * @param shxStream the index file stream - .shx, can be null.
     * @param dbfStream the attribute file stream - *.dbf, can be null.
     *
     * @throws  WWRuntimeException if the shapefile cannot be instantiated for any reason.
     */
    public Shapefile(InputStream shpStream, InputStream shxStream, InputStream dbfStream)
    {
        if (shpStream == null)
        {
            String message = Logging.getMessage("nullValue.InputStreamIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.file = null;
        try
        {
            this.header = readHeaderFromStream(shpStream);
            this.records = readRecordsFromStream(shpStream, shxStream, dbfStream);
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("generic.ExceptionAttemptingToReadFrom", shpStream.toString());
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            throw new WWRuntimeException(message, e);
        }
    }

    public File getFile()
    {
        return this.file;
    }

    public int getVersion()
    {
        return this.header.version;
    }

    public int getLength()
    {
        return this.header.fileLength;
    }

    public String getShapeType()
    {
        return this.header.shapeType;
    }

    public Rectangle2D getBoundingRectangle()
    {
        return this.header.boundingRectangle;
    }

    /**
     * Returns a {@link List} containing the shapefile records. If the Shapefile was instantiated from a File, the
     * shapefile content - it's records and attributes, will be read during the first call to this method.
     *
     * @return a List containing all the shapefile records.
     */
    public List<ShapefileRecord> getRecords()
    {
        if (this.records == null && this.getFile() != null)
        {
            File file = this.getFile();
            try
            {
                this.records = this.readRecordsFromFile(file);
            }
            catch (Exception e)
            {
                String message = Logging.getMessage("SHP.ExceptionAttemptingToReadFile", file.getPath());
                Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
                throw new WWRuntimeException(message, e);
            }
        }

        return this.records;
    }

    /**
     * Get the underlying {@link CompoundVecBuffer} describing the geometry.
     *
     * @return the underlying {@link CompoundVecBuffer}.
     */
    public CompoundVecBuffer getBuffer()
    {
        return this.buffer;
    }

    /**
     * Returns <code>true</code> if this shapefile has an associated index file - .shx.
     *
     * @return Returns <code>true</code> if this shapefile has an associated index file - .shx.
     */
    protected boolean hasIndexFile()
    {
        File idxFile = this.getIndexFile();
        return idxFile != null && idxFile.exists();
    }

    /**
     * Returns <code>true</code> if this shapefile has an associated attribute file - .dbf.
     *
     * @return <code>true</code> if this shapefile has an associated attribute file - .dbf.
     */
    protected boolean hasAttributeFile()
    {
        File attrFile = this.getAttributeFile();
        return attrFile != null && attrFile.exists();
    }

    protected int getNumberOfRecords()
    {
        int[] index = this.getIndex();
        return index != null ? index.length / 2 : -1;
    }

    protected int[] getIndex()
    {
        if (this.index == null && this.hasIndexFile())
        {
            File indexFile = this.getIndexFile();
            try
            {
                this.index = this.readIndexFromFile(indexFile);
            }
            catch (Exception e)
            {
                String message = Logging.getMessage("SHP.ExceptionAttemptingToReadFile", indexFile.getPath());
                Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
                throw new WWRuntimeException(message, e);
            }
        }

        return this.index;
    }

    protected int getRecordOffset(int n)
    {
        int[] index = this.getIndex();
        if (index == null || n > index.length / 2)
            return -1;

        return index[(n - 1) * 2];
    }

    protected File getIndexFile()
    {
        if (this.getFile() == null)
            return null;

        String filePath = WWIO.replaceSuffix(this.file.getAbsolutePath(), INDEX_FILE_SUFFIX);
        return new File(filePath);
    }

    protected File getAttributeFile()
    {
        if (this.getFile() == null)
            return null;

        String filePath = WWIO.replaceSuffix(this.file.getAbsolutePath(), ATTRIBUTE_FILE_SUFFIX);
        return new File(filePath);
    }

    //=== Shapefile header ===

    protected Header readHeaderFromFile(File file) throws IOException
    {
        InputStream is = null;
        Header header = null;
        try
        {
            is = new BufferedInputStream(new FileInputStream(file));
            header = this.readHeaderFromStream(is);
        }
        finally
        {
            if (is != null)
                is.close();
        }

        return header;
    }

    protected Header readHeaderFromStream(InputStream stream) throws IOException
    {
        ReadableByteChannel channel = Channels.newChannel(stream);
        ByteBuffer headerBuffer = ShapefileUtils.readByteChannelToBuffer(channel, HEADER_LENGTH);
        return this.readHeaderFromBuffer(headerBuffer);
    }

    protected Header readHeaderFromBuffer(ByteBuffer buffer) throws WWRuntimeException
    {
        // Read file code - first 4 bytes, big endian
        buffer.order(ByteOrder.BIG_ENDIAN);
        int fileCode = buffer.getInt();
        if (fileCode != FILE_CODE)
        {
            String message = Logging.getMessage("SHP.NotAShapeFile", file.getPath());
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new WWRuntimeException(message);
        }

        // Skip 5 unused ints
        buffer.position(buffer.position() + 5 * 4);

        // File length
        int lengthInWords = buffer.getInt();

        // Switch to little endian for the remaining part
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Read remaining header data
        int version = buffer.getInt();
        int type = buffer.getInt();
        double[] bounds = ShapefileUtils.readDoubleArray(buffer, 4);
        Rectangle2D rect = new Rectangle2D.Double(bounds[0], bounds[1], bounds[2] - bounds[0], bounds[3] - bounds[1]);

        // Check whether the shape type is supported
        String shapeType = getShapeType(type);
        if (shapeType == null)
        {
            String message = Logging.getMessage("SHP.UnsupportedShapeType", type);
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new WWRuntimeException(message);
        }

        // Assemble header
        Header header = new Header();
        header.fileLength = lengthInWords * 2; // one word = 2 bytes
        header.version = version;
        header.shapeType = shapeType;
        header.boundingRectangle = rect;

        return header;
    }

    // === Shapefile index ===

    protected int[] readIndexFromFile(File file) throws IOException
    {
        InputStream is = null;
        int[] index = null;
        try
        {
            is = new BufferedInputStream(new FileInputStream(file));
            index = this.readIndexFromStream(is);
        }
        finally
        {
            if (is != null)
                is.close();
        }

        return index;
    }

    protected int[] readIndexFromStream(InputStream stream) throws IOException
    {
        ByteBuffer indexBuffer = WWIO.readStreamToBuffer(stream);
        return this.readIndexFromBuffer(indexBuffer);
    }

    protected int[] readIndexFromBuffer(ByteBuffer buffer)
    {
        Header indexHeader = this.readHeaderFromBuffer(buffer);
        int numRecords = (indexHeader.fileLength - HEADER_LENGTH) / 8;
        int[] index = new int[numRecords * 2];

        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.position(HEADER_LENGTH); // Skip file header
        int idx = 0;
        for (int i = 0; i < numRecords; i++)
        {
            index[idx++] = buffer.getInt() * 2;  // record offset in bytes
            index[idx++] = buffer.getInt() * 2;  // record length in bytes
        }

        return index;
    }

    // === Shapefile records ===

    protected ArrayList<ShapefileRecord> readRecordsFromFile(File file) throws IOException
    {
        InputStream shpStream = null;
        InputStream shxStream = null;
        InputStream dbfStream = null;
        ArrayList<ShapefileRecord> records = null;
        try
        {
            shpStream = new BufferedInputStream(new FileInputStream(file));
            ShapefileUtils.skipBytes(shpStream, HEADER_LENGTH);
            if (this.hasIndexFile())
                shxStream = new BufferedInputStream(new FileInputStream(this.getIndexFile()));
            if (this.hasAttributeFile())
                dbfStream = new BufferedInputStream(new FileInputStream(this.getAttributeFile()));

            records = this.readRecordsFromStream(shpStream, shxStream, dbfStream);
        }
        finally
        {
            if (shpStream != null)
                shpStream.close();
            if (shxStream != null)
                shxStream.close();
            if (dbfStream != null)
                dbfStream.close();
        }

        return records;
    }

    protected ArrayList<ShapefileRecord> readRecordsFromStream(InputStream is, InputStream shxStream,
        InputStream dbfStream) throws IOException
    {
        ArrayList<ShapefileRecord> recordList = new ArrayList<ShapefileRecord>();
        DBaseFile attrFile = dbfStream != null ? new DBaseFile(dbfStream) : null;
        this.index = shxStream != null ? this.readIndexFromStream(shxStream) : null;
        ByteBuffer headerBuffer = ByteBuffer.allocate(RECORD_HEADER_LENGTH);
        ByteBuffer recordBuffer = null;

        // Allocate point buffer
        int numPoints = this.computeNumberOfPointsEstimate();
        VecBuffer pointBuffer = new VecBuffer(2, numPoints, new BufferFactory.DoubleBufferFactory());
        List<Integer> partsOffset = new ArrayList<Integer>();
        List<Integer> partsLength = new ArrayList<Integer>();

        // Get channel
        ReadableByteChannel channel = Channels.newChannel(is);

        // Read all records
        int bytesRead = 0;
        while (bytesRead < this.header.fileLength - HEADER_LENGTH)
        {
            // Read record header and get data length
            headerBuffer.clear();
            headerBuffer = ShapefileUtils.readByteChannelToBuffer(channel, RECORD_HEADER_LENGTH, headerBuffer);
            headerBuffer.order(ByteOrder.BIG_ENDIAN);
            int recLength = headerBuffer.getInt(4) * 2; // skip record number

            // Read record data
            int numBytes = RECORD_HEADER_LENGTH + recLength; // record header + data
            if (recordBuffer == null || recordBuffer.capacity() < numBytes)
                recordBuffer = ByteBuffer.allocate(numBytes);
            recordBuffer.limit(numBytes).rewind();
            recordBuffer.put(headerBuffer);
            recordBuffer = ShapefileUtils.readByteChannelToBuffer(channel, recLength, recordBuffer);

            // Create record
            ShapefileRecord record = ShapefileRecord.fromBuffer(this, recordBuffer, pointBuffer, partsOffset, partsLength);

            // Set record attributes from dbf file
            record = this.setRecordAttributes(record, attrFile);

            // Add record to list if accepted
            if (this.acceptRecord(record))
                recordList.add(record);

            bytesRead += numBytes;
        }

        // Create CompoundVecBuffer
        int numParts = partsOffset.size();
        IntBuffer offsetBuffer = BufferUtil.newIntBuffer(numParts);
        IntBuffer lengthBuffer = BufferUtil.newIntBuffer(numParts);
        for (int i = 0; i < partsOffset.size(); i++)
        {
                offsetBuffer.put(partsOffset.get(i));
                lengthBuffer.put(partsLength.get(i));
        }
        offsetBuffer.rewind();
        lengthBuffer.rewind();

        this.buffer = new CompoundVecBuffer(pointBuffer, offsetBuffer, lengthBuffer, numParts,
            new BufferFactory.DoubleBufferFactory());

        if (recordList.size() == 0)
            return null;

        if (this.header.boundingRectangle.getX() < -180 || this.header.boundingRectangle.getMaxX() > 180)
            this.normalizeLocations(recordList);

        return recordList;
    }

    /**
     * Returns <code>true</code> if the given record is to be included in the record list. This method
     * should be overrided by subclasses that wish to enable record selection at load time rather than
     * after all records have been read.
     *
     * @param record the {@link ShapefileRecord} to accept.
     * @return true if the record should be included in the record list.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected boolean acceptRecord(ShapefileRecord record)
    {
        return true;
    }

    protected void normalizeLocations(List<ShapefileRecord> recordList)
    {
        for (ShapefileRecord record : recordList)
            record.normalizeLocations();
        ShapefileUtils.normalizeRectangle(this.header.boundingRectangle);
    }

    // === Record attributes ===

    protected ShapefileRecord setRecordAttributes(ShapefileRecord record, DBaseFile attrFile)
    {
        if (attrFile == null || attrFile.getRecords() == null)
            return record;

        if (record.getRecordNumber() > attrFile.getRecords().size())
        {
            String message = Logging.getMessage("generic.indexOutOfRange", record.getRecordNumber());
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new WWRuntimeException(message);
        }

        record.attributes = attrFile.getRecords().get(record.getRecordNumber() - 1);
        return record;
    }

    protected static String getShapeType(int type)
    {
        switch (type)
        {
            case 0 :
                return SHAPE_NULL;
            case 1 :
                return SHAPE_POINT;
            case 3 :
                return SHAPE_POLYLINE;
            case 5 :
                return SHAPE_POLYGON;
            default :
                return null; // unsupported shape type
        }
    }

    protected int computeNumberOfPointsEstimate()
    {
        int POINT_SIZE = 16; // two doubles
        int maxPoints = (this.getLength() - HEADER_LENGTH) / POINT_SIZE; // num of x, y tuples that can fit in the file length
        int numRecords = this.getNumberOfRecords();

        // Return estimate based on file size if num records unknown
        if (numRecords < 0)
            return maxPoints;

        // Subtract 12 bytes per record for record header and record shape type
        maxPoints -= (numRecords * 12) / POINT_SIZE;

        // Subtract 40 bytes per record for bounding box and two ints if polyline or polygon
        if (this.getShapeType().equals(SHAPE_POLYLINE) || this.getShapeType().equals(SHAPE_POLYGON))
            maxPoints -= (numRecords * 40) / POINT_SIZE;

        return maxPoints;
    }


    // Tests
//    public static void main(String[] args)
//    {
//        File file = new File("J:\\Data\\Shapefiles\\World\\TM_WORLD_BORDERS_SIMPL-0.2.shp");
//        try
//        {
//            Shapefile shp = new Shapefile(file);
//            //Shapefile shp = new Shapefile(new BufferedInputStream(new FileInputStream(file)), null, null);
//
//            // Dump shapefile properties
//            System.out.println("Shapefile : " + (shp.getFile() != null ? shp.getFile().getName() : "null"));
//            System.out.println("has index : " + shp.hasIndexFile());
//            System.out.println("has attributes : " + shp.hasAttributeFile());
//            System.out.println("length : " + shp.getLength());
//            System.out.println("type : " + shp.getShapeType());
//            System.out.println("version : " + shp.getVersion());
//            System.out.println("sector : " + shp.getBoundingRectangle());
//
//            // Read index
//            int[] index = shp.getIndex();
//            if (index != null)
//            {
//                System.out.println("Index: " + index.length / 2 + " entries");
//                for (int i = 0; i < index.length / 2 && i < 10; i++)
//                    System.out.println("Rec " + (i + 1) + ", offset: " + index[i * 2] + ", length: " + index[i * 2 + 1]);
//            }
//
//            // Read records
//            List<ShapefileRecord> records = shp.getRecords();
//            System.out.println("records : " + records.size());
//
//            int totShapes = 0;
//            for (int i = 0; i < records.size() && i < 50; i++)
//            {
//                ShapefileRecord rec = records.get(i);
//
//                // Dump record properties
//                if (rec instanceof ShapefileRecordPoint)
//                {
//                    ShapefileRecordPoint point = (ShapefileRecordPoint)rec;
//                    System.out.println("record : " + point.getRecordNumber()
//                        + ", coord: " + LatLon.fromDegrees(point.getPoint()[1], point.getPoint()[0]));
//                    totShapes++;
//                }
//                if (rec instanceof ShapefileRecordPolyline)
//                {
//                    ShapefileRecordPolyline polyline = (ShapefileRecordPolyline)rec;
//                    System.out.println("record : " + polyline.getRecordNumber()
//                        + ", parts: " + polyline.getNumberOfParts()
//                        + ", points: " + polyline.getNumberOfPoints()
//                        + ", rect: " + polyline.getBoundingRectangle());
//                    totShapes += polyline.getNumberOfParts();
//                }
//
//                // Dump record attributes
//                if (rec.getAttributes() != null)
//                {
//                    System.out.print("Attributes : ");
//                    for (Map.Entry entry : rec.getAttributes().getEntries())
//                        System.out.print(entry.getKey() + " = " + entry.getValue() + ", ");
//                    System.out.println("");
//                }
//            }
//
//            System.out.println("Tot shapes : " + totShapes);
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//
//    }

}