/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.metrics.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.indy.metrics.zabbix.sender.SenderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xiabai on 5/9/17.
 */
public class Capitalizer
                implements Runnable
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private Socket socket;

    private int clientNumber;

    private Expectation expect;

    private ConcurrentHashMap<String, Expectation> expections;

    public Capitalizer( Socket socket, int clientNumber, ConcurrentHashMap<String, Expectation> expections )
    {
        this.socket = socket;
        this.clientNumber = clientNumber;
        this.expections = expections;
        logger.info( "New connection with client# " + clientNumber + " at " + socket );
    }

    /**
     */
    public void run()
    {
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try
        {

            in = new BufferedInputStream( socket.getInputStream() );
            ObjectMapper mapper = new ObjectMapper();
            byte[] responseData = new byte[1024];
            int readCount = 0;
            int read = in.read( responseData, readCount, responseData.length - readCount );
            //                }
            byte[] responseDataTmp = Arrays.copyOfRange( responseData, 13, responseData.length );
            SenderRequest senderRequest = mapper.readValue( responseDataTmp, SenderRequest.class );
            out = new BufferedOutputStream( socket.getOutputStream() );
            ZabbixResult zr = new ZabbixResult( expections.get( senderRequest.getRequest() ).getSenderResult() );
            out.write( getResult( mapper.writeValueAsBytes( zr ) ) );
            out.flush();
            logger.info( senderRequest.toString() );

        }
        catch ( IOException e )
        {
            logger.info( "Error handling client# " + clientNumber + ": " + e );
        }
        finally
        {
            try
            {
                if ( in != null )
                {
                    in.close();
                }
                if ( out != null )
                {
                    out.close();
                }
                if ( socket != null )
                {
                    socket.close();
                }
            }
            catch ( IOException e )
            {
                logger.info( "Couldn't close a socket, what's going on?" );
            }
            logger.info( "Connection with client# " + clientNumber + " closed" );
        }
    }

    private byte[] getResult( byte[] responseData )
    {
        byte header[] = { 'Z', 'B', 'X', 'D', '\1' };

        byte[] info = new byte[header.length + 4 + 4 + responseData.length];

        System.arraycopy( header, 0, info, 0, header.length );

        info[header.length] = (byte) ( responseData.length & 0xFF );
        info[header.length + 1] = (byte) ( ( responseData.length >> 8 ) & 0x00FF );
        info[header.length + 2] = (byte) ( ( responseData.length >> 16 ) & 0x0000FF );
        info[header.length + 3] = (byte) ( ( responseData.length >> 24 ) & 0x000000FF );

        System.arraycopy( responseData, 0, info, header.length + 4 + 4, responseData.length );
        return info;
    }
}
