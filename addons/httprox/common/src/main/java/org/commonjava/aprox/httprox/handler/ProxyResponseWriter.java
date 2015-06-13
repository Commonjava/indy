package org.commonjava.aprox.httprox.handler;

import static org.commonjava.aprox.httprox.util.HttpProxyConstants.ALLOW_HEADER_VALUE;
import static org.commonjava.aprox.httprox.util.HttpProxyConstants.GET_METHOD;
import static org.commonjava.aprox.httprox.util.HttpProxyConstants.HEAD_METHOD;
import static org.commonjava.aprox.httprox.util.HttpProxyConstants.OPTIONS_METHOD;
import static org.commonjava.aprox.httprox.util.HttpProxyConstants.PROXY_AUTHENTICATE_FORMAT;
import static org.commonjava.aprox.httprox.util.HttpProxyConstants.PROXY_REPO_PREFIX;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.core.ctl.ContentController;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.folo.ctl.FoloConstants;
import org.commonjava.aprox.folo.model.TrackingKey;
import org.commonjava.aprox.httprox.conf.HttproxConfig;
import org.commonjava.aprox.httprox.util.UserPass;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.util.HttpUtils;
import org.commonjava.aprox.util.ApplicationHeader;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.aprox.util.UrlInfo;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.transport.htcli.model.HttpExchangeMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.channels.Channels;
import org.xnio.conduits.ConduitStreamSinkChannel;

