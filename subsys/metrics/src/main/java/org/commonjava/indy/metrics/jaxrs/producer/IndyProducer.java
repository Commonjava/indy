package org.commonjava.indy.metrics.jaxrs.producer;

import com.codahale.metrics.MetricRegistry;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

/**
 * Created by xiabai on 2/27/17.
 */
public class IndyProducer
{

//    @Inject
//    IndyMetricsConfig indyMetricsConfig;

    @ApplicationScoped
    @Produces
    public MetricRegistry getMetricRegistry()
    {
        return new MetricRegistry();
    }


//    @Dependent
//    @Produces
//    @IndyMetricsNamed
////    @Default
//    public  IndyMetricsConfig getIndyMetrcisConfig(){
//        return indyMetricsConfig;
//    }
}
