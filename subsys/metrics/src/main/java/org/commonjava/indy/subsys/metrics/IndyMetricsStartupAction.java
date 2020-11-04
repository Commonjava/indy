package org.commonjava.indy.subsys.metrics;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.StartupAction;
import org.commonjava.o11yphant.metrics.reporter.ReporterInitializer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class IndyMetricsStartupAction implements StartupAction
{
    @Inject
    private ReporterInitializer reporterInitializer;

    @Override
    public void start() throws IndyLifecycleException
    {
        try
        {
            reporterInitializer.init();
        }
        catch ( Exception e )
        {
            throw new IndyLifecycleException( "Failed to setup metrics!", e );
        }
    }

    @Override
    public int getStartupPriority()
    {
        return 10;
    }

    @Override
    public String getId()
    {
        return "Indy metrics initialization";
    }
}
