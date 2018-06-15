package org.commonjava.indy.bind.jaxrs.metrics;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;

import static org.commonjava.indy.metrics.IndyMetricsManager.HEALTH_CHECK_REGISTRY;

public class IndyHealthCheckServletContextListener extends HealthCheckServlet.ContextListener
{
    @Override
    protected HealthCheckRegistry getHealthCheckRegistry()
    {
        return HEALTH_CHECK_REGISTRY;
    }
}