public final class ProxyResponseWriter
    implements ChannelListener<ConduitStreamSinkChannel>
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private List<String> headerLines;

    private Throwable error;

    private final HttproxConfig config;

    private final ContentController contentController;

    private final StoreDataManager storeManager;

    private boolean transferred;

    public ProxyResponseWriter( final HttproxConfig config, final StoreDataManager storeManager,
                                final ContentController contentController )
    {
        this.config = config;
        this.contentController = contentController;
        this.storeManager = storeManager;
    }

    @Override
    public void handleEvent( final ConduitStreamSinkChannel channel )
    {
        logger.info( "\n\n\n>>>>>>> Handle write\n\n\n\n\n" );
        if ( error == null )
        {
            try
            {
                final UserPass proxyUserPass =
                    UserPass.parse( ApplicationHeader.proxy_authorization, headerLines, null );
                if ( proxyUserPass == null )
                {
                    writeStatus( channel, ApplicationStatus.PROXY_AUTHENTICATION_REQUIRED );
                    writeHeader( channel, ApplicationHeader.proxy_authenticate,
                                 String.format( PROXY_AUTHENTICATE_FORMAT, config.getProxyRealm() ) );
                }
                else
                {
                    final String firstLine = headerLines.get( 0 );
                    logger.debug( "Got first line:\n  '{}'", firstLine );
                    final String[] parts = headerLines.get( 0 )
                                                      .split( " " );

                    final String method = parts[0].toUpperCase();

                    switch ( method )
                    {
                        case GET_METHOD:
                        case HEAD_METHOD:
                        {
                            final URL url = new URL( parts[1] );
                            final RemoteRepository repo = getRepository( url );
                            transfer( channel, repo, url.getPath(), GET_METHOD.equals( method ), proxyUserPass );

                            break;
                        }
                        case OPTIONS_METHOD:
                        {
                            writeStatus( channel, ApplicationStatus.OK );
                            writeHeader( channel, ApplicationHeader.allow, ALLOW_HEADER_VALUE );
                            break;
                        }
                        default:
                        {
                            writeStatus( channel, ApplicationStatus.METHOD_NOT_ALLOWED );
                        }
                    }
                }

                logger.info( "Response complete." );
            }
            catch ( IOException | AproxWorkflowException | AproxDataException e )
            {
                error = e;
            }
        }

        if ( error != null )
        {
            logger.error( "HTTProx request failed: " + error.getMessage(), error );
            try
            {
                if ( channel != null && channel.isOpen() )
                {
                    if ( error instanceof AproxWorkflowException )
                    {
                        writeStatus( channel,
                                     ApplicationStatus.getStatus( ( (AproxWorkflowException) error ).getStatus() ) );
                    }
                    else
                    {
                        writeStatus( channel, ApplicationStatus.SERVER_ERROR );
                    }

                    writeError( channel, error );

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

        try
        {
            channel.shutdownWrites();
            Channels.flushBlocking( channel );
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to flush/shutdown response.", e );
        }

        //        IOUtils.closeQuietly( channel );
    }

    private void transfer( final ConduitStreamSinkChannel sinkChannel, final RemoteRepository repo, final String path,
                           final boolean writeBody, final UserPass proxyUserPass )
        throws IOException, AproxWorkflowException
    {
        if ( transferred )
        {
            return;
        }

        transferred = true;
        if ( sinkChannel == null || !sinkChannel.isOpen() )
        {
            throw new IOException( "Sink channel already closed (or null)!" );
        }

        final EventMetadata eventMetadata = new EventMetadata();
        if ( writeBody )
        {
            eventMetadata.set( FoloConstants.TRACKING_KEY, new TrackingKey( proxyUserPass.getUser() ) );
        }

        Transfer txfr = null;
        try
        {
            txfr = contentController.get( repo.getKey(), path, eventMetadata );
        }
        catch ( final AproxWorkflowException e )
        {
            // block TransferException to allow handling below.
            if ( !( e.getCause() instanceof TransferException ) )
            {
                throw e;
            }
        }

        if ( txfr != null && txfr.exists() )
        {
            logger.debug( "Valid transfer found." );
            final ReadableByteChannel channel = null;
            InputStream stream = null;
            try
            {
                writeStatus( sinkChannel, ApplicationStatus.OK );
                writeHeader( sinkChannel, ApplicationHeader.content_length, Long.toString( txfr.length() ) );
                writeHeader( sinkChannel, ApplicationHeader.content_type, contentController.getContentType( path ) );
                writeHeader( sinkChannel, ApplicationHeader.last_modified,
                             HttpUtils.formatDateHeader( txfr.lastModified() ) );

                if ( writeBody )
                {
                    sinkChannel.write( ByteBuffer.wrap( "\r\n".getBytes() ) );

                    stream = txfr.openInputStream();
                    final byte[] bytes = new byte[4096];
                    int read = -1;
                    while ( ( read = stream.read( bytes ) ) > -1 )
                    {
                        final ByteBuffer buf = ByteBuffer.wrap( bytes, 0, read );
                        sinkChannel.write( buf );
                    }
                    //                    IoUtils.transfer( channel, txfr.length(), ByteBuffer.allocate( 4096 ), sinkChannel );
                }
            }
            finally
            {
                IOUtils.closeQuietly( channel );
                IOUtils.closeQuietly( stream );
            }
        }
        else
        {
            logger.debug( "No transfer found." );
            final HttpExchangeMetadata metadata = contentController.getHttpMetadata( repo.getKey(), path );
            if ( metadata == null )
            {
                logger.debug( "No transfer metadata." );
                writeStatus( sinkChannel, ApplicationStatus.NOT_FOUND );
            }
            else
            {
                logger.debug( "Writing metadata from http exchange with upstream." );
                if ( metadata.getResponseStatusCode() == 500 )
                {
                    logger.debug( "Translating 500 error upstream into 502" );
                    writeStatus( sinkChannel, 502, "Bad Gateway" );
                }
                else
                {
                    logger.debug( "Passing through upstream status: " + metadata.getResponseStatusCode() );
                    writeStatus( sinkChannel, metadata.getResponseStatusCode(), metadata.getResponseStatusMessage() );
                }

                writeHeader( sinkChannel, ApplicationHeader.content_type, contentController.getContentType( path ) );
                for ( final Map.Entry<String, List<String>> headerSet : metadata.getResponseHeaders()
                                                                                .entrySet() )
                {
                    final String key = headerSet.getKey();
                    if ( ApplicationHeader.content_type.upperKey()
                                                       .equals( key ) )
                    {
                        continue;
                    }

                    for ( final String value : headerSet.getValue() )
                    {
                        writeHeader( sinkChannel, headerSet.getKey(), value );
                    }
                }
            }
        }
    }

    private RemoteRepository getRepository( final URL url )
        throws AproxDataException
    {

        final String name = PROXY_REPO_PREFIX + url.getHost()
                                                   .replace( '.', '-' );

        RemoteRepository remote = storeManager.findRemoteRepository( url.toExternalForm() );
        if ( remote == null )
        {
            remote = storeManager.getRemoteRepository( name );
        }

        if ( remote == null )
        {
            final UrlInfo info = new UrlInfo( url.toExternalForm() );

            final String baseUrl = String.format( "%s://%s:%s/", url.getProtocol(), url.getHost(), url.getPort() );
            remote = new RemoteRepository( name, baseUrl );
            remote.setDescription( "HTTProx proxy based on: " + info.getUrl() );

            final UserPass up = UserPass.parse( ApplicationHeader.authorization, headerLines, url.getAuthority() );
            if ( up != null )
            {
                remote.setUser( up.getUser() );
                remote.setPassword( up.getPassword() );
            }

            storeManager.storeArtifactStore( remote,
                                             new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                "Creating HTTProx proxy for: " + info.getUrl() ),
                                             new EventMetadata() );
        }

        return remote;
    }

    private void writeError( final ConduitStreamSinkChannel sinkChannel, final Throwable e )
        throws IOException
    {
        final String message =
            String.format( "%s:\n  %s", e.getMessage(), StringUtils.join( e.getStackTrace(), "\n  " ) );

        sinkChannel.write( ByteBuffer.wrap( message.getBytes() ) );
    }

    private void writeHeader( final ConduitStreamSinkChannel sinkChannel, final ApplicationHeader header,
                              final String value )
        throws IOException
    {
        final ByteBuffer b = ByteBuffer.wrap( String.format( "%s: %s\r\n", header.key(), value )
                                                    .getBytes() );
        sinkChannel.write( b );
    }

    private void writeHeader( final ConduitStreamSinkChannel sinkChannel, final String header, final String value )
        throws IOException
    {
        final ByteBuffer b = ByteBuffer.wrap( String.format( "%s: %s\r\n", header, value )
                                                    .getBytes() );
        sinkChannel.write( b );
    }

    private void writeStatus( final ConduitStreamSinkChannel sinkChannel, final ApplicationStatus status )
        throws IOException
    {
        final ByteBuffer b = ByteBuffer.wrap( String.format( "HTTP/1.1 %d %s\r\n", status.code(), status.message() )
                                                    .getBytes() );
        sinkChannel.write( b );
    }

    private void writeStatus( final ConduitStreamSinkChannel sinkChannel, final int code, final String message )
        throws IOException
    {
        final ByteBuffer b = ByteBuffer.wrap( String.format( "HTTP/1.1 %d %s\r\n", code, message )
                                                    .getBytes() );
        sinkChannel.write( b );
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