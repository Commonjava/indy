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
package org.commonjava.indy.repo.proxy;

import org.apache.commons.lang3.StringUtils;
import org.commonjava.indy.client.core.Indy;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.content.browse.client.IndyContentBrowseClientModule;
import org.commonjava.indy.content.browse.model.ContentBrowseResult;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.repo.proxy.conf.RepoProxyConfig;
import org.commonjava.util.jhttpc.auth.MemoryPasswordManager;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.commonjava.util.jhttpc.model.SiteConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.commonjava.indy.repo.proxy.RepoProxyAddon.ADDON_NAME;
import static org.commonjava.indy.repo.proxy.RepoProxyUtils.getRequestAbsolutePath;

/**
 * This rewrite manager handles the content listing request for case that is with a indy remote target. It will
 * call remote indy content listing api instead of local listing request to get the real content listing result.
 */
@ApplicationScoped
public class ContentBrowseRemoteIndyListingRewriteManager

{
    private static final Logger logger = LoggerFactory.getLogger( ContentBrowseRemoteIndyListingRewriteManager.class );

    @Inject
    private RepoProxyConfig config;

    @Inject
    private IndyObjectMapper mapper;

    private Indy indyClient;

    @PostConstruct
    public void initIndyClient()
    {
        if ( !config.isEnabled() || !config.isRemoteIndyListingRewriteEnabled() )
        {
            logger.debug(
                    "[{}] Addon not enabled or not allowed to use remote indy listing rewriting, will not init indy client.",
                    ADDON_NAME );
            return;
        }
        final String remoteIndyUrl = config.getDefaultRemoteIndyUrl();
        if ( remoteIndyUrl != null )
        {
            SiteConfig siteConfig =
                    new SiteConfigBuilder( "indy-static", remoteIndyUrl + "api/" ).withRequestTimeoutSeconds(
                            config.getRemoteIndyRequestTimeout() ).build();
            try
            {
                //FIXME: Here we used a MemPassMgr for simple. Maybe some changes in future for more complex cases.
                indyClient = new Indy( siteConfig, new MemoryPasswordManager(), mapper,
                                       new IndyContentBrowseClientModule() );
            }
            catch ( IndyClientException e )
            {
                logger.error(
                        String.format( "[%s] Cannot init indy client successfully for remote indy access", ADDON_NAME ),
                        e );
            }
        }
        else
        {
            logger.warn(
                    "[{}] remote indy listing rewriting enabled but no remote indy supplied, please check your configuration to add remote indy url.",
                    ADDON_NAME );
        }
    }

    public boolean rewriteResponse( HttpServletRequest request, HttpServletResponse response )
            throws IOException
    {
        if ( !config.isEnabled() || !config.isRemoteIndyListingRewriteEnabled() )
        {
            logger.debug(
                    "[{}] Addon not enabled or not allowed to use remote indy listing rewriting, will not decorate the response by remote indy listing rewriting.",
                    ADDON_NAME );
            return false;
        }

        final String absolutePath = getRequestAbsolutePath( request );
        if ( !absolutePath.startsWith( "/api/browse/" ) )
        {
            logger.debug(
                    "[{}] Remote indy listing rewriting: {} is not a content browse request, will not decorate the response. ",
                    ADDON_NAME, absolutePath );
            return false;
        }
        final String httpMethod = request.getMethod();
        if ( !httpMethod.equals( HttpMethod.GET ) && !httpMethod.equals( HttpMethod.HEAD ) )
        {
            logger.warn( "[{}] Directory listing does not support this type of method: {}", ADDON_NAME, httpMethod );
            response.sendError( SC_METHOD_NOT_ALLOWED );
            return true;
        }

        final String pathInfo = request.getPathInfo();
        final Optional<String> originalRepo = RepoProxyUtils.getOriginalStoreKeyFromPath( pathInfo );
        if ( !originalRepo.isPresent() )
        {
            logger.debug( "[{}] No matched repo path in request path {}, will not rewrite.", ADDON_NAME, pathInfo );
            return false;
        }

        final String path = RepoProxyUtils.extractPath( absolutePath );
        final String remoteIndyUrl = config.getDefaultRemoteIndyUrl();

        logger.trace( "Start to do {} request to remote indy content list for indy {} and path {}", httpMethod,
                      remoteIndyUrl, path );
        try
        {
            final StoreKey originalStoreKey = StoreKey.fromString( originalRepo.get() );
            switch ( httpMethod )
            {
                case HttpMethod.GET:
                    return handleGetRequest( remoteIndyUrl, originalStoreKey, path, request, response );
                case HttpMethod.HEAD:
                    return handleHeadRequest( originalStoreKey, path, response );
                default:
                    logger.warn( "[{}] Directory listing does not support this type of method: {}", ADDON_NAME,
                                 httpMethod );
                    response.sendError( SC_METHOD_NOT_ALLOWED );
                    return true;
            }
        }
        catch ( IOException | IndyClientException e )
        {
            logger.error( String.format( "[%s]Error happened during content browse rewriting", ADDON_NAME ), e );
            response.sendError( SC_INTERNAL_SERVER_ERROR, e.getMessage() );
            return true;
        }
    }

