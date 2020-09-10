/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
import org.apache.commons.io.output.CountingOutputStream;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.io.AbstractTransferDecorator;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.spi.metrics.TimingProvider;
import org.commonjava.o11yphant.metrics.api.Meter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class IOLatencyDecorator
        extends AbstractTransferDecorator
{
    private Function<String, TimingProvider> timerProvider;

    private Function<String, Meter> meterProvider;

    private BiConsumer<String, Double> cumulativeTimer;

    public IOLatencyDecorator( final Function<String, TimingProvider> timerProvider,
                               final Function<String, Meter> meterProvider,
                               final BiConsumer<String, Double> cumulativeTimer )
    {
        this.timerProvider = timerProvider;
        this.meterProvider = meterProvider;
        this.cumulativeTimer = cumulativeTimer;
    }

    @Override
    public InputStream decorateRead( final InputStream stream, final Transfer transfer, final EventMetadata metadata )
            throws IOException
    {
        return new TimingInputStream( new CountingInputStream( stream ), timerProvider, meterProvider, cumulativeTimer );
    }

    @Override
    public OutputStream decorateWrite( final OutputStream stream, final Transfer transfer, final TransferOperation op,
                                       final EventMetadata metadata )
            throws IOException
    {
        if ( op == TransferOperation.UPLOAD )
        {
            return new TimingOutputStream( new CountingOutputStream( stream ), timerProvider, meterProvider, cumulativeTimer );
        }

        return super.decorateWrite( stream, transfer, op, metadata );
    }
}
