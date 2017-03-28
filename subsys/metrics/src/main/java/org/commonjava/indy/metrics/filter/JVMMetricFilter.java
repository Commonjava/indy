package org.commonjava.indy.metrics.filter;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

/**
 * Created by xiabai on 3/14/17.
 */
public class JVMMetricFilter
                implements MetricFilter
{
    @Override
    public boolean matches( String name, Metric metric )
    {
        if ( !name.contains( "org.commonjava.indy" ) && name.contains( "jvm" ) )
        {
            return true;
        }
        return false;
    }
}
