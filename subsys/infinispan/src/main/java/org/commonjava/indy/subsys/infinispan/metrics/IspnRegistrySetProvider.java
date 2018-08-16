package org.commonjava.indy.subsys.infinispan.metrics;

import com.codahale.metrics.MetricRegistry;
import org.commonjava.indy.metrics.MetricSetProvider;
import org.commonjava.indy.metrics.conf.IndyMetricsConfig;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;
import static org.commonjava.indy.subsys.infinispan.metrics.IspnCheckRegistrySet.INDY_METRIC_ISPN;

@ApplicationScoped
public class IspnRegistrySetProvider
        implements MetricSetProvider
{
    @Inject
    private IndyMetricsConfig metricsConfig;

    @Inject
    private CacheProducer cacheProducer;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public void registerMetricSet( final MetricRegistry registry )
    {
        if ( !metricsConfig.isIspnMetricsEnabled() )
        {
            return;
        }

        logger.info( "Adding ISPN checks to registry: {}", registry );
        String gauges = metricsConfig.getIspnGauges();
        List<String> list = null;
        if ( gauges != null )
        {
            list = Arrays.asList( gauges.trim().split( "\\s*,\\s*" ) );
        }

        registry.register( name( metricsConfig.getNodePrefix(), INDY_METRIC_ISPN ),
                                 new IspnCheckRegistrySet( cacheProducer.getCacheManager(), list ) );
    }
}
