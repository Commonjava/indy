/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.bind.vertx.util;

import java.io.IOException;
import java.io.InputStream;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.ReadStream;

public class VertXInputStream
    extends InputStream
{

    private final class DataHandler
        implements Handler<Buffer>
    {
        @Override
        public void handle( final Buffer event )
        {
            if ( done )
            {
                return;
            }

            synchronized ( stream )
            {
                while ( !done && buf != null )
                {
                    try
                    {
                        stream.wait( 500 );
                    }
                    catch ( final InterruptedException e )
                    {
                        Thread.currentThread()
                              .interrupt();
                        return;
                    }
                }

                buf = event;
                stream.pause();
            }
        }
    };

    private final class EndHandler
        implements Handler<Void>
    {
        @Override
        public void handle( final Void event )
        {
            synchronized ( stream )
            {
                done = true;
                stream.notifyAll();
            }
        }
    };

    private final ReadStream<?> stream;

    private final DataHandler dataHandler;

    private final EndHandler endHandler;

    private boolean done;

    private Buffer buf;

    private int index = 0;

    public VertXInputStream( final ReadStream<?> stream )
    {
        if ( stream == null )
        {
            throw new NullPointerException( "Cannot read from null stream!" );
        }

        this.stream = stream;
        dataHandler = new DataHandler();
        endHandler = new EndHandler();
        stream.dataHandler( dataHandler );
        stream.endHandler( endHandler );
    }

    @Override
    public int read()
        throws IOException
    {
        synchronized ( stream )
        {
            if ( buf == null || index >= buf.length() )
            {
                if ( done )
                {
                    return -1;
                }

                buf = null;
                stream.resume();
                stream.notifyAll();
            }

            while ( buf == null )
            {
                if ( done )
                {
                    return -1;
                }

                try
                {
                    stream.wait( 500 );
                }
                catch ( final InterruptedException e )
                {
                    Thread.currentThread()
                          .interrupt();
                    return -1;
                }
            }
        }

        return buf.getByte( index++ );
    }

}
