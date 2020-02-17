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
package org.commonjava.indy.subsys.honeycomb;

import io.honeycomb.beeline.tracing.Span;
import org.commonjava.indy.subsys.honeycomb.config.HoneycombConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.commonjava.indy.metrics.RequestContextHelper.getContext;

@ApplicationScoped
public class HoneycombFilter
                implements Filter
{
    @Inject
    private HoneycombManager honeycombManager;

    @Inject
    private HoneycombConfiguration config;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public void init( final FilterConfig filterConfig ) throws ServletException
    {
    }

    @Override
    public void doFilter( final ServletRequest request, final ServletResponse response, final FilterChain chain )
                    throws IOException, ServletException
    {
        logger.trace( "START: {}", getClass().getSimpleName() );

        HttpServletRequest hsr = (HttpServletRequest) request;
        logger.debug( "START: {}", hsr.getPathInfo() );

        Span rootSpan = null;
        try
        {
            rootSpan = honeycombManager.startRootTracer( getEndpointName( hsr.getMethod(), hsr.getPathInfo() ) );
            if ( rootSpan != null )
            {
                rootSpan.addField( "path_info", hsr.getPathInfo() );
            }

            chain.doFilter( request, response );
        }
        finally
        {
            logger.debug( "END: {}", hsr.getPathInfo() );
            if ( rootSpan != null )
            {
                honeycombManager.addFields( rootSpan );
                rootSpan.close();
                honeycombManager.endTrace();
            }

            logger.trace( "END: {}", getClass().getSimpleName() );
        }
    }

    private String getEndpointName( String method, String pathInfo )
    {
        StringBuilder sb = new StringBuilder( method + "_" );
        String[] toks = pathInfo.split( "/" );
        for ( String s : toks )
        {
            if ( isBlank( s ) || "api".equals( s ) )
            {
                continue;
            }
            sb.append( s );
            if ( "admin".equals( s ) )
            {
                sb.append( "_" );
            }
            else
            {
                break;
            }
        }
        return sb.toString();
    }

    @Override
    public void destroy()
    {
    }

}
