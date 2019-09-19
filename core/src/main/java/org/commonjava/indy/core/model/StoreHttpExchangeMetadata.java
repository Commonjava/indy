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
package org.commonjava.indy.core.model;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.commonjava.maven.galley.transport.htcli.model.HttpExchangeMetadata;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class StoreHttpExchangeMetadata
        extends HttpExchangeMetadata
{
    public StoreHttpExchangeMetadata()
    {
    }

    public StoreHttpExchangeMetadata( HttpRequest request, HttpResponse response )
    {
        super( request, response );
    }

    public StoreHttpExchangeMetadata( final HttpServletRequest request, final Response response )
    {
        populateHeaders( requestHeaders, request );
        populateHeaders( responseHeaders, response.getStringHeaders() );

        final Response.StatusType st = response.getStatusInfo();
        this.responseStatusCode = st.getStatusCode();
        this.responseStatusMessage = st.getReasonPhrase();
    }

    private void populateHeaders( final Map<String, List<String>> headerMap,
                                  final MultivaluedMap<String, String> allHeadersMap )
    {
        for ( final String name : allHeadersMap.keySet() )
        {
            List<String> values = headerMap.get( name );
            if ( values == null )
            {
                values = new ArrayList<String>();
                headerMap.put( name.toUpperCase(), values );
            }

            values.add( allHeadersMap.getFirst( name ) );
        }
    }

    private void populateHeaders( final Map<String, List<String>> headerMap, final HttpServletRequest request )
    {
        if ( request == null )
        {
            return;
        }

        Enumeration<String> en = request.getHeaderNames();
        while ( en.hasMoreElements() )
        {
            String name = en.nextElement();
            List<String> values = headerMap.get( name );
            if ( values == null )
            {
                values = new ArrayList<String>();
                headerMap.put( name.toUpperCase(), values );
            }
            values.add( request.getHeader( name ) );
        }
    }
}
