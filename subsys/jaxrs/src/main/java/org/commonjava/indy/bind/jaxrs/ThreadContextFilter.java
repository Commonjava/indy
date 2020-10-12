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
package org.commonjava.indy.bind.jaxrs;

import org.commonjava.cdi.util.weft.ThreadContext;
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

import static org.commonjava.indy.util.RequestContextHelper.X_FORWARDED_FOR;

@ApplicationScoped
public class ThreadContextFilter
        implements Filter
{
    @Inject
    private MDCManager mdcManager;

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
        try
        {
            ThreadContext.clearContext();
            ThreadContext.getContext( true );

            if ( request instanceof HttpServletRequest )
            {
                HttpServletRequest hsr = (HttpServletRequest) request;

                String clientAddr = request.getRemoteAddr();
                final String xForwardFor = hsr.getHeader( X_FORWARDED_FOR );
                if ( xForwardFor != null )
                {
                    clientAddr = xForwardFor; // OSE proxy use HTTP header 'x-forwarded-for' to represent user IP
                }

                mdcManager.putUserIP( clientAddr );
                mdcManager.putExtraHeaders( hsr );
                mdcManager.putRequestIDs( hsr );
            }

            chain.doFilter( request, response );
        }
        finally
        {
            ThreadContext.clearContext();
            mdcManager.clear();
        }
    }

    @Override
    public void destroy()
    {
    }

}
