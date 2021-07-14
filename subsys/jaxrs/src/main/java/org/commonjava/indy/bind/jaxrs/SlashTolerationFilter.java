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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@ApplicationScoped
public class SlashTolerationFilter
        implements Filter
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public void doFilter( ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain )
            throws IOException, ServletException
    {
        final HttpServletRequest hsr = (HttpServletRequest) servletRequest;

        final HttpServletRequestWrapper wrapped = new HttpServletRequestWrapper(hsr) {
            @Override
            public StringBuffer getRequestURL() {
                final StringBuffer originalUrl = hsr.getRequestURL();
                try
                {
                    return new StringBuffer( new URI( originalUrl.toString()).normalize().toString() );
                }
                catch ( URISyntaxException e )
                {
                    logger.error("URL rewriting error.", e);
                }
                return originalUrl;
            }
            @Override
            public String getRequestURI(){
                try
                {
                    return new URI( hsr.getRequestURI() ).normalize().toString();
                }
                catch ( URISyntaxException e )
                {
                    logger.error("URL rewriting error.", e);
                }
                return hsr.getRequestURI();
            }

        };
        filterChain.doFilter(wrapped, servletResponse);
    }

}
