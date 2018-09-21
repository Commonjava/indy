package org.commonjava.indy.httprox.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.conduits.ConduitStreamSinkChannel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ruhan on 9/6/18.
 */
public class ProxySSLTunnel implements Runnable
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private volatile Selector selector; // selecting READ events for target channel

    private static final long SELECTOR_TIMEOUT = 60 * 1000; // 60 seconds

    private static final int DEFAULT_READ_BUF_SIZE = 1024 * 32; // 32 K

    private final ConduitStreamSinkChannel sinkChannel;

    private final SocketChannel socketChannel;

    private volatile boolean closed;

    private final ExecutorService service;

    public ProxySSLTunnel( ConduitStreamSinkChannel sinkChannel, SocketChannel socketChannel )
    {
        this.sinkChannel = sinkChannel;
        this.socketChannel = socketChannel;
        this.service = Executors.newSingleThreadExecutor();
    }

    @Override
    public void run()
    {
        try
        {
            pipeTargetToSinkChannel( sinkChannel, socketChannel );
        }
        catch ( Exception e )
        {
            logger.error( "Pipe to sink channel failed", e );
        }
    }

    private void pipeTargetToSinkChannel( ConduitStreamSinkChannel sinkChannel, SocketChannel targetChannel )
                    throws IOException
    {
        logger.trace( "Start target to sink channel pipe" );
        selector = Selector.open();

        targetChannel.configureBlocking( false );
        targetChannel.register( selector, SelectionKey.OP_READ );

        breakPipe:
        while ( true )
        {
            if ( closed )
            {
                break;
            }

            logger.trace( "Select on target channel" );
            int readyChannels = selector.select( SELECTOR_TIMEOUT );

            logger.trace( "Select returns, {} ready channels", readyChannels );
            if ( readyChannels == 0 || !selector.isOpen() )
            {
                logger.trace( "No ready channel or selector closed, break" );
                break;
            }

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();
            while ( iterator.hasNext() )
            {
                SelectionKey key = iterator.next();
                if ( key.isReadable() )
                {
                    logger.trace( "Read from target channel" );
                    byte[] bytes = doRead( (SocketChannel) key.channel() );

                    logger.trace( "Read done, write to sink channel, bytes: {}", bytes.length );
                    if ( bytes.length > 0 && sinkChannel.isOpen() )
                    {
                        sinkChannel.write( ByteBuffer.wrap( bytes ) );
                    }
                    else
                    {
                        // read 0 or -1 means the other side have closed the socket
                        logger.debug( "Peer closed socket" );
                        break breakPipe;
                    }
                }
                iterator.remove();
            }
        }
    }

    private byte[] doRead( SocketChannel channel ) throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        ByteBuffer buf = ByteBuffer.allocate( DEFAULT_READ_BUF_SIZE );
        int read = -1;
        while ( ( read = channel.read( buf ) ) > 0 )
        {
            buf.flip();
            final byte[] bbuf = new byte[buf.limit()];
            buf.get( bbuf );
            bos.write( bbuf );
        }
        return bos.toByteArray();
    }

    public void write( byte[] bytes ) throws IOException
    {
        socketChannel.write( ByteBuffer.wrap( bytes ) );
    }

    public void close()
    {
        this.closed = true;
        try
        {
            selector.close(); // wake it up to complete the tunnel
            socketChannel.close();
        }
        catch ( IOException e )
        {
            logger.error( "Close tunnel selector failed", e );
        }
        service.shutdown();
    }
}
