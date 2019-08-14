package org.commonjava.indy.bind.jaxrs.util;

import com.codahale.metrics.Meter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.output.CountingOutputStream;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.measure.annotation.MetricNamed;
import org.commonjava.indy.metrics.IndyMetricsManager;
import org.commonjava.indy.metrics.conf.IndyMetricsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

import static org.commonjava.indy.measure.annotation.MetricNamed.DEFAULT;
import static org.commonjava.indy.metrics.IndyMetricsConstants.METER;
import static org.commonjava.indy.metrics.IndyMetricsConstants.getDefaultName;
import static org.commonjava.indy.metrics.IndyMetricsConstants.getName;

public class DTOStreamingOutput
        implements StreamingOutput
{
    private static final String TRANSFER_METRIC_NAME = "indy.transferred.dto";

    private static final double NANOS_PER_SEC = 1000000000.0;

    private final ObjectMapper mapper;

    private final Object dto;

    private final IndyMetricsManager metricsManager;

    private final IndyMetricsConfig metricsConfig;

    public DTOStreamingOutput( final ObjectMapper mapper, final Object dto, final IndyMetricsManager metricsManager,
                               final IndyMetricsConfig metricsConfig )
    {
        this.mapper = mapper;
        this.dto = dto;
        this.metricsManager = metricsManager;
        this.metricsConfig = metricsConfig;
    }

    @Override
    public void write( final OutputStream outputStream )
            throws IOException, WebApplicationException
    {
        AtomicReference<IOException> ioe = new AtomicReference<>();
        metricsManager.wrapWithStandardMetrics( () -> {
            CountingOutputStream cout = new CountingOutputStream( outputStream );
            long start = System.nanoTime();
            try
            {
                mapper.writeValue( cout, dto );
            }
            catch ( IOException e )
            {
                ioe.set( e );
            }
            finally
            {
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.trace( "Wrote: {} bytes", cout.getByteCount() );

                String name = getName( metricsConfig.getNodePrefix(), TRANSFER_METRIC_NAME,
                                       getDefaultName( dto.getClass().getSimpleName(), "write" ), METER );

                long end = System.nanoTime();
                double elapsed = (end-start)/NANOS_PER_SEC;

                Meter meter = metricsManager.getMeter( name );
                meter.mark( Math.round( cout.getByteCount() / elapsed ) );
            }

            return null;

        }, () -> null );

        if ( ioe.get() != null )
        {
            throw ioe.get();
        }
    }
}
