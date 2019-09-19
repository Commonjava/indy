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

import static java.lang.Thread.sleep;

public class SlowInputStream
    extends InputStream
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final byte[] data;

    private long perByteTransferTime;

    private int idx = 0;

    public SlowInputStream( final byte[] data, long transferTime )
    {
        this.data = data;
        this.perByteTransferTime = transferTime / data.length;
    }
    
    @Override
    public synchronized int read()
        throws IOException
    {
        if ( idx >= data.length )
        {
            logger.debug( "out of data: {}\nSent:\n\n{}\n\n", idx, new String(data) );

            return -1;
        }

        logger.debug( "read()" );
        try
        {
            if ( perByteTransferTime > 0 )
            {
                sleep( perByteTransferTime );
            }
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
//        Logger logger = LoggerFactory.getLogger( getClass() );
//        logger.debug( "Returning: {}", (Integer.toHexString( result )) );

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