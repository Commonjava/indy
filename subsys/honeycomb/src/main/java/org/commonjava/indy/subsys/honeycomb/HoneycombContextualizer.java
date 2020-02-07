package org.commonjava.indy.subsys.honeycomb;

import io.honeycomb.beeline.tracing.Span;
import org.commonjava.cdi.util.weft.ThreadContextualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

@ApplicationScoped
@Named
public class HoneycombContextualizer
        implements ThreadContextualizer
{
    private static final String THREAD_NAME = "thread.name";

    private static final String THREAD_GROUP_NAME = "thread.group.name";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static ThreadLocal<Span> SPAN = new ThreadLocal<>();

    @Inject
    private HoneycombManager honeycombManager;

    @Inject
    private IndyTracingContext tracingContext;

    @Override
    public String getId()
    {
        return "honeycomb.threadpool.spanner";
    }

    @Override
    public Object extractCurrentContext()
    {
        SpanContext ctx = new SpanContext( honeycombManager.getBeeline().getActiveSpan() );
        logger.trace( "Extracting parent-thread context: {}", ctx );
        return ctx;
    }

    @Override
    public void setChildContext( final Object parentContext )
    {
        tracingContext.reinitThreadSpans();

        logger.trace( "Creating thread-level root span using parent-thread context: {}", parentContext );
        SPAN.set( honeycombManager.startRootTracer( "thread." + Thread.currentThread().getThreadGroup().getName(), (SpanContext) parentContext ) );
    }

    @Override
    public void clearContext()
    {
        Span span = SPAN.get();
        if ( span != null )
        {
            logger.trace( "Closing thread-level root span: {}", span );
            honeycombManager.addFields( span );
            span.addField( THREAD_NAME, Thread.currentThread().getName() );
            span.addField( THREAD_GROUP_NAME, Thread.currentThread().getThreadGroup().getName() );

            span.close();

            honeycombManager.endTrace();
        }

        SPAN.remove();

        tracingContext.clearThreadSpans();
    }
}
