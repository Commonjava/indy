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

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

@ApplicationScoped
public class SlashTolerationFilter
        implements Filter
{

    private static final String PROTOCOL_HTTP = "http:";
    private static final String PROTOCOL_HTTPS = "https:";

    @Override
    public void init( final FilterConfig filterConfig )
                    throws ServletException
    {
    }

    @Override
    public void doFilter( ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain )
            throws IOException, ServletException
    {
        final HttpServletRequest hsr = (HttpServletRequest) servletRequest;

        final HttpServletRequestWrapper wrapped = new HttpServletRequestWrapper(hsr) {
            @Override
            public StringBuffer getRequestURL() {
                final StringBuffer originalUrl = ((HttpServletRequest) getRequest()).getRequestURL();
                if ( originalUrl.indexOf( PROTOCOL_HTTP ) != -1 )
                {
                    String temp = originalUrl.toString().substring( PROTOCOL_HTTP.length() + 2 );
                    return new StringBuffer( PROTOCOL_HTTP ).append( "//" ).append( handleSlash( temp ) );
                }
                else if ( originalUrl.indexOf( PROTOCOL_HTTPS ) != -1 )
                {
                    String temp = originalUrl.toString().substring( PROTOCOL_HTTPS.length() + 2 );
                    return new StringBuffer( PROTOCOL_HTTPS ).append( "//" ).append( handleSlash( temp ) );
                }
                else
                {
                    return originalUrl;
                }
            }
            @Override
            public String getRequestURI(){
                return handleSlash( hsr.getRequestURI() );
            }

            private String handleSlash( String url ){
                return url.replaceAll( "/+", "/" );
            }
        };
        filterChain.doFilter(wrapped, servletResponse);
    }

    @Override
    public void destroy()
    {
    }

}
