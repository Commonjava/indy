package org.commonjava.indy.subsys.honeycomb;

import org.commonjava.indy.subsys.honeycomb.config.IndyTraceConfiguration;
import org.commonjava.o11yphant.otel.OtelTracePlugin;
import org.commonjava.o11yphant.trace.SpanFieldsDecorator;
import org.commonjava.o11yphant.trace.TraceManager;
import org.commonjava.o11yphant.trace.spi.SpanFieldsInjector;

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
    private IndyTraceConfiguration config;

    @Inject
    private Instance<SpanFieldsInjector> rsfInstance;

    @PostConstruct
    public void init()
    {
        traceManager = new TraceManager( new OtelTracePlugin( config, config ), new SpanFieldsDecorator( getRootSpanFields() ), config );
    }

    @Produces
    @Default
    public TraceManager getTraceManager()
    {
        return traceManager;
    }

    private List<SpanFieldsInjector> getRootSpanFields()
    {
        List<SpanFieldsInjector> result = new ArrayList<>();
        if ( !rsfInstance.isUnsatisfied() )
        {
            rsfInstance.forEach( rsf -> result.add( rsf ) );
        }
        return result;
    }
}