    private boolean handleGetRequest( final String remoteIndyUrl, final StoreKey originalStoreKey, final String path,
                                      final HttpServletRequest request, final HttpServletResponse response )
            throws IOException, IndyClientException
    {
        URL remoteIndyURL = new URL( remoteIndyUrl );
        String remoteIndyHost = remoteIndyURL.getHost();
        int remotePort = remoteIndyURL.getPort();
        // 80 or 443 can be ignored in http(s) url
        if ( remoteIndyUrl.contains( ":" ) && remotePort != 80 && remotePort != 443 && remotePort > 0 )
        {
            remoteIndyHost = remoteIndyHost + ":" + remotePort;
        }
        String localProxyHost = request.getServerName();
        int localPort = request.getServerPort();
        if ( localPort != 80 && localPort != 443 && localPort > 0 )
        {
            localProxyHost = localProxyHost + ":" + localPort;
        }

        final String remoteContentResult = getRemoteIndyListingContent( originalStoreKey, path );

        if ( StringUtils.isNotBlank( remoteContentResult ) )
        {
            logger.trace( "Start replacing original host {} to target host {}", remoteIndyHost, localProxyHost );
            final String replaced =
                    RepoProxyUtils.replaceAllWithNoRegex( remoteContentResult, remoteIndyHost, localProxyHost );
            logger.trace( "Replaced result for remote indy content list: {}", replaced );
            logger.trace( "Handle get request for content browse rewrite" );
            response.getWriter().write( replaced );
            return true;
        }

        return false;
    }

    private String getRemoteIndyListingContent( final StoreKey key, final String path )
            throws IOException, IndyClientException
    {
        if ( indyClient == null )
        {
            initIndyClient();
            if ( indyClient == null )
            {
                throw new IOException( "Failed to init indy client to access remote indy" );
            }
        }
        ContentBrowseResult result =
                indyClient.module( IndyContentBrowseClientModule.class ).getContentList( key, path );
        return mapper.writeValueAsString( result );
    }

    private boolean handleHeadRequest( final StoreKey originalStoreKey, final String path,
                                       final HttpServletResponse response )
            throws IOException, IndyClientException
    {
        logger.trace( "Handle head request for content browse rewrite" );
        Map<String, String> headers = getRemoteIndyListingHeaders( originalStoreKey, path );
        for ( Map.Entry<String, String> entry : headers.entrySet() )
        {
            response.setHeader( entry.getKey(), entry.getValue() );
        }
        response.setStatus( SC_OK );
        return true;
    }

    private Map<String, String> getRemoteIndyListingHeaders( final StoreKey key, final String path )
            throws IOException, IndyClientException
    {
        if ( indyClient == null )
        {
            initIndyClient();
            if ( indyClient == null )
            {
                throw new IOException( "Failed to init indy client to access remote indy" );
            }
        }
        return indyClient.module( IndyContentBrowseClientModule.class ).headForContentList( key, path );
    }

}


