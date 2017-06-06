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
package org.commonjava.indy.httprox.handler;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.commonjava.indy.IndyMetricsNames;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.core.ctl.ContentController;
import org.commonjava.indy.data.ArtifactStoreQuery;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.folo.ctl.FoloConstants;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.httprox.conf.HttproxConfig;
import org.commonjava.indy.httprox.conf.TrackingType;
import org.commonjava.indy.httprox.keycloak.KeycloakProxyAuthenticator;
import org.commonjava.indy.httprox.metrics.IndyMetricsHttpProxyNames;
import org.commonjava.indy.httprox.util.HttpConduitWrapper;
import org.commonjava.indy.measure.annotation.IndyMetrics;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.measure.annotation.MetricNamed;
import org.commonjava.indy.model.core.AccessChannel;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.GenericPackageTypeDescriptor;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.subsys.http.HttpWrapper;
import org.commonjava.indy.subsys.http.util.UserPass;
import org.commonjava.indy.util.ApplicationHeader;
import org.commonjava.indy.util.ApplicationStatus;
import org.commonjava.indy.util.UrlInfo;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.conduits.ConduitStreamSinkChannel;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.rmi.Remote;

import static org.commonjava.indy.httprox.util.HttpProxyConstants.ALLOW_HEADER_VALUE;
import static org.commonjava.indy.httprox.util.HttpProxyConstants.GET_METHOD;
import static org.commonjava.indy.httprox.util.HttpProxyConstants.HEAD_METHOD;
import static org.commonjava.indy.httprox.util.HttpProxyConstants.OPTIONS_METHOD;
import static org.commonjava.indy.httprox.util.HttpProxyConstants.PROXY_AUTHENTICATE_FORMAT;
import static org.commonjava.indy.httprox.util.HttpProxyConstants.PROXY_REPO_PREFIX;
import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_PKG_KEY;
import static org.commonjava.maven.galley.io.SpecialPathConstants.PKG_TYPE_GENERIC_HTTP;

