/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.bind.jaxrs.metrics;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.commonjava.indy.bind.jaxrs.metrics.events.JaxRSEvent;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose of this filter is to generate events for JaxRS calls when profiling is enabled.
 *
 */
@Provider
public class FlightRecorderFilter implements ContainerRequestFilter, ContainerResponseFilter
{

    @Override
    public void filter( ContainerRequestContext requestContext ) throws IOException
    {
        logger.trace("processing request context");
        final JaxRSEvent event = new JaxRSEvent();
        final boolean isEnabled = event.isEnabled();
        if ( !isEnabled )
        {
            return;
        }
        event.begin();
        requestContext.setProperty( JaxRSEvent.NAME, event );
    }

    @Override
    public void filter( ContainerRequestContext requestContext, ContainerResponseContext responseContext )
            throws IOException
    {
        logger.trace("processing response context");
        if ( requestContext == null )
        {
            logger.error( "request context is null" );
            return;
        }
        Object prop = requestContext.getProperty( JaxRSEvent.NAME );
        if ( prop == null )
        {
            return;
        }
        JaxRSEvent event = ( JaxRSEvent ) prop;
        if ( !event.isEnabled() )
        {
            return;
        }

        event.end();

        if ( event.shouldCommit() ) {
            event.method = requestContext.getMethod();
            event.mediaType = String.valueOf ( requestContext.getMediaType() ) ;
            event.length = requestContext.getLength();
            event.methodFrameName = getMethodName( requestContext );
            event.path = requestContext.getUriInfo().getPath();
            event.responseLength = responseContext.getLength();
            event.status = responseContext.getStatus();

            event.commit();
        }
    }

    private String getMethodName( ContainerRequestContext context )
    {
        Object p = context.getProperty( METHOD_NAME );
        if ( p == null )
        {
            return "";
        }
        if ( p instanceof ResourceMethodInvoker )
        {
            ResourceMethodInvoker invoker = (ResourceMethodInvoker) p;
            return invoker.getMethod().getName();
        }
        else
        {
            return "";
        }
    }

    private static final String METHOD_NAME  = ResourceMethodInvoker.class.getName();
    private static final Logger logger = LoggerFactory.getLogger( FlightRecorderFilter.class.getName() );
}
