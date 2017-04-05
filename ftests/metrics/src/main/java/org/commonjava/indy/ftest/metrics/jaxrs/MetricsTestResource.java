package org.commonjava.indy.ftest.metrics.jaxrs;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.measure.annotation.IndyMetrics;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.measure.annotation.MetricNamed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Random;

/**
 * Created by xiabai on 3/22/17.
 */
@Path( "/api/ftest/metrics/" )
@Produces( "application/json" )
@Consumes( "application/json" )
public class MetricsTestResource
                implements IndyResources
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    MetricRegistry metricRegistry;

    @GET
    @Path( "/metricRegistry/timer" )
    public Response getTimerCount()
    {
        Timer timer = metricRegistry.timer( MetricRegistry.name( MetricsTestResource.class, "testTimerRequest" ) );
        return Response.ok( timer.getCount(), MediaType.APPLICATION_JSON ).build();
    }

    @GET
    @Path( "/metricRegistry/timer/exception" )
    public Response getTimerCountWithException()
    {
        Meter meter = null;
        try
        {
            meter = metricRegistry.meter(
                            MetricRegistry.name( MetricsTestResource.class, "testTimerRequestWithException" ) );
        }
        catch ( Throwable t )
        {
            t.printStackTrace();
        }
        return Response.ok( meter.getCount(), MediaType.APPLICATION_JSON ).build();
    }

    @GET
    @Path( "/metricRegistry/meter" )
    public Response getMeterCount()
    {
        Meter meter = metricRegistry.meter( MetricRegistry.name( MetricsTestResource.class, "testMeterRequest" ) );
        return Response.ok( meter.getCount(), MediaType.APPLICATION_JSON ).build();
    }

    @GET
    @Path( "/metricRegistry/meter/exception" )
    public Response getMeterCountWithException()
    {
        Meter meter = metricRegistry.meter(
                        MetricRegistry.name( MetricsTestResource.class, "testMeterRequestException" ) );
        return Response.ok( meter.getCount(), MediaType.APPLICATION_JSON ).build();
    }

    @GET
    @Path( "/timer/{isException :[a-zA-Z]+}" )
    @IndyMetrics( c = MetricsTestResource.class, measure = @Measure( timers = @MetricNamed( name = "testTimerRequest" ) ), exceptions = @Measure( meters = @MetricNamed( name = "testTimerRequestWithException" ) ) )
    public Response getTimer( @PathParam( "isException" ) String isException ) throws Exception
    {
        if ( isException.equals( "true" ) )
        {
            throw new Exception( "MetricsTest has a exception" );
        }
        logger.info( "call in method : MetricsTest" );
        Random random = new Random();
        Thread.sleep( random.nextInt( 100 ) );
        return Response.ok( " \"Timer: well done\"", MediaType.APPLICATION_JSON ).build();
    }

    @GET
    @Path( "/meter/{isException :[a-zA-Z]+}" )
    @IndyMetrics( c = MetricsTestResource.class, measure = @Measure( meters = @MetricNamed( name = "testMeterRequest" ) ), exceptions = @Measure( meters = @MetricNamed( name = "testMeterRequestException" ) ) )
    public Response getMeter( @PathParam( "isException" ) String isException ) throws Exception
    {
        logger.info( "call in method : getMeter" );
        if ( isException.equals( "true" ) )
        {
            throw new Exception( "getMeter has a exception" );
        }
        Thread.sleep( 100 );
        return Response.ok( " " + "\"Meter :well done\"", MediaType.APPLICATION_JSON ).build();
    }
}
