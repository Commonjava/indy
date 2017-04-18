package org.commonjava.indy.metrics.jaxrs.producer;

import com.codahale.metrics.MetricRegistry;
import org.commonjava.indy.metrics.conf.IndyMetricsConfig;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * Created by xiabai on 2/27/17.
 */
public class IndyProducer
{

    @ApplicationScoped
    @Produces
    public MetricRegistry getMetricRegistry()
    {
        return new MetricRegistry();
    }





}