public final class ProxyResponseWriter
                implements ChannelListener<ConduitStreamSinkChannel>
{

    private static final String TRACKED_USER_SUFFIX = "+tracking";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private Throwable error;

    private final HttproxConfig config;

    private final ContentController contentController;

    private final StoreDataManager storeManager;

    private KeycloakProxyAuthenticator proxyAuthenticator;

    private CacheProvider cacheProvider;

    private ProxyRepositoryCreator repoCreator;

    private boolean transferred;

    private HttpRequest httpRequest;

    public ProxyResponseWriter( final HttproxConfig config, final StoreDataManager storeManager,
                                final ContentController contentController,
                                final KeycloakProxyAuthenticator proxyAuthenticator, final CacheProvider cacheProvider,
                                final ProxyRepositoryCreator repoCreator )
    {
        this.config = config;
        this.contentController = contentController;
        this.storeManager = storeManager;
        this.proxyAuthenticator = proxyAuthenticator;
        this.cacheProvider = cacheProvider;
        this.repoCreator = repoCreator;
    }

    @Override
    @IndyMetrics( measure = @Measure( meters = @MetricNamed( name =
                    IndyMetricsHttpProxyNames.METHOD_PROXYRESPONSEWRITER_HANDLEEVENT + IndyMetricsNames.METER ) ) )
    public void handleEvent( final ConduitStreamSinkChannel channel )
    {
        HttpConduitWrapper http = new HttpConduitWrapper( channel, httpRequest, contentController, cacheProvider );
        if ( httpRequest == null )
        {
            if ( error != null )
            {
                logger.debug( "handling error from request reader: " + error.getMessage(), error );
                handleError( error, http );
            }
            else
            {
                logger.debug( "invalid state (no error or request) from request reader. Sending 400." );
                try
                {
                    http.writeStatus( ApplicationStatus.BAD_REQUEST );
                }
                catch ( final IOException e )
                {
                    logger.error( "Failed to write BAD REQUEST for missing HTTP first-line to response channel.", e );
                }
            }

            return;
        }

        // TODO: Can we handle this?
        final String oldThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName( "PROXY-" + httpRequest.getRequestLine().toString() );
        channel.getCloseSetter().set( ( sinkChannel ) ->
        {
            logger.debug("sink channel closing.");
            Thread.currentThread().setName( oldThreadName );
        } );

        logger.info( "\n\n\n>>>>>>> Handle write\n\n\n\n\n" );
        if ( error == null )
        {
            try
            {
                final UserPass proxyUserPass =
                                UserPass.parse( ApplicationHeader.proxy_authorization, httpRequest, null );

                logger.debug( "Proxy UserPass: {}\nConfig secured? {}\nConfig tracking type: {}", proxyUserPass, config.isSecured(), config.getTrackingType() );
                if ( proxyUserPass == null && ( config.isSecured()
                        || TrackingType.ALWAYS == config.getTrackingType() ) )
                {

                    http.writeStatus( ApplicationStatus.PROXY_AUTHENTICATION_REQUIRED );
                    http.writeHeader( ApplicationHeader.proxy_authenticate,
                                      String.format( PROXY_AUTHENTICATE_FORMAT, config.getProxyRealm() ) );
                }
                else
                {
                    RequestLine requestLine = httpRequest.getRequestLine();
                    String method = requestLine.getMethod().toUpperCase();
                    switch ( method )
                    {
                        case GET_METHOD:
                        case HEAD_METHOD:
                        {
                            if ( proxyUserPass != null )
                            {
                                logger.debug(
                                        "Passing BASIC authentication credentials to Keycloak bearer-token translation authenticator" );
                                if ( !proxyAuthenticator.authenticate( proxyUserPass, http ) )
                                {
                                    break;
                                }
                            }

                            final URL url = new URL( requestLine.getUri() );
                            final RemoteRepository repo = getRepository( url );
                            transfer( http, repo, url.getPath(), GET_METHOD.equals( method ), proxyUserPass );
                            break;
                        }
                        case OPTIONS_METHOD:
                        {
                            http.writeStatus( ApplicationStatus.OK );
                            http.writeHeader( ApplicationHeader.allow, ALLOW_HEADER_VALUE );
                            break;
                        }
                        default:
                        {
                            http.writeStatus( ApplicationStatus.METHOD_NOT_ALLOWED );
                        }
                    }
                }

                logger.info( "Response complete." );
            }
            catch ( final Throwable e )
            {
                error = e;
            }
        }

        if ( error != null )
        {
            handleError( error, http );
        }

        try
        {
            http.close();
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to flush/shutdown response.", e );
        }
    }

    private void handleError( final Throwable error, final HttpWrapper http )
    {
        logger.error( "HTTProx request failed: " + error.getMessage(), error );
        try
        {
            if ( http.isOpen() )
            {
                if ( error instanceof IndyWorkflowException )
                {
                    http.writeStatus( ApplicationStatus.getStatus( ( (IndyWorkflowException) error ).getStatus() ) );
                }
                else
                {
                    http.writeStatus( ApplicationStatus.SERVER_ERROR );
                }

                http.writeError( error );

                logger.debug( "Response error complete." );
                //                    Channels.flushBlocking( channel );
                //                    channel.close();
            }
        }
        catch ( final IOException closeException )
        {
            logger.error( "Failed to close httprox request: " + error.getMessage(), error );
        }
    }

    private void transfer( final HttpConduitWrapper http, final RemoteRepository repo, final String path,
                           final boolean writeBody, final UserPass proxyUserPass )
                    throws IOException, IndyWorkflowException
    {
        if ( transferred )
        {
            return;
        }

        transferred = true;
        if ( !http.isOpen() )
        {
            throw new IOException( "Sink channel already closed (or null)!" );
        }

        final EventMetadata eventMetadata = createEventMetadata( writeBody, proxyUserPass, path, repo );

        Transfer txfr = null;
        try
        {
            txfr = contentController.get( repo.getKey(), path, eventMetadata );
        }
        catch ( final IndyWorkflowException e )
        {
            // TODO: timeouts?
            // block TransferException to allow handling below.
            if ( !( e.getCause() instanceof TransferException ) )
            {
                throw e;
            }
            logger.debug( "Suppressed exception for further handling inside proxy logic:", e );
        }

        if ( txfr != null && txfr.exists() )
        {
            http.writeExistingTransfer( txfr, writeBody, path, eventMetadata );
        }
        else
        {
            http.writeNotFoundTransfer( repo, path );
        }
    }

    private EventMetadata createEventMetadata( final boolean writeBody, final UserPass proxyUserPass, final String path,
                                               final RemoteRepository repo )
                    throws IndyWorkflowException
    {
        final EventMetadata eventMetadata = new EventMetadata();
        if ( writeBody )
        {
            TrackingKey tk = null;
            switch ( config.getTrackingType() )
            {
                case ALWAYS:
                {
                    if ( proxyUserPass == null )
                    {
                        throw new IndyWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                                          "Tracking is always-on, but no username was provided! Cannot initialize tracking key." );
                    }

                    tk = new TrackingKey( proxyUserPass.getUser() );

                    break;
                }
                case SUFFIX:
                {
                    if ( proxyUserPass != null )
                    {
                        final String user = proxyUserPass.getUser();

                        // TODO: Will this always be non-null here? Can we have an unsecured proxy?
                        if ( user.endsWith( TRACKED_USER_SUFFIX ) && user.length() > TRACKED_USER_SUFFIX.length() )
                        {
                            tk = new TrackingKey( StringUtils.substring( user, 0, - TRACKED_USER_SUFFIX.length() ) );
                        }
                    }

                    break;
                }
                default:
                {
                }
            }

            if ( tk != null )
            {
                logger.debug( "TRACKING {} in {} (KEY: {})", path, repo, tk );
                eventMetadata.set( FoloConstants.TRACKING_KEY, tk );

                eventMetadata.set( FoloConstants.ACCESS_CHANNEL, AccessChannel.GENERIC_PROXY );
            }
            else
            {
                logger.debug( "NOT TRACKING: {} in {}", path, repo );
            }
        }
        else
        {
            logger.debug( "NOT TRACKING non-body request: {} in {}", path, repo );
        }

        eventMetadata.setPackageType( PKG_TYPE_GENERIC_HTTP );

        return eventMetadata;
    }

    private RemoteRepository getRepository( final URL url )
                    throws IndyDataException
    {
        int port = url.getPort();
        if ( port < 1 )
        {
            port = url.getDefaultPort();
        }

        String name = PROXY_REPO_PREFIX + url.getHost().replace( '.', '-' ) + '_' + port;

        final String baseUrl = String.format( "%s://%s:%s/", url.getProtocol(), url.getHost(), port );

        ArtifactStoreQuery<RemoteRepository> query =
                storeManager.query().packageType( GENERIC_PKG_KEY ).storeType( RemoteRepository.class );

        RemoteRepository remote = query.getRemoteRepositoryByUrl( baseUrl );
        if ( remote == null )
        {
            logger.debug( "Looking for remote repo with name: {}", name );
            remote = query.getByName( name );
            // if repo with this name already exists, it has a different url, so we need to use a different name
            int i = 1;
            while ( remote != null )
            {
                name = PROXY_REPO_PREFIX + url.getHost().replace( '.', '-' ) + "_" + i++;
                logger.debug( "Looking for remote repo with name: {}", name );
                remote = query.getByName( name );
            }
        }

        if ( remote == null )
        {
            logger.debug( "Creating remote repository for HTTProx request: {}", url );

            final UrlInfo info = new UrlInfo( url.toExternalForm() );

            if ( repoCreator == null )
            {
                throw new IndyDataException(
                        "No valid instance of ProxyRepositoryCreator. Cannot auto-create remote proxy to: '{}'",
                        baseUrl );
            }

            final UserPass up = UserPass.parse( ApplicationHeader.authorization, httpRequest, url.getAuthority() );

            remote = repoCreator.create( name, baseUrl, info, up, LoggerFactory.getLogger( repoCreator.getClass() ) );
            remote.setMetadata( ArtifactStore.METADATA_ORIGIN, ProxyAcceptHandler.HTTPROX_ORIGIN );

            final ChangeSummary changeSummary =
                    new ChangeSummary( ChangeSummary.SYSTEM_USER, "Creating HTTProx proxy for: " + info.getUrl() );

            storeManager.storeArtifactStore( remote, changeSummary, false, true, new EventMetadata() );
        }

        return remote;
    }

    public void setError( final Throwable error )
    {
        this.error = error;
    }

    public void setHttpRequest( final HttpRequest request )
    {
        this.httpRequest = request;
    }
}
