package org.commonjava.indy.metrics.healthcheck;

import com.codahale.metrics.health.HealthCheck;

/**
 * Created by xiabai on 3/10/17.
 */
public abstract class IndyHealthCheck
                extends HealthCheck
{

    public abstract String getName();
}
