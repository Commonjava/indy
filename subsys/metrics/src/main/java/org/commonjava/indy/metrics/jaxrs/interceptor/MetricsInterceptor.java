package org.commonjava.indy.metrics.jaxrs.interceptor;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.commonjava.indy.IndyMetricsManager;
import org.commonjava.indy.metrics.conf.annotation.IndyMetricsNamed;
import org.commonjava.indy.measure.annotation.IndyMetrics;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.metrics.conf.IndyMetricsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by xiabai on 2/27/17.
 */
@Interceptor
@IndyMetrics
public class MetricsInterceptor
{

    private static final Logger logger = LoggerFactory.getLogger( MetricsInterceptor.class );

    @Inject
    IndyMetricsManager util;

    @Inject
    @IndyMetricsNamed
    IndyMetricsConfig config;

    @AroundInvoke
    public Object operation( InvocationContext context ) throws Exception
    {
        if ( !config.isMetricsEnabled() )
            return context.proceed();
        logger.info( "call in MeterHandler.operation" );
        IndyMetrics metrics = context.getMethod().getAnnotation( IndyMetrics.class );
        if ( metrics == null )
        {
            return context.proceed();
        }

        Measure measures = metrics.measure();
        List<Timer.Context> timers = Stream.of( measures.timers() )
                                           .map( named -> util.getTimer( metrics, measures, named ).time() )
                                           .collect( Collectors.toList() );

        try
        {
            return context.proceed();
        }
        catch ( Exception e )
        {
            Measure me = metrics.exceptions();
            Stream.of( me.meters() ).forEach( ( named ) ->
                                              {
                                                  Meter requests = util.getMeter( metrics, me, named );
                                                  requests.mark();
                                              } );

            throw e;
        }
        finally
        {
            if ( timers != null )
            {
                timers.forEach( Timer.Context::stop );

            }
            Stream.of( measures.meters() ).forEach( ( named ) ->
                                                    {
                                                        Meter requests = util.getMeter( metrics, measures, named );
                                                        requests.mark();
                                                    } );
        }
    }
}
