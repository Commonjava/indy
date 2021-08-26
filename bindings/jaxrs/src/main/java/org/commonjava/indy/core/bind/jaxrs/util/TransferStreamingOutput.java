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
package org.commonjava.indy.core.bind.jaxrs.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.commonjava.indy.subsys.metrics.conf.IndyMetricsConfig;
import org.commonjava.o11yphant.metrics.MetricsManager;
import org.commonjava.o11yphant.metrics.RequestContextHelper;
import org.commonjava.o11yphant.metrics.annotation.Measure;
import org.commonjava.o11yphant.metrics.api.Histogram;
import org.commonjava.o11yphant.trace.TraceManager;
import org.commonjava.o11yphant.trace.spi.CloseBlockingDecorator;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import static org.commonjava.indy.IndyContentConstants.NANOS_PER_SEC;
import static org.commonjava.o11yphant.metrics.MetricsConstants.METER;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.RAW_IO_WRITE_NANOS;
import static org.commonjava.o11yphant.metrics.util.NameUtils.getDefaultName;
import static org.commonjava.o11yphant.metrics.util.NameUtils.getName;
import static org.commonjava.o11yphant.trace.TraceManager.addFieldToActiveSpan;
import static org.commonjava.o11yphant.trace.TraceManager.getActiveSpan;

public class TransferStreamingOutput
    implements StreamingOutput
{

    private static final String TRANSFER_METRIC_NAME = "indy.transferred.content";

    private static final String WRITE_SPEED = TRANSFER_METRIC_NAME + ".write.kps";

    private static final String WRITE_SIZE = TRANSFER_METRIC_NAME + ".write.kb";

    private final Optional<SpanAdapter> rootSpan;

    private final InputStream stream;

    private final MetricsManager metricsManager;

    private final IndyMetricsConfig metricsConfig;

    private long start = -1;

    private CountingOutputStream cout;

    private double kbCount;

    private long writeSpeed;

    public TransferStreamingOutput( final InputStream stream, final MetricsManager metricsManager,
                                    final IndyMetricsConfig metricsConfig )
    {
        this.stream = stream;
        this.metricsManager = metricsManager;
        this.metricsConfig = metricsConfig;

        Optional<SpanAdapter> rs = getActiveSpan();
        if ( rs.isPresent() ){
            this.rootSpan = TraceManager.addCloseBlockingDecorator( rs, new TransferFieldInjector() );
        }
        else
        {
            this.rootSpan = Optional.empty();
        }
    }

    @Override
    @Measure
    public void write( final OutputStream out )
        throws IOException, WebApplicationException
    {
        start = System.nanoTime();
        try
        {
            cout = new CountingOutputStream( out );
            IOUtils.copy( stream, cout );

            kbCount = (double) cout.getByteCount() / 1024;

            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.trace( "Wrote: {} bytes", kbCount );

            long end = System.nanoTime();
            RequestContextHelper.setContext( RAW_IO_WRITE_NANOS, end - start );

            double elapsed = (end-start)/NANOS_PER_SEC;

            String rateName = getName( metricsConfig.getNodePrefix(), WRITE_SPEED,
                                       getDefaultName( TransferStreamingOutput.class, WRITE_SPEED ), METER );

            Histogram rateGram = metricsManager.getHistogram( rateName );
            writeSpeed = Math.round( kbCount / elapsed );
            logger.info( "measured speed: {} kb/s to metric: {}", (writeSpeed/1024), rateName );

            rateGram.update( writeSpeed );

            String sizeName = getName( metricsConfig.getNodePrefix(), WRITE_SIZE,
                                       getDefaultName( TransferStreamingOutput.class, WRITE_SIZE ), METER );

            logger.info( "measured size: {} kb to metric: {}", (kbCount/1024), sizeName );

            Histogram sizeGram = metricsManager.getHistogram( sizeName );
            sizeGram.update( Math.round( kbCount ) );

        }
        finally
        {
            IOUtils.closeQuietly( stream );

            rootSpan.ifPresent( SpanAdapter::close );
        }
    }

    private class TransferFieldInjector
                    implements CloseBlockingDecorator
    {
        @Override
        public void decorateSpanAtClose( SpanAdapter span )
        {
            if ( cout == null || start == -1 )
            {
                return;
            }

            if ( span != null )
            {
                span.addField( WRITE_SPEED, writeSpeed );
                span.addField( WRITE_SIZE, kbCount );
            }
        }
    }
}
