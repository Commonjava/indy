package org.commonjava.indy.subsys.honeycomb;

import org.commonjava.indy.subsys.honeycomb.config.IndyHoneycombConfiguration;
import org.commonjava.o11yphant.otel.OtelTracePlugin;
import org.commonjava.o11yphant.trace.RootSpanDecorator;
import org.commonjava.o11yphant.trace.TraceManager;
import org.commonjava.o11yphant.trace.spi.RootSpanFields;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class TraceManagerProducer
{
    private TraceManager traceManager;

    @Inject
    private IndyHoneycombConfiguration config;

    @Inject
    private Instance<RootSpanFields> rsfInstance;

    @PostConstruct
    public void init()
    {
        traceManager = new TraceManager( new OtelTracePlugin( config ), new RootSpanDecorator( getRootSpanFields() ) );
    }

    @Produces
    @Default
    public TraceManager getTraceManager()
    {
        return traceManager;
    }

    private List<RootSpanFields> getRootSpanFields()
    {
        List<RootSpanFields> result = new ArrayList<>();
        if ( !rsfInstance.isUnsatisfied() )
        {
            rsfInstance.forEach( rsf -> result.add( rsf ) );
        }
        return result;
    }
}
