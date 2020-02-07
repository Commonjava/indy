package org.commonjava.indy.subsys.honeycomb;

import io.honeycomb.beeline.tracing.TracerSpan;
import io.honeycomb.beeline.tracing.context.TracingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayDeque;
import java.util.Deque;

@ApplicationScoped
public class IndyTracingContext
        implements TracingContext
{
    private static ThreadLocal<Deque<TracerSpan>> SPANS = ThreadLocal.withInitial( ArrayDeque::new );

    private Logger logger = LoggerFactory.getLogger( getClass() );

    public void reinitThreadSpans()
    {
        logger.info( "Clearing spans in current thread: {}", Thread.currentThread().getId()  );
        SPANS.set( new ArrayDeque<>() );
    }

    public void clearThreadSpans()
    {
        logger.info( "Clearing context...SPANs in current thread: {} (thread: {})", SPANS.get().size(), Thread.currentThread().getId() );
        TracerSpan tracerSpan = SPANS.get().peekLast();
        if ( tracerSpan != null )
        {
            tracerSpan.close();
        }

        logger.info( "Clearing spans deque in: {}", Thread.currentThread().getId()  );
        SPANS.remove();
    }

    @Override
    public Deque<TracerSpan> get()
    {
        return SPANS.get();
    }

    @Override
    public int size()
    {
        logger.info( "SPANs in current thread: {} (thread: {})", SPANS.get().size(), Thread.currentThread().getId() );
        return SPANS.get().size();
    }

    @Override
    public TracerSpan peekLast()
    {
        logger.info( "SPANs in current thread: {} (thread: {})", SPANS.get().size(), Thread.currentThread().getId()  );
        return SPANS.get().peekLast();
    }

    @Override
    public TracerSpan peekFirst()
    {
        logger.info( "SPANs in current thread: {} (thread: {})", SPANS.get().size(), Thread.currentThread().getId() );
        return SPANS.get().peekFirst();
    }

    @Override
    public boolean isEmpty()
    {
        Deque<TracerSpan> spans = SPANS.get();
        logger.info( "SPANs in current thread: {} (thread: {})", spans.size(), Thread.currentThread().getId() );
        boolean empty = spans.isEmpty();

        logger.info( "SPANs.isEmpty() ? {}", empty );
        return empty;
    }

    @Override
    public void push( final TracerSpan span )
    {
        logger.info( "SPANs in current thread: {} (thread: {})", SPANS.get().size(), Thread.currentThread().getId()  );
        SPANS.get().push( span );
    }

    @Override
    public TracerSpan pop()
    {
        logger.info( "Pre-POP SPANs in current thread: {} (thread: {})", SPANS.get().size(), Thread.currentThread().getId()  );

        TracerSpan span = SPANS.get().pop();

        logger.info( "Post-POP SPANs in current thread: {} (thread: {})", SPANS.get().size(), Thread.currentThread().getId()  );
        return span;
    }
}
