/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.ftest.core.fixture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class ReluctantInputStream
    extends InputStream
{
    private final byte[] data;

    private int idx = 0;

    public ReluctantInputStream( final byte[] data )
    {
        this.data = data;
    }
    
    public boolean hasNext()
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "{} remaining", data.length - idx );
        return idx < data.length;
    }

    public synchronized void next()
    {
        notifyAll();
    }

    @Override
    public synchronized int read()
        throws IOException
    {
        if ( idx >= data.length )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.debug( "out of data" );

            return -1;
        }

        // don't send anything until pinged.
        try
        {
            wait();
        }
        catch ( final InterruptedException e )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.debug( "interrupted" );

            idx = data.length;
            return -1;
        }

        int result = data[idx];

        idx++;
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "Returning: {}", (Integer.toHexString( result )) );

        return result;
    }

    @Override
    public void close()
        throws IOException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "closed" );

        idx = data.length;
        super.close();
    }

}