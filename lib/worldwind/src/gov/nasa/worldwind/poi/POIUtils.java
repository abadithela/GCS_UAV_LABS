/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.poi;

import gov.nasa.worldwind.exception.*;
import gov.nasa.worldwind.util.*;

import java.io.*;
import java.net.*;
import java.nio.*;

/**
 * Utilites for working with points of interest and gazetteers.
 *
 * @author tag
 * @version $Id: POIUtils.java 9306 2009-03-11 20:05:32Z tgaskins $
 */
public class POIUtils
{
    /**
     * Invoke a point-of-interest service.
     *
     * @param urlString the URL to use to invoke the service.
     * @return the service results.
     * @throws NoItemException  if <code>HTTP_BAD_REQUEST<code> is returned from the service.
     * @throws ServiceException if there is a problem invoking the service or retrieving its results.
     */
    public static String callService(String urlString) throws NoItemException, ServiceException
    {
        if (urlString == null || urlString.length() < 1)
            return null;

        InputStream inputStream = null;

        try
        {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();

            HttpURLConnection htpc = (HttpURLConnection) connection;
            int responseCode = htpc.getResponseCode();
            String responseMessage = htpc.getResponseMessage();

            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                inputStream = connection.getInputStream();
                ByteBuffer buffer = WWIO.readStreamToBuffer(inputStream);
                StringBuffer sb = new StringBuffer();
                while (buffer.hasRemaining() && !Thread.currentThread().isInterrupted())
                {
                    sb.append((char) buffer.get());
                }
                return sb.toString();
            }
            else if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST)
            {
                throw new NoItemException(responseMessage);
            }
            else
            {
                throw new ServiceException(responseMessage);
            }
        }
        catch (MalformedURLException e) // occurs only if protocol of URL is unknown
        {
            String msg = Logging.getMessage("generic.MalformedURL", urlString);
            Logging.logger().log(java.util.logging.Level.SEVERE, msg);
            throw new WWRuntimeException(msg);
        }
        catch (IOException e)
        {
            String msg = Logging.getMessage("POI.ServiceError", urlString);
            Logging.logger().log(java.util.logging.Level.SEVERE, msg);
            throw new ServiceException(msg);
        }
        finally
        {
            WWIO.closeStream(inputStream, urlString);
        }
    }
}
