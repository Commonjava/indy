package org.commonjava.indy.metrics.healthcheck;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

/**
 * Created by xiabai on 3/13/17.
 */
public class IndyHealthCheckRegistrySet
                implements MetricSet
{
    private static final Logger logger = LoggerFactory.getLogger( IndyHealthCheckRegistrySet.class );

    private String name = "healthcheck";

    private HealthCheckRegistry healthCheckRegistry;

    public IndyHealthCheckRegistrySet()
    {
        healthCheckRegistry = new HealthCheckRegistry();
    }

    @Override
    public Map<String, Metric> getMetrics()
    {
        final Map<String, Metric> gauges = new HashMap<String, Metric>();
        SortedMap<String, HealthCheck.Result> healthResult = healthCheckRegistry.runHealthChecks();
        healthResult.forEach( ( s, result ) ->
                              {
                                  gauges.put( s, (Gauge<Integer>) () ->
                                  {
                                      if ( result.isHealthy() )
                                      {
                                          return 1;
                                      }
                                      return 0;
                                  } );
                              } );
        logger.info( "get metrics " + gauges );
        return gauges;
    }

    public void register( String name, HealthCheck healthCheck )
    {
        healthCheckRegistry.register( name, healthCheck );
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getName()
    {
        return name;

    }
}
