/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.filer.def;

import org.apache.commons.io.output.CountingOutputStream;
import org.commonjava.indy.util.RequestContextHelper;
import org.commonjava.maven.galley.spi.metrics.TimingProvider;
import org.commonjava.maven.galley.util.IdempotentCloseOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Function;

public class TimingOutputStream
        extends IdempotentCloseOutputStream
{
    private static final String RAW_IO_WRITE = "io.raw.write";
    private static final String RAW_IO_WRITE_TIMER = RAW_IO_WRITE + ".timer";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private Long nanos;

    private Function<String, TimingProvider> timerProvider;

    private TimingProvider timer;

    public TimingOutputStream( final CountingOutputStream stream, Function<String, TimingProvider> timerProvider )
    {
        super( stream );
        this.timerProvider = timerProvider == null ? ( s ) -> null : timerProvider;
    }

    @Override
    public void write( final int b )
            throws IOException
    {
        initMetrics();
        super.write( b );
    }

    @Override
    public void write( final byte[] b )
            throws IOException
    {
        initMetrics();
        super.write( b );
    }

    @Override
    public void write( final byte[] b, final int off, final int len )
            throws IOException
    {
        initMetrics();
        super.write( b, off, len );
    }

    @Override
    public void close()
            throws IOException
    {
        super.close();

        if ( nanos != null )
        {
            long elapsed = System.nanoTime() - nanos;
            logger.trace( "Measured elapsed time (in nanos) for write: {}", elapsed );

            RequestContextHelper.setContext( RequestContextHelper.RAW_IO_WRITE_NANOS, elapsed );

            if ( timer != null )
            {
                logger.trace( "Stopping timer: {}", timer );
                timer.stop();
            }
        }

    }

    private void initMetrics()
    {
        if ( nanos == null )
        {
            nanos = System.nanoTime();
            timer = timerProvider.apply( RAW_IO_WRITE_TIMER );
            logger.trace( "At nanos: {}, initialized timer: {}", nanos, timer );
        }
    }

}
