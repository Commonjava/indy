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
import org.commonjava.o11yphant.metrics.annotation.Measure;
import org.commonjava.o11yphant.metrics.api.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.commonjava.indy.IndyContentConstants.NANOS_PER_SEC;
import static org.commonjava.o11yphant.metrics.MetricsConstants.METER;
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

    private InputStream stream;

    private MetricsManager metricsManager;

    private IndyMetricsConfig metricsConfig;

    public TransferStreamingOutput( final InputStream stream, final MetricsManager metricsManager,
                                    final IndyMetricsConfig metricsConfig )
    {
        this.stream = stream;
        this.metricsManager = metricsManager;
        this.metricsConfig = metricsConfig;
    }

    @Override
    @Measure
    public void write( final OutputStream out )
        throws IOException, WebApplicationException
    {
        long start = System.nanoTime();
        try
        {
            CountingOutputStream cout = new CountingOutputStream( out );
            IOUtils.copy( stream, cout );

            double kbCount = (double) cout.getByteCount() / 1024;

            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.trace( "Wrote: {} bytes", kbCount );

            long end = System.nanoTime();
            double elapsed = (end-start)/NANOS_PER_SEC;

            String rateName = getName( metricsConfig.getNodePrefix(), WRITE_SPEED,
                                   getDefaultName( TransferStreamingOutput.class, WRITE_SPEED ), METER );

            Histogram rateGram = metricsManager.getHistogram( rateName );
            long writeSpeed = Math.round( kbCount / elapsed );
            logger.info( "measured speed: {} kb/s to metric: {}", (writeSpeed/1024), rateName );

            rateGram.update( writeSpeed );
            addFieldToActiveSpan( WRITE_SPEED, writeSpeed );

            String sizeName = getName( metricsConfig.getNodePrefix(), WRITE_SIZE,
                                       getDefaultName( TransferStreamingOutput.class, WRITE_SIZE ), METER );

            logger.info( "measured size: {} kb to metric: {}", (kbCount/1024), sizeName );

            Histogram sizeGram = metricsManager.getHistogram( sizeName );
            sizeGram.update( Math.round( kbCount ) );
            addFieldToActiveSpan( WRITE_SIZE, kbCount );
        }
        finally
        {
            IOUtils.closeQuietly( stream );

            getActiveSpan().ifPresent( span -> span.close() );
        }
    }

}
