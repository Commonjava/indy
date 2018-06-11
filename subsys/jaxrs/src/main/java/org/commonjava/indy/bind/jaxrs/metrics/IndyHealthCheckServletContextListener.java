package org.commonjava.indy.bind.jaxrs.metrics;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import org.commonjava.indy.IndyMetricsManager;

import static org.commonjava.indy.IndyMetricsManager.HEALTH_CHECK_REGISTRY;

public class IndyHealthCheckServletContextListener extends HealthCheckServlet.ContextListener
{
    private IndyMetricsManager indyMetricsManager;

    @Override
    protected HealthCheckRegistry getHealthCheckRegistry()
    {
        return HEALTH_CHECK_REGISTRY;
    }
}
