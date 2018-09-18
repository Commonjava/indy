package org.commonjava.indy.httprox.handler;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.httprox.conf.HttproxConfig;
import org.commonjava.indy.httprox.util.CertificateAndKeys;
import org.commonjava.indy.subsys.http.util.HttpResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Thread.sleep;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
import static org.commonjava.indy.boot.PortFinder.findOpenPort;
import static org.commonjava.indy.httprox.util.CertUtils.createKeyStore;
import static org.commonjava.indy.httprox.util.CertUtils.createSignedCertificateAndKey;
import static org.commonjava.indy.httprox.util.CertUtils.getPrivateKey;
import static org.commonjava.indy.httprox.util.CertUtils.loadX509Certificate;

/**
 * We create server crt based on the host name and use a CA crt to sign it. We send the CA crt to client and they use
 * it when sending requests to MITM server.
 */
public class ProxyMITMSSLServer implements Runnable
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final String host;

    private final int port;

    private final HttproxConfig config;

    private volatile int serverPort;

    public ProxyMITMSSLServer( String host, int port, HttproxConfig config )
    {
        this.host = host;
        this.port = port;
        this.config = config;
    }

    @Override
    public void run()
    {
        try
        {
            execute();
        }
        catch ( Exception e )
        {
            logger.warn( "Execute failed", e );
        }
    }

    private volatile boolean started;

    private volatile ServerSocket sslServerSocket;

    private volatile Socket socket;

    private char[] keystorePassword = "password".toCharArray(); // keystore password can not be null

    private static Map<String, KeyStore> keystoreMap = new ConcurrentHashMap(); // cache keystore

    /**
     * Generate the keystore on-the-fly and initiate SSL socket factory.
     */
    private SSLServerSocketFactory getSSLServerSocketFactory( String host ) throws Exception
    {
        AtomicReference<Exception> err = new AtomicReference<>();
        KeyStore ks = keystoreMap.computeIfAbsent( host, (k) -> {
            try
            {
                return getKeyStore(k);
            }
            catch ( Exception e )
            {
                err.set( e );
            }
            return null;
        } );

        if ( ks == null )
        {
            throw err.get();
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance( KeyManagerFactory.getDefaultAlgorithm() );
        kmf.init( ks, keystorePassword );

        SSLContext sc = SSLContext.getInstance( "TLS" );
        sc.init( kmf.getKeyManagers(), null, null );
        return sc.getServerSocketFactory();
    }

    private KeyStore getKeyStore( String host ) throws Exception
    {
        PrivateKey caKey = getPrivateKey( config.getMITMCAKey() );
        X509Certificate caCert = loadX509Certificate( new File( config.getMITMCACert() ));

        String dn = "CN=#, O=Test Org".replace( "#", host );
        CertificateAndKeys certificateAndKeys = createSignedCertificateAndKey( dn, caCert, caKey, false );
        Certificate signedCertificate = certificateAndKeys.getCertificate();
        logger.debug( "Create signed cert:\n" + signedCertificate.toString() );

        KeyStore ks = createKeyStore();
        String alias = host;
        ks.setKeyEntry( alias, certificateAndKeys.getPrivateKey(), keystorePassword, new Certificate[] { signedCertificate, caCert } );
        return ks;
    }

    private void execute() throws Exception
    {
        SSLServerSocketFactory sslServerSocketFactory = getSSLServerSocketFactory( host );

        serverPort = findOpenPort( maxRetries );
        sslServerSocket = sslServerSocketFactory.createServerSocket( serverPort );

        logger.debug( "MITM server started, {}", sslServerSocket );
        started = true;

        socket = sslServerSocket.accept();

        logger.debug( "MITM server accepted" );
        BufferedReader in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );

        String path = null;
        StringBuilder sb = new StringBuilder();
        String line;
        while ( ( line = in.readLine() ) != null )
        {
            sb.append( line + "\n" );
            if ( line.startsWith( GET ) || line.startsWith( HEAD ) )
            {
                String[] toks = line.split("\\s+");
                path = toks[1];
            }
            else if ( line.isEmpty() )
            {
                logger.debug( "Get empty line and break" );
                break;
            }
        }

        logger.debug( "Request:\n{}", sb.toString() );

        if ( path != null )
        {
            String body = requestRemoteHTTPS( host, port, path );
            int len = body.getBytes().length;

            String resp = "HTTP/1.1 200 OK\r\n" + "Content-Type: text/html; charset=UTF-8\r\n" + "Content-Length: " + len + "\r\n"
                            + "Connection: close\r\n" + "\r\n" + body;

            BufferedWriter out = new BufferedWriter( new OutputStreamWriter( socket.getOutputStream() ) );
            out.write( resp );
            out.close();
        }
        else
        {
            logger.debug( "MITM server failed to get GET/HEAD request from client" );
        }
        in.close();
        socket.close();
        sslServerSocket.close();
        logger.debug( "MITM server closed" );
    }

    private String requestRemoteHTTPS( String host, int port, String path ) throws Exception
    {
        String protocol = "https";
        String auth = null;
        String query = null;
        String fragment = null;
        URI uri = new URI( protocol, auth, host, port, path, query, fragment);
        String remoteUrl = uri.toURL().toString();
        logger.debug( "Requesting remote URL: {}", remoteUrl );

        final HttpGet req = new HttpGet( remoteUrl );
        CloseableHttpClient client = HttpClients.createDefault();
        try
        {
            CloseableHttpResponse response = client.execute( req );

            StatusLine statusLine = response.getStatusLine();
            int status = statusLine.getStatusCode();
            if ( status == HttpStatus.SC_OK )
            {
                String ret = HttpResources.entityToString( response );
                return ret;
            }
            else
            {
                throw new IndyWorkflowException( status, "Request: %s failed: %s", remoteUrl, statusLine );
            }
        }
        finally
        {
            IOUtils.closeQuietly( client );
        }
    }

    private static final int maxRetries = 16;

    public SocketChannel get() throws InterruptedException, ExecutionException
    {
        for ( int i = 0; i < maxRetries; i++ )
        {
            logger.debug( "Try to get socket channel #{}", i + 1 );
            if ( started )
            {
                logger.debug( "Server started" );
                try
                {
                    return openSocketChannelToServer();
                }
                catch ( IOException e )
                {
                    throw new ExecutionException( "", e );
                }
            }
            else
            {
                logger.debug( "Server not started, wait..." );
                sleep( TimeUnit.SECONDS.toMillis( 1 ) );
            }
        }
        return null;
    }

    private SocketChannel openSocketChannelToServer() throws IOException
    {
        logger.debug( "Open socket channel to MITM server, localhost:{}", serverPort );
        InetSocketAddress target = new InetSocketAddress( "localhost", serverPort );
        SocketChannel socketChannel = SocketChannel.open( target );
        return socketChannel;
    }

    public void stop()
    {
        try
        {
            if ( !sslServerSocket.isClosed() )
            {
                sslServerSocket.close();
            }
            if ( socket != null && !socket.isClosed() )
            {
                socket.close();
            }
        }
        catch ( IOException e )
        {
            logger.debug( "Close MITM server, {}", e.toString() );
        }
    }
}
