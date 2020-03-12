/**
 * Copyright (C) 2013 Red Hat, Inc.
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
package org.commonjava.indy.repo.proxy;

import org.commonjava.indy.repo.proxy.conf.RepoProxyConfig;
import org.commonjava.maven.galley.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_MOVED_PERMANENTLY;
import static org.commonjava.indy.repo.proxy.RepoProxyAddon.ADDON_NAME;
import static org.commonjava.indy.repo.proxy.RepoProxyUtils.proxyTo;

@ApplicationScoped
public class RepoProxyContoller
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Inject
    private RepoProxyConfig config;

    @Inject
    private Instance<RepoProxyResponseDecorator> responseDecoratorInstances;

    private Iterable<RepoProxyResponseDecorator> responseDecorators;

    protected RepoProxyContoller()
    {
    }

    public RepoProxyContoller( final RepoProxyConfig config,
                               final Iterable<RepoProxyResponseDecorator> responseDecorators )
    {
        this.config = config;
        this.responseDecorators = responseDecorators;
    }

    @PostConstruct
    public void init()
    {
        this.responseDecorators = this.responseDecoratorInstances;
    }

    public boolean doProxy( ServletRequest request, ServletResponse response )
            throws IOException, ServletException
    {
        if ( !config.isEnabled() )
        {
            trace( "addon not enabled, will not do any proxy." );
            return false;
        }
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final String method = httpRequest.getMethod().trim().toUpperCase();
        if ( !config.getApiMethods().contains( method ) )
        {
            trace( "http method {} not allowed for the request, no proxy action will be performed", method );
            return false;
        }
        final String pathInfo = httpRequest.getPathInfo();

        final String absoluteOriginalPath =
                PathUtils.normalize( httpRequest.getServletPath(), httpRequest.getContextPath(),
                                     httpRequest.getPathInfo() );
        trace( "absolute path {}", absoluteOriginalPath );
        final String proxyTo = proxyTo( absoluteOriginalPath, config.getProxyRules() );
        if ( proxyTo == null )
        {
            return false;
        }
        trace( "proxied to path info {}", proxyTo );

        HttpServletResponse decoratedResponse = decoratingResponse( httpRequest, (HttpServletResponse) response );

        // Here we do not use redirect but forward.
        // doRedirect( (HttpServletResponse)response, proxyTo );
        doForward( httpRequest, decoratedResponse, proxyTo );

        return true;
    }

    //TODO: not used but just leave here for reference
    private void doRedirect( final HttpServletResponse httpResponse, final String directTo )
            throws IOException
    {
        trace( "will redirect to {}", directTo );
        httpResponse.setStatus( SC_MOVED_PERMANENTLY );
        httpResponse.setHeader( "Location", directTo );
        httpResponse.sendRedirect( directTo );
    }

    private void doForward( final HttpServletRequest httpRequest, final ServletResponse response,
                            final String forwardTo )
            throws IOException, ServletException
    {
        trace( "will redirect to {}", forwardTo );
        httpRequest.getRequestDispatcher( forwardTo ).forward( httpRequest, response );
    }

    private HttpServletResponse decoratingResponse( final HttpServletRequest request,
                                                    final HttpServletResponse response )
            throws IOException
    {
        HttpServletResponse decorated = response;

        for ( RepoProxyResponseDecorator decorator : responseDecorators )
        {
            decorated = decorator.decoratingResponse( request, decorated );
        }

        return decorated;
    }

    private void trace( final String template, final Object... params )
    {
        RepoProxyUtils.trace( logger, template, params );
    }

}
