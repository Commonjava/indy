/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.bind.jaxrs;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.util.RequestContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.commonjava.indy.util.RequestContextHelper.CLIENT_ADDR;
import static org.commonjava.indy.util.RequestContextHelper.COMPONENT_ID;
import static org.commonjava.indy.util.RequestContextHelper.EXTERNAL_TRACE_ID;
import static org.commonjava.indy.util.RequestContextHelper.FORCE_METERED;
import static org.commonjava.indy.util.RequestContextHelper.HTTP_METHOD;
import static org.commonjava.indy.util.RequestContextHelper.HTTP_REQUEST_URI;
import static org.commonjava.indy.util.RequestContextHelper.INTERNAL_ID;
import static org.commonjava.indy.util.RequestContextHelper.TRACE_ID;
import static org.commonjava.indy.util.RequestContextHelper.REQUEST_PARENT_SPAN;
import static org.commonjava.indy.util.RequestContextHelper.SPAN_ID_HEADER;
import static org.commonjava.indy.util.RequestContextHelper.setContext;

@ApplicationScoped
public class MDCManager
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private IndyConfiguration config;

    public MDCManager() {}

    public void clear()
    {
        MDC.clear();
    }

    private List<String> mdcHeadersList = new ArrayList();

    @PostConstruct
    public void setUp()
    {
        mdcHeadersList.add( COMPONENT_ID ); // default

        String mdcHeaders = config.getMdcHeaders();
        if ( isNotBlank( mdcHeaders ) )
        {
            String[] headers = mdcHeaders.split( "," );
            for ( String s : headers )
            {
                if ( isNotBlank( s ) && !mdcHeadersList.contains( s ) )
                {
                    mdcHeadersList.add( s.trim() );
                }
            }
        }
    }

    public void putExternalID( String externalID )
    {
        String internalID = UUID.randomUUID().toString();
        String preferredID = externalID != null ? externalID : internalID;
        putRequestIDs( internalID, externalID, preferredID, null );
    }

    public void putRequestIDs( String internalID, String externalID, String preferredID, final String spanID )
    {
        RequestContextHelper.setContext( TRACE_ID, preferredID );
        RequestContextHelper.setContext( INTERNAL_ID, internalID );

        if ( externalID != null )
        {
            RequestContextHelper.setContext( EXTERNAL_TRACE_ID, externalID );
        }

        if ( spanID != null )
        {
            RequestContextHelper.setContext( REQUEST_PARENT_SPAN, spanID );
        }
    }

    public void putUserIP( String userIp )
    {
        RequestContextHelper.setContext( CLIENT_ADDR, userIp );
    }

    public void putExtraHeaders( HttpServletRequest request )
    {
        // use setContext here so we get this value in ThreadContext too, for decision-making in the workflow, SLI classification, etc.
        setContext( HTTP_METHOD, request.getMethod() );

        String forceMetered = request.getHeader( FORCE_METERED );
        RequestContextHelper.setContext( FORCE_METERED,
                                         forceMetered != null && Boolean.TRUE.equals( Boolean.parseBoolean( forceMetered ) ) );

        RequestContextHelper.setContext( HTTP_REQUEST_URI, request.getRequestURI() );
        mdcHeadersList.forEach( ( header ) -> RequestContextHelper.setContext( header, request.getHeader( header ) ) );
    }

    public void putExtraHeaders( HttpRequest httpRequest )
    {
        mdcHeadersList.forEach( ( header ) -> {
            Header h = httpRequest.getFirstHeader( header );
            if ( h != null )
            {
                RequestContextHelper.setContext( header, h.getValue() );
            }
        } );
    }

    public void putRequestIDs( final HttpServletRequest hsr )
    {
        /* We would always generate internalID and provide that in the MDC.
         * If the calling service supplies an traceID, we'd map that under its own key.
         * PreferredID should try to use traceID if it's available, and default over to using internalID if it's not.
         * What this gives us is a single key we can use to reference an ID for the request,
         * and whenever possible it'll reflect the externally supplied ID.
         */
        String internalID = UUID.randomUUID().toString();

        String traceID = hsr.getHeader( EXTERNAL_TRACE_ID );
        String spanID = hsr.getHeader( SPAN_ID_HEADER );

        String preferredID = traceID != null ? traceID : internalID;


        putRequestIDs( internalID, traceID, preferredID, spanID );
    }

}
