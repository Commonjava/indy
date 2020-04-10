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

import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.repo.proxy.conf.RepoProxyConfig;
import org.commonjava.indy.repo.proxy.create.ProxyRepoCreateManager;
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
import java.util.Optional;

import static java.util.Optional.empty;
import static javax.servlet.http.HttpServletResponse.SC_MOVED_PERMANENTLY;
import static org.commonjava.indy.repo.proxy.RepoProxyUtils.getOriginalStoreKeyFromPath;
import static org.commonjava.indy.repo.proxy.RepoProxyUtils.getProxyTo;

@ApplicationScoped
public class RepoProxyContoller
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Inject
    private RepoProxyConfig config;

    @Inject
    private ProxyRepoCreateManager repoCreateManager;

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
        if ( !checkEnabled() )
        {
            return false;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if ( !checkApiMethods( httpRequest ) )
        {
            return false;
        }

        final Optional<StoreKey> proxyToRemoteKey = getProxyToRemoteKey( httpRequest );
        if ( !proxyToRemoteKey.isPresent() )
        {
            return false;
        }

        final String absoluteOriginalPath = getAbsolutePath( httpRequest );
        final Optional<String> proxyToPath = getProxyTo( absoluteOriginalPath, proxyToRemoteKey.get() );
        if ( !proxyToPath.isPresent() )
        {
            return false;
        }

        trace( "proxied to path info {}", proxyToPath );

        HttpServletResponse decoratedResponse = decoratingResponse( httpRequest, (HttpServletResponse) response, proxyToRemoteKey.get() );

        // Here we do not use redirect but forward.
        // doRedirect( (HttpServletResponse)response, proxyTo );
        doForward( httpRequest, decoratedResponse, proxyToPath.get() );

        return true;
    }

    private boolean checkEnabled()
    {
        if ( !config.isEnabled() )
        {
            trace( "addon not enabled, will not do any proxy." );
            return false;
        }
        return true;
    }

    private boolean checkApiMethods( HttpServletRequest request )
    {
        final String method = request.getMethod().trim().toUpperCase();
        if ( !config.getApiMethods().contains( method ) )
        {
            trace( "http method {} not allowed for the request, no proxy action will be performed", method );
            return false;
        }
        return true;
    }

    private Optional<StoreKey> getProxyToRemoteKey( HttpServletRequest request )
    {
        final String absoluteOriginalPath = getAbsolutePath( request );

        final Optional<String> originKeyStr = getOriginalStoreKeyFromPath( absoluteOriginalPath );
        if ( !originKeyStr.isPresent() )
        {
            trace( "No matched repo path in absolute path {}, so no proxy action will be performed",
                   absoluteOriginalPath );
            return empty();
        }
        else
        {
            try
            {
                final Optional<RemoteRepository> proxyToRemote =
                        repoCreateManager.createProxyRemote( StoreKey.fromString( originKeyStr.get() ) );
                if ( !proxyToRemote.isPresent() )
                {
                    trace( "The proxy to remote can not be created or found for original store {}, no proxy will do.",
                           originKeyStr.get() );
                    return empty();
                }
                else
                {
                    trace( "absolute path {}", absoluteOriginalPath );
                    return Optional.of( proxyToRemote.get().getKey() );
                }
            }
            catch ( RepoProxyException e )
            {
                logger.error( "[Repository Proxy]: Error happened to create proxy to repository.", e );
                return empty();
            }
        }

    }

    private String getAbsolutePath(HttpServletRequest request){
        final String pathInfo = request.getPathInfo();

        return PathUtils.normalize( request.getServletPath(), request.getContextPath(), request.getPathInfo() );
    }

    private HttpServletResponse decoratingResponse( final HttpServletRequest request,
                                                    final HttpServletResponse response, final StoreKey proxyToStoreKey )
            throws IOException
    {
        HttpServletResponse decorated = response;

        for ( RepoProxyResponseDecorator decorator : responseDecorators )
        {
            decorated = decorator.decoratingResponse( request, decorated, proxyToStoreKey );
        }

        return decorated;
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
        trace( "will forward to {}", forwardTo );
        httpRequest.getRequestDispatcher( forwardTo ).forward( httpRequest, response );
    }

    private void trace( final String template, final Object... params )
    {
        RepoProxyUtils.trace( this.logger, template, params );
    }

}
