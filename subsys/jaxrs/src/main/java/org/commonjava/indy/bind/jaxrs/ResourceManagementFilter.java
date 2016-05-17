/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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

import org.commonjava.maven.galley.spi.cache.CacheProvider;
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

@ApplicationScoped
public class ResourceManagementFilter
        implements Filter
{

    @Inject
    private CacheProvider cacheProvider;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public void init( final FilterConfig filterConfig )
            throws ServletException
    {
    }

    @Override
    public void doFilter( final ServletRequest request, final ServletResponse response, final FilterChain chain )
            throws IOException, ServletException
    {
        final HttpServletRequest hsr = (HttpServletRequest) request;
        String name = Thread.currentThread().getName();

        String tn = hsr.getMethod() + " " + hsr.getPathInfo() + " (" + System.currentTimeMillis() + "." + System.nanoTime() + ")";
        String clientAddr = request.getRemoteAddr();
        try
        {
            logger.info( "START request: {} (from: {})", tn, clientAddr );

            Thread.currentThread().setName( tn );
            chain.doFilter( request, response );
        }
        finally
        {
            logger.debug( "Cleaning up resources for thread: {}", Thread.currentThread().getName() );
            Thread.currentThread().setName( name );
            try
            {
                cacheProvider.cleanupCurrentThread();
            }
            catch ( Exception e )
            {
                logger.error( "Failed to cleanup resources", e );
            }

            logger.info( "END request: {} (from: {})", tn, clientAddr );
        }
    }

    @Override
    public void destroy()
    {
    }

}
