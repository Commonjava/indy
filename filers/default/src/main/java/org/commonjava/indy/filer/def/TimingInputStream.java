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

import org.apache.commons.io.input.CountingInputStream;
import org.commonjava.indy.util.RequestContextHelper;
import org.commonjava.maven.galley.spi.metrics.TimingProvider;
import org.commonjava.maven.galley.util.IdempotentCloseInputStream;

import java.io.IOException;
import java.util.function.Function;

public class TimingInputStream
        extends IdempotentCloseInputStream
{
    private static final String RAW_IO_READ = "io.raw.read";
    private static final String RAW_IO_READ_TIMER = RAW_IO_READ + ".timer";

    private Long nanos;

    private Function<String, TimingProvider> timerProvider;

    private TimingProvider timer;

    public TimingInputStream( final CountingInputStream stream, final Function<String, TimingProvider> timerProvider )
    {
        super( stream );
        this.timerProvider = timerProvider == null ? ( s ) -> null : timerProvider;
    }

    @Override
    public int read()
            throws IOException
    {
        initMetrics();
        return super.read();
    }

    @Override
    public int read( final byte[] b )
            throws IOException
    {
        initMetrics();
        return super.read( b );
    }

    @Override
    public int read( final byte[] b, final int off, final int len )
            throws IOException
    {
        initMetrics();
        return super.read( b, off, len );
    }

    @Override
    public void close()
            throws IOException
    {
        super.close();

        if ( nanos != null )
        {
            long elapsed = System.nanoTime() - nanos;
            RequestContextHelper.setContext( RequestContextHelper.RAW_IO_WRITE_NANOS, elapsed );

            if ( timer != null )
            {
                timer.stop();
            }
        }
    }

    private void initMetrics()
    {
        if ( nanos == null )
        {
            nanos = System.nanoTime();
            timer = timerProvider.apply( RAW_IO_READ_TIMER );
        }
    }
}
