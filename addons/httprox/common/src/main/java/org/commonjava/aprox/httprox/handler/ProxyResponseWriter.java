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
package org.commonjava.aprox.httprox.handler;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.core.ctl.ContentController;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.folo.ctl.FoloConstants;
import org.commonjava.aprox.folo.model.TrackingKey;
import org.commonjava.aprox.httprox.conf.HttproxConfig;
import org.commonjava.aprox.httprox.conf.TrackingType;
import org.commonjava.aprox.httprox.keycloak.KeycloakProxyAuthenticator;
import org.commonjava.aprox.httprox.util.HttpConduitWrapper;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.subsys.http.HttpWrapper;
import org.commonjava.aprox.subsys.http.util.UserPass;
import org.commonjava.aprox.util.ApplicationHeader;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.aprox.util.UrlInfo;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.conduits.ConduitStreamSinkChannel;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.commonjava.aprox.httprox.util.HttpProxyConstants.ALLOW_HEADER_VALUE;
import static org.commonjava.aprox.httprox.util.HttpProxyConstants.GET_METHOD;
import static org.commonjava.aprox.httprox.util.HttpProxyConstants.HEAD_METHOD;
import static org.commonjava.aprox.httprox.util.HttpProxyConstants.OPTIONS_METHOD;
import static org.commonjava.aprox.httprox.util.HttpProxyConstants.PROXY_AUTHENTICATE_FORMAT;
import static org.commonjava.aprox.httprox.util.HttpProxyConstants.PROXY_REPO_PREFIX;

public final class ProxyResponseWriter
                implements ChannelListener<ConduitStreamSinkChannel>
{

    private static final String TRACKED_USER_SUFFIX = "+tracking";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private List<String> headerLines;

    private Throwable error;

    private final HttproxConfig config;

    private final ContentController contentController;

    private final StoreDataManager storeManager;

    private KeycloakProxyAuthenticator proxyAuthenticator;

    private boolean transferred;

    public ProxyResponseWriter( final HttproxConfig config, final StoreDataManager storeManager,
                                final ContentController contentController,
                                KeycloakProxyAuthenticator proxyAuthenticator )
    {
        this.config = config;
        this.contentController = contentController;
        this.storeManager = storeManager;
        this.proxyAuthenticator = proxyAuthenticator;
    }

    @Override
    public void handleEvent( final ConduitStreamSinkChannel channel )
    {
        HttpConduitWrapper http = new HttpConduitWrapper( channel, headerLines, contentController );
        if ( headerLines.size() < 1 )
        {
            try
            {
                http.writeStatus( ApplicationStatus.BAD_REQUEST );
            }
            catch ( final IOException e )
            {
                logger.error( "Failed to write BAD REQUEST for missing HTTP first-line to response channel.", e );
            }

            return;
        }

        // TODO: Can we handle this?
        final String oldThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName( headerLines.get( 0 ) );
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
                                UserPass.parse( ApplicationHeader.proxy_authorization, headerLines, null );
                if ( ( config.isSecured() || TrackingType.ALWAYS == config.getTrackingType() )
                                && proxyUserPass == null )
                {
                    http.writeStatus( ApplicationStatus.PROXY_AUTHENTICATION_REQUIRED );
                    http.writeHeader( ApplicationHeader.proxy_authenticate,
                                      String.format( PROXY_AUTHENTICATE_FORMAT, config.getProxyRealm() ) );
                }
                else
                {
                    final String firstLine = headerLines.get( 0 );
                    logger.debug( "Got first line:\n  '{}'", firstLine );
                    final String[] parts = headerLines.get( 0 ).split( " " );

                    final String method = parts[0].toUpperCase();

                    switch ( method )
                    {
                        case GET_METHOD:
                        case HEAD_METHOD:
                        {
                            if ( proxyUserPass != null )
                            {
                                if (!proxyAuthenticator.authenticate( proxyUserPass, http ))
                                {
                                    break;
                                }
                            }

                            final URL url = new URL( parts[1] );
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

    private void handleError( Throwable error, HttpWrapper http )
    {
        logger.error( "HTTProx request failed: " + error.getMessage(), error );
        try
        {
            if ( http.isOpen() )
            {
                if ( error instanceof AproxWorkflowException )
                {
                    http.writeStatus( ApplicationStatus.getStatus( ( (AproxWorkflowException) error ).getStatus() ) );
                }
                else
                {
                    http.writeStatus( ApplicationStatus.SERVER_ERROR );
                }

                http.writeError( error );

                logger.info( "Response error complete." );
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
                    throws IOException, AproxWorkflowException
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
        catch ( final AproxWorkflowException e )
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

    private EventMetadata createEventMetadata( boolean writeBody, UserPass proxyUserPass, String path,
                                               RemoteRepository repo )
                    throws AproxWorkflowException
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
                        throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
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
                            tk = new TrackingKey( user );
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
            }
            else
            {
                logger.info( "NOT TRACKING: {} in {}", path, repo );
            }
        }
        else
        {
            logger.debug( "NOT TRACKING non-body request: {} in {}", path, repo );
        }

        return eventMetadata;
    }

    private RemoteRepository getRepository( final URL url )
                    throws AproxDataException
    {

        final String name = PROXY_REPO_PREFIX + url.getHost().replace( '.', '-' );

        final String baseUrl = String.format( "%s://%s:%s/", url.getProtocol(), url.getHost(), url.getPort() );

        RemoteRepository remote = storeManager.findRemoteRepository( baseUrl );
        if ( remote == null )
        {
            logger.debug( "Looking for remote repo with name: {}", name );
            remote = storeManager.getRemoteRepository( name );
        }

        if ( remote == null )
        {
            logger.debug( "Creating remote repository for HTTProx request: {}", url );

            final UrlInfo info = new UrlInfo( url.toExternalForm() );

            remote = new RemoteRepository( name, baseUrl );
            remote.setDescription( "HTTProx proxy based on: " + info.getUrl() );

            final UserPass up = UserPass.parse( ApplicationHeader.authorization, headerLines, url.getAuthority() );
            if ( up != null )
            {
                remote.setUser( up.getUser() );
                remote.setPassword( up.getPassword() );
            }

            storeManager.storeArtifactStore( remote, new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                        "Creating HTTProx proxy for: "
                                                                                + info.getUrl() ), new EventMetadata() );
        }

        return remote;
    }

    public ProxyResponseWriter setHead( final List<String> headerLines )
    {
        this.headerLines = headerLines;
        return this;
    }

    public void setError( final Throwable error )
    {
        this.error = error;
    }

}