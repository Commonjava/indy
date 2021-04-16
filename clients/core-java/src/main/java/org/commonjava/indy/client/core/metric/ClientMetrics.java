package org.commonjava.indy.client.core.metric;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.commonjava.o11yphant.metrics.RequestContextHelper;
import org.commonjava.o11yphant.metrics.sli.GoldenSignalsFunctionMetrics;
import org.slf4j.Logger;

import java.io.Closeable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.commonjava.o11yphant.metrics.MetricsConstants.NANOS_PER_MILLISECOND;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.REQUEST_LATENCY_MILLIS;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.REQUEST_LATENCY_NS;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.TRAFFIC_TYPE;
import static org.commonjava.o11yphant.trace.TraceManager.addFieldToActiveSpan;
import static org.slf4j.LoggerFactory.getLogger;

public class ClientMetrics
                extends ClientMetricManager implements Closeable
{
    private static final String ERROR = "request-error";

    private boolean enabled;

    private final HttpUriRequest request;

    private Collection<String> functions;

    private ClientGoldenSignalsMetricSet metricSet;

    private final Logger logger = getLogger( getClass().getName() );

    private final long start;

    private HttpResponse response;

    private long end;

    public ClientMetrics( boolean enabled, HttpUriRequest request, Collection<String> functions,
                          ClientGoldenSignalsMetricSet metricSet )
    {
        this.enabled = enabled;
        this.request = request;
        this.functions = functions;
        this.metricSet = metricSet;
        this.start = System.nanoTime();

        if ( enabled )
        {
            logger.debug( "Client trace starting: {}", request.getURI().getPath() );
            functions.forEach( function -> metricSet.function( function ).ifPresent( GoldenSignalsFunctionMetrics::started ) );

            Set<String> classifierTokens = new LinkedHashSet<>();
            functions.forEach( function -> {
                String[] parts = function.split( "\\." );
                for ( int i = 0; i < parts.length - 1; i++ )
                {
                    classifierTokens.add( parts[i] );
                }
            } );

            String classification = StringUtils.join( classifierTokens, "," );

            RequestContextHelper.setContext( TRAFFIC_TYPE, classification );
            addFieldToActiveSpan( TRAFFIC_TYPE, classification );
        }
    }

    public void registerErr( Object error ) {
        if ( !enabled )
        {
            return;
        }

        logger.debug( "Client trace registerErr: {}", request.getURI().getPath() );
        if ( error instanceof Throwable )
        {
            StringBuilder sb = new StringBuilder();
            sb.append( error.getClass().getSimpleName() );
            sb.append( ": " );
            sb.append( ( (Throwable) error ).getMessage() );
            addFieldToActiveSpan( ERROR, sb );
        }
        else
        {
            addFieldToActiveSpan( ERROR, error );
        }
        functions.forEach( function -> metricSet.function( function )
                                                .ifPresent( GoldenSignalsFunctionMetrics::error ) );
    }

    public void registerEnd( HttpResponse response )
    {
        if ( !enabled )
        {
            return;
        }

        this.response = response;

        logger.debug( "Client trace registerEnd: {}", request.getURI().getPath() );
        boolean error = ( response != null && response.getStatusLine() != null ) && ( response.getStatusLine().getStatusCode() > 499 );

        functions.forEach( function -> metricSet.function( function ).ifPresent( functionMetrics -> {
            functionMetrics.latency( end - start ).call();
            if ( error ) {
                functionMetrics.error();
            }
        } ) );
    }

    @Override
    public void close()
    {
        if ( !enabled )
        {
            return;
        }

        logger.trace( "Client trace closing: {}", request.getURI().getPath() );

        this.end = RequestContextHelper.getRequestEndNanos() - RequestContextHelper.getRawIoWriteNanos();
        RequestContextHelper.setContext( REQUEST_LATENCY_NS, String.valueOf( end - start ) );
        RequestContextHelper.setContext( REQUEST_LATENCY_MILLIS, ( end - start ) / NANOS_PER_MILLISECOND );

        if ( metricSet.getFunctionMetrics().isEmpty() ) {
            logger.trace( "Client trace metricSet is empty: {}", request.getURI().getPath() );
            return;
        }
        String pathInfo = request.getURI().getPath();

        addFieldToActiveSpan( "path_info", pathInfo );
    }
}
