package org.commonjava.indy.subsys.honeycomb;

import io.honeycomb.beeline.tracing.Beeline;
import io.honeycomb.beeline.tracing.SpanBuilderFactory;
import io.honeycomb.beeline.tracing.SpanPostProcessor;
import io.honeycomb.beeline.tracing.Tracer;
import io.honeycomb.beeline.tracing.Tracing;
import io.honeycomb.beeline.tracing.sampling.Sampling;
import io.honeycomb.libhoney.HoneyClient;
import io.honeycomb.libhoney.LibHoney;
import org.commonjava.cdi.util.weft.ThreadContextualizer;
import org.commonjava.indy.subsys.honeycomb.config.HoneycombConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@ApplicationScoped
@Named
public class HoneycombContextualizer
        implements ThreadContextualizer
{
    private static final String SPAN_BUILDER_FACTORY = "honeycomb.span-builder-factory";

    private static InheritableThreadLocal<Beeline> BEELINE = new InheritableThreadLocal<>();
    private static InheritableThreadLocal<HoneyClient> CLIENT = new InheritableThreadLocal<>();

    private static Logger logger = LoggerFactory.getLogger( HoneycombContextualizer.class );

    @Inject
    private HoneycombConfiguration configuration;

    @Inject
    private IndyTraceSampler traceSampler;

    public Beeline getBeeline()
    {
        Beeline bl = BEELINE.get();
        logger.info( "ThreadLocal contains beeline: {}", bl );

        if ( bl == null )
        {
            initThread();
            bl = BEELINE.get();
        }

        return bl;
    }

    public HoneyClient getHoneyClient()
    {
        HoneyClient client = CLIENT.get();
        if ( client == null )
        {
            initThread();
            client = CLIENT.get();
        }

        return client;
    }

    private void initThread()
    {
        logger.warn( "Beeline not found in ThreadLocal! Attempting to initialize it using ThreadContext info." );

        if ( !configuration.isEnabled() )
        {
            logger.debug( "Honeycomb is not enabled" );
            return;
        }

        String writeKey = configuration.getWriteKey();
        String dataset = configuration.getDataset();
        if ( isNotBlank( writeKey ) && isNotBlank( dataset ) )
        {
            logger.debug( "Init Honeycomb manager, dataset: {}", dataset );
            HoneyClient client = LibHoney.create( LibHoney.options().setDataset( dataset ).setWriteKey( writeKey ).build() );
            client.closeOnShutdown();
            CLIENT.set( client );

            SpanPostProcessor postProcessor = Tracing.createSpanProcessor( client, traceSampler );
            SpanBuilderFactory factory = Tracing.createSpanBuilderFactory( postProcessor, Sampling.alwaysSampler() );

            Tracer tracer = Tracing.createTracer( factory );
            BEELINE.set( Tracing.createBeeline( tracer, factory ) );
        }
    }

    @Override
    public String getId()
    {
        return "honeycomb.beeline";
    }

    @Override
    public Object extractCurrentContext()
    {
        return null;
    }

    @Override
    public void setChildContext( final Object parentContext )
    {
        initThread();
    }

    @Override
    public void clearContext()
    {
    }
}
