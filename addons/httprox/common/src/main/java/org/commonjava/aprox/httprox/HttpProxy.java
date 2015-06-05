package org.commonjava.aprox.httprox;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.action.AproxLifecycleException;
import org.commonjava.aprox.action.ShutdownAction;
import org.commonjava.aprox.action.StartupAction;
import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.boot.BootOptions;
import org.commonjava.aprox.core.ctl.ContentController;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.httprox.conf.HttproxConfig;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.util.HttpUtils;
import org.commonjava.aprox.util.ApplicationHeader;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.aprox.util.UrlInfo;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.StreamConnection;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;
import org.xnio.channels.Channels;
import org.xnio.conduits.ConduitStreamSinkChannel;
import org.xnio.conduits.ConduitStreamSourceChannel;

@ApplicationScoped
public class HttpProxy
    implements ChannelListener<AcceptingChannel<StreamConnection>>, StartupAction, ShutdownAction
{

    private static final String PROXY_REPO_PREFIX = "httprox_";

    private static final String GET_METHOD = "GET";

    private static final String HEAD_METHOD = "HEAD";

    private static final String OPTIONS_METHOD = "OPTIONS";

    private static final Set<String> ALLOWED_METHODS =
        Collections.unmodifiableSet( new HashSet<>( Arrays.asList( GET_METHOD, HEAD_METHOD, OPTIONS_METHOD ) ) );

    private static final String ALLOW_HEADER_VALUE = StringUtils.join( ALLOWED_METHODS, "," );

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private HttproxConfig config;

    @Inject
    private BootOptions bootOptions;

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private ContentController contentController;

    private AcceptingChannel<StreamConnection> server;

    protected HttpProxy()
    {
    }

    public HttpProxy( final HttproxConfig config, final BootOptions bootOptions, final StoreDataManager storeManager,
                      final ContentController contentController )
    {
        this.config = config;
        this.bootOptions = bootOptions;
        this.storeManager = storeManager;
        this.contentController = contentController;
    }

    @Override
    public void start()
        throws AproxLifecycleException
    {
        XnioWorker worker;
        try
        {
            worker = Xnio.getInstance()
                         .createWorker( OptionMap.EMPTY );

            final InetSocketAddress addr = new InetSocketAddress( bootOptions.getBind(), config.getPort() );
            server = worker.createStreamConnectionServer( addr, this, OptionMap.EMPTY );

            server.resumeAccepts();
            logger.info( "HTTProxy listening on: {}", addr );
        }
        catch ( IllegalArgumentException | IOException e )
        {
            throw new AproxLifecycleException( "Failed to start HTTProx general content proxy: %s", e, e.getMessage() );
        }
    }

    @Override
    public void stop()
    {
        try
        {
            logger.info( "stopping server" );
            server.suspendAccepts();
            server.close();
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to stop: " + e.getMessage(), e );
        }
    }

    @Override
    public void handleEvent( final AcceptingChannel<StreamConnection> channel )
    {
        try
        {
            StreamConnection accepted;
            while ( ( accepted = channel.accept() ) != null )
            {
                logger.debug( "accepted {}", accepted.getPeerAddress() );

                final ConduitStreamSourceChannel source = accepted.getSourceChannel();
                final Reader reader = new Reader( accepted.getSinkChannel() );

                logger.debug( "Setting reader: {}", reader );
                source.getReadSetter()
                      .set( reader );

                source.resumeReads();
            }

            channel.resumeAccepts();
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to accept httprox requests: " + e.getMessage(), e );
        }
    }

    public final class Reader
        implements ChannelListener<ConduitStreamSourceChannel>
    {
        private final ConduitStreamSinkChannel sinkChannel;

        private StringBuilder currentLine = new StringBuilder();

        private final List<String> headerLines = new ArrayList<>();

        private char lastChar = 0;

        private boolean headDone = false;

        public Reader( final ConduitStreamSinkChannel sinkChannel )
        {
            this.sinkChannel = sinkChannel;
        }

        // TODO: May need to tune this to preserve request body.
        // TODO: NONE of the request headers (except authorization) are passed through!
        @Override
        public void handleEvent( final ConduitStreamSourceChannel channel )
        {
            final ByteBuffer buf = ByteBuffer.allocate( 256 );
            try
            {
                logger.debug( "Starting read: {}", channel );
                int read = 0;
                readLoop: while ( ( read = channel.read( buf ) ) > 0 )
                {
                    logger.debug( "Read {} bytes", read );

                    buf.flip();
                    final byte[] bbuf = new byte[buf.limit()];
                    buf.get( bbuf );

                    final String part = new String( bbuf );
                    logger.debug( part );
                    for ( final char c : part.toCharArray() )
                    {
                        if ( !headDone )
                        {
                            if ( lastChar == '\n' && c == '\r' && !headerLines.isEmpty() )
                            {
                                // end of request head
                                logger.debug( "Detected end of request head. Breaking read loop." );
                                headDone = true;
                                break readLoop;
                            }
                            else if ( c == '\n' )
                            {
                                logger.debug( "Got line: '{}'", currentLine );
                                headerLines.add( currentLine.toString() );
                                currentLine.setLength( 0 );
                                currentLine = new StringBuilder();
                            }
                            else if ( c != '\r' )
                            {
                                currentLine.append( c );
                            }

                            lastChar = c;
                        }
                    }
                }

                if ( read < 0 || headDone )
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
                            transfer( repo, url.getPath(), GET_METHOD.equals( method ) );

                            break;
                        }
                        case OPTIONS_METHOD:
                        {
                            writeStatus( ApplicationStatus.OK );
                            writeHeader( ApplicationHeader.allow, ALLOW_HEADER_VALUE );
                            break;
                        }
                        default:
                        {
                            writeStatus( ApplicationStatus.METHOD_NOT_ALLOWED );
                        }
                    }

                    Channels.flushBlocking( sinkChannel );
                    channel.close();
                    sinkChannel.close();
                }
                else
                {
                    logger.debug( "request head not finished. Pausing until more reads are available." );
                    channel.resumeReads();
                }
            }
            catch ( final IOException | AproxDataException e )
            {
                logger.error( "HTTProx request failed: " + e.getMessage(), e );
                try
                {
                    if ( channel != null && channel.isOpen() )
                    {
                        channel.close();
                    }

                    if ( sinkChannel != null && sinkChannel.isOpen() )
                    {
                        writeStatus( ApplicationStatus.SERVER_ERROR );
                        writeError( e );

                        Channels.flushBlocking( sinkChannel );
                        sinkChannel.close();
                    }
                }
                catch ( final IOException closeException )
                {
                    logger.error( "Failed to close httprox request: " + e.getMessage(), e );
                }
            }
            catch ( final AproxWorkflowException e )
            {
                logger.error( "HTTProx request failed: " + e.getMessage(), e );
                try
                {
                    if ( channel != null && channel.isOpen() )
                    {
                        channel.close();
                    }

                    if ( sinkChannel != null && sinkChannel.isOpen() )
                    {
                        writeStatus( ApplicationStatus.getStatus( e.getStatus() ) );
                        writeError( e );

                        Channels.flushBlocking( sinkChannel );
                        sinkChannel.close();
                    }
                }
                catch ( final IOException closeException )
                {
                    logger.error( "Failed to close httprox request: " + e.getMessage(), e );
                }
            }

        }

        private void transfer( final RemoteRepository repo, final String path, final boolean writeBody )
            throws AproxWorkflowException, IOException
        {
            if ( sinkChannel == null || !sinkChannel.isOpen() )
            {
                throw new IOException( "Sink channel already closed (or null)!" );
            }

            final Transfer txfr = contentController.get( repo.getKey(), path );
            if ( txfr != null && txfr.exists() )
            {
                final ReadableByteChannel channel = null;
                InputStream stream = null;
                try
                {
                    stream = txfr.openInputStream();
                    java.nio.channels.Channels.newChannel( stream );

                    writeStatus( ApplicationStatus.OK );
                    writeHeader( ApplicationHeader.content_length, Long.toString( txfr.length() ) );
                    writeHeader( ApplicationHeader.content_type, contentController.getContentType( path ) );
                    writeHeader( ApplicationHeader.last_modified, HttpUtils.formatDateHeader( txfr.lastModified() ) );

                    if ( writeBody )
                    {
                        IoUtils.transfer( channel, txfr.length(), ByteBuffer.allocate( 4096 ), sinkChannel );
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
                writeStatus( ApplicationStatus.NOT_FOUND );
            }
        }

        private void writeError( final Exception e )
            throws IOException
        {
            final String message =
                String.format( "%s:\n  %s", e.getMessage(), StringUtils.join( e.getStackTrace(), "\n  " ) );

            sinkChannel.write( ByteBuffer.wrap( message.getBytes() ) );
        }

        private RemoteRepository getRepository( final URL url )
            throws AproxDataException
        {
            final String name = PROXY_REPO_PREFIX + url.getHost()
                                                       .replace( '.', '-' );

            RemoteRepository remote = storeManager.getRemoteRepository( name );
            if ( remote == null )
            {
                final UrlInfo info = new UrlInfo( url.toExternalForm() );

                final String baseUrl = String.format( "%s://%s:%s/", url.getProtocol(), url.getHost(), url.getPort() );
                remote = new RemoteRepository( name, baseUrl );
                remote.setDescription( "HTTProx proxy based on: " + info.getUrl() );

                String userpass = url.getAuthority();
                if ( userpass == null )
                {
                    for ( final String line : headerLines )
                    {
                        final String upperLine = line.toUpperCase();
                        if ( upperLine.startsWith( ApplicationHeader.authorization.upperKey() )
                            && upperLine.contains( "BASIC" ) )
                        {
                            final String[] authParts = line.split( " " );
                            if ( authParts.length > 2 )
                            {
                                userpass = new String( Base64.decodeBase64( authParts[2] ) );
                            }
                        }
                    }
                }

                if ( userpass != null )
                {
                    final String[] up = userpass.split( ":" );
                    if ( up.length > 0 )
                    {
                        remote.setUser( up[0] );
                    }
                    if ( up.length > 1 )
                    {
                        remote.setPassword( up[1] );
                    }
                }

                storeManager.storeArtifactStore( remote,
                                                 new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                    "Creating HTTProx proxy for: " + info.getUrl() ) );
            }

            return remote;
        }

        private void writeHeader( final ApplicationHeader header, final String value )
            throws IOException
        {
            final ByteBuffer b = ByteBuffer.wrap( String.format( "%s: %s\r\n", header.key(), value )
                                                        .getBytes() );
            sinkChannel.write( b );
        }

        private void writeStatus( final ApplicationStatus status )
            throws IOException
        {
            final ByteBuffer b = ByteBuffer.wrap( String.format( "%d %s\r\n", status.code(), status.message() )
                                                        .getBytes() );
            sinkChannel.write( b );
        }
    }

    @Override
    public String getId()
    {
        return "httproxy-listener";
    }

    @Override
    public int getStartupPriority()
    {
        return 1;
    }

    @Override
    public int getShutdownPriority()
    {
        return 99;
    }
}
