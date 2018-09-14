package org.commonjava.indy.httprox.handler;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.model.core.dto.StoreListingDTO;
import org.commonjava.indy.subsys.http.IndyHttpException;
import org.commonjava.indy.subsys.http.util.HttpResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.commonjava.indy.boot.PortFinder.findOpenPort;

/**
 * We create a server.crt and use a ca.crt to sign it. The we send the ca.crt to client and they use it when sending
 * request to server.
 *
 * Question: can we create a server.crt on the fly with the requested domain name when user requests?
 * if so, we can start up a ssl server socket with the faked server.crt, and direct the traffic to it.
 *
 openssl genpkey -algorithm RSA -out ca.key
 openssl req -new -x509 -days 360 -key ca.key -subj "/CN=Test CA/O=Test Org" -out ca.crt
 openssl genpkey -algorithm RSA -out server.key
 openssl req -new -key server.key -subj "/CN=localhost/O=Test Org" -out server.csr
 openssl x509 -days 360 -req -in server.csr -CAcreateserial -CA ca.crt -CAkey ca.key -out server.crt

 openssl pkcs12 -export -in server.crt -inkey server.key -out server.p12

 Test:
 wget -dv https://localhost:8000/a.html --ca-certificate=/home/ruhan/ssl/ca.crt
 curl -vvv https://localhost:8000/a.html --cacert /home/ruhan/ssl/ca.crt
 */
public class ProxyMITMSSLServer implements Runnable
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final String host;

    private final int port;

    private volatile int serverPort;

    public ProxyMITMSSLServer( String host, int port )
    {
        this.host = host;
        this.port = port;
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

    private void execute() throws Exception
    {
        System.setProperty( "javax.net.ssl.keyStore", "/home/ruhan/ssl/oss-server.p12" );
        System.setProperty( "javax.net.ssl.keyStorePassword", "passwd" );

        SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

        serverPort = findOpenPort( maxRetries );

        sslServerSocket = sslServerSocketFactory.createServerSocket( serverPort );
        logger.debug( "SSL ServerSocket started, {}", sslServerSocket );
        started = true;

        socket = sslServerSocket.accept();

        logger.debug( "SSL ServerSocket accepted" );
        BufferedReader in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );

        String path = null;
        StringBuilder sb = new StringBuilder();
        String line;
        while ( ( line = in.readLine() ) != null )
        {
            sb.append( line + "\n" );
            if ( line.startsWith( "GET" ))
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

        //String body = "<html><body>Hello World!</body></html>"; // for test
        String body = requestRemoteHTTPS( host, port, path );
        int len = body.getBytes().length;

        String resp = "HTTP/1.1 200 OK\r\n"
                        + "Content-Type: text/html; charset=UTF-8\r\n"
                        + "Content-Length: " + len + "\r\n"
                        + "Connection: close\r\n" + "\r\n" + body;

        BufferedWriter out = new BufferedWriter( new OutputStreamWriter( socket.getOutputStream() ) );
        out.write( resp );
        out.close();
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

    private SocketChannel openSocketChannelToServer() throws IOException
    {
        logger.debug( "Open socket channel to MITM server, localhost:{}", serverPort );
        InetSocketAddress target = new InetSocketAddress( "localhost", serverPort );
        SocketChannel socketChannel = SocketChannel.open( target );
        return socketChannel;
    }

    public SocketChannel get() throws InterruptedException, ExecutionException
    {
        try
        {
            return get( 1, TimeUnit.SECONDS );
        }
        catch ( TimeoutException e )
        {
            throw new ExecutionException( "", e );
        }
    }

    private static final int maxRetries = 16;

    private SocketChannel get( long l, TimeUnit timeUnit ) throws InterruptedException, ExecutionException, TimeoutException
    {
        for( int i = 0 ; i < maxRetries; i++ )
        {
            logger.debug( "Try to get socket channel #{}", i+1 );
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
                Thread.sleep( timeUnit.toMillis( l ) );
            }
        }
        return null;
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
