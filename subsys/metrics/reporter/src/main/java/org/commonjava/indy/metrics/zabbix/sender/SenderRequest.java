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
package org.commonjava.indy.metrics.zabbix.sender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 *
 */
public class SenderRequest
{
    static final byte header[] = { 'Z', 'B', 'X', 'D', '\1' };

    private static final Logger logger = LoggerFactory.getLogger( SenderRequest.class );

    /**
     * TimeUnit is SECONDS.
     */
    long clock;

    List<DataObject> data;

    String request = "sender data";

    public byte[] toBytes() throws JsonProcessingException
    {
        // https://www.zabbix.org/wiki/Docs/protocols/zabbix_sender/2.0
        // https://www.zabbix.org/wiki/Docs/protocols/zabbix_sender/1.8/java_example

        ObjectMapper mapper = new ObjectMapper();
        byte[] jsonBytes = mapper.writeValueAsBytes( this );

        byte[] result = new byte[header.length + 4 + 4 + jsonBytes.length];

        System.arraycopy( header, 0, result, 0, header.length );

        result[header.length] = (byte) ( jsonBytes.length & 0xFF );
        result[header.length + 1] = (byte) ( ( jsonBytes.length >> 8 ) & 0x00FF );
        result[header.length + 2] = (byte) ( ( jsonBytes.length >> 16 ) & 0x0000FF );
        result[header.length + 3] = (byte) ( ( jsonBytes.length >> 24 ) & 0x000000FF );

        System.arraycopy( jsonBytes, 0, result, header.length + 4 + 4, jsonBytes.length );
        return result;
    }

    public String getRequest()
    {
        return request;
    }

    public void setRequest( String request )
    {
        this.request = request;
    }

    /**
     * TimeUnit is SECONDS.
     *
     * @return
     */
    public long getClock()
    {
        return clock;
    }

    /**
     * TimeUnit is SECONDS.
     *
     * @param clock
     */
    public void setClock( long clock )
    {
        this.clock = clock;
    }

    public List<DataObject> getData()
    {
        return data;
    }

    public void setData( List<DataObject> data )
    {
        this.data = data;
    }

    @Override
    public String toString()
    {
        try
        {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString( this );

        }
        catch ( JsonProcessingException e )
        {
            logger.error( e.getMessage() );
        }
        return super.toString();
    }
}

