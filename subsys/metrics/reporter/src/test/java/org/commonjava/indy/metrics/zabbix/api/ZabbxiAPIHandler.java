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
package org.commonjava.indy.metrics.zabbix.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.commonjava.test.http.expect.ExpectationHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by xiabai on 5/12/17.
 */
public class ZabbxiAPIHandler
                implements ExpectationHandler
{
    @Override
    public void handle( HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse )
                    throws ServletException, IOException
    {
        String json = IOUtils.toString( httpServletRequest.getInputStream() );
        ObjectMapper mapper = new ObjectMapper();
        Request request = mapper.readValue( json, Request.class );
        String result = "";
        if ( request.getMethod().equals( "host.get" ) )
        {
            result = getHostids();
        }
        if ( request.getMethod().equals( "item.get" ) )
        {
            result = getItem();
        }
        httpServletResponse.setCharacterEncoding( "UTF-8" );
        httpServletResponse.setContentType( "application/json; charset=utf-8" );
        httpServletResponse.setStatus( 200 );
        OutputStream out = httpServletResponse.getOutputStream();
        IOUtils.write( result, out );

    }

    private String getHostids() throws JsonProcessingException
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put( "jsonrpc", "2.0" );
        Map[] result = new HashMap[1];
        Map host = new HashMap();
        host.put( "hostid", "123" );
        result[0] = host;
        map.put( "result", result );
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString( map );
    }

    private String getItem() throws JsonProcessingException
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put( "jsonrpc", "2.0" );
        Map[] result = new HashMap[1];
        Map host = new HashMap();
        host.put( "itemid", "456" );
        result[0] = host;
        map.put( "result", result );
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString( map );
    }
}
