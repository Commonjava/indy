/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.measure.annotation.MetricNamed;
import org.commonjava.indy.metrics.IndyMetricsManager;
import org.commonjava.indy.metrics.conf.IndyMetricsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.commonjava.indy.measure.annotation.MetricNamed.DEFAULT;
import static org.commonjava.indy.metrics.IndyMetricsConstants.METER;
import static org.commonjava.indy.metrics.IndyMetricsConstants.getName;
import static org.commonjava.indy.metrics.IndyMetricsConstants.getDefaultName;

public class TransferStreamingOutput
    implements StreamingOutput
{

    private static final String TRANSFER_METRIC_NAME = "indy.transferred";

    private InputStream stream;

    private IndyMetricsManager metricsManager;

    private IndyMetricsConfig metricsConfig;

    public TransferStreamingOutput( final InputStream stream, final IndyMetricsManager metricsManager,
                                    final IndyMetricsConfig metricsConfig )
    {
        this.stream = stream;
        this.metricsManager = metricsManager;
        this.metricsConfig = metricsConfig;
    }

    @Override
    @Measure( timers = @MetricNamed( DEFAULT ) )
    public void write( final OutputStream out )
        throws IOException, WebApplicationException
    {
        try
        {
            CountingOutputStream cout = new CountingOutputStream( out );
            IOUtils.copy( stream, cout );

            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.trace( "Wrote: {} bytes", cout.getByteCount() );

            String name = getName( metricsConfig.getNodePrefix(), TRANSFER_METRIC_NAME,
                                   getDefaultName( TransferStreamingOutput.class, "write" ), METER );

            Meter meter = metricsManager.getMeter( name );
            meter.mark( cout.getByteCount() );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
        }
    }

}
