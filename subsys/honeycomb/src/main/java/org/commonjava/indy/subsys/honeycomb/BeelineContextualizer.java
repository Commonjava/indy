package org.commonjava.indy.subsys.honeycomb;

import io.honeycomb.beeline.tracing.Beeline;
import io.honeycomb.beeline.tracing.SpanBuilderFactory;
import io.honeycomb.beeline.tracing.Tracer;
import io.honeycomb.beeline.tracing.Tracing;
import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.cdi.util.weft.ThreadContextualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

@ApplicationScoped
@Named
public class BeelineContextualizer
        implements ThreadContextualizer
{
    private static final String SPAN_BUILDER_FACTORY = "honeycomb.span-builder-factory";

    private static InheritableThreadLocal<Beeline> TL = new InheritableThreadLocal<>();

    private static Logger logger = LoggerFactory.getLogger( BeelineContextualizer.class );

    public static Beeline getBeeline()
    {
        Beeline bl = TL.get();
        logger.info( "ThreadLocal contains beeline: {}", bl );

        if ( bl == null )
        {
            logger.warn( "Beeline not found in ThreadLocal! Attempting to initialize it using ThreadContext info." );

            SpanBuilderFactory sbf =
                    (SpanBuilderFactory) ThreadContext.getContext( true ).get( SPAN_BUILDER_FACTORY );
            if ( sbf != null )
            {
                bl = setBeeline( sbf );
            }
        }

        return bl;
    }

    public static Beeline setBeeline( final SpanBuilderFactory factory )
    {
        ThreadContext.getContext( true ).put( SPAN_BUILDER_FACTORY, factory );
        Tracer tracer = Tracing.createTracer( factory );
        Beeline beeline = Tracing.createBeeline( tracer, factory );

        TL.set( beeline );

        logger.info( "Set beeline to: {}", TL.get() );

        return beeline;
    }

    @Override
    public String getId()
    {
        return "honeycomb.beeline";
    }

    @Override
    public Object extractCurrentContext()
    {
        Beeline bl = TL.get();
        logger.info( "Extracting Beeline from parent thread: {}", bl );

        return bl;
    }

    @Override
    public void setChildContext( final Object parentContext )
    {
        logger.info( "Initializing beeline using span factory from parent Beeline: {}", parentContext );
        Beeline old = (Beeline) parentContext;

        SpanBuilderFactory factory = old.getSpanBuilderFactory();
        setBeeline( factory );
    }

    @Override
    public void clearContext()
    {
        TL.remove();
        logger.info( "Removed beeline from current ThreadLocal" );
    }
}
