package org.commonjava.indy.subsys.honeycomb;

import org.commonjava.cdi.util.weft.ThreadContextualizer;

public class ParentSpanContextualizer
        implements ThreadContextualizer
{
    private static ThreadLocal<SpanContext> TL = new ThreadLocal<SpanContext>();

    public static SpanContext getCurrentSpanContext()
    {
        return TL.get();
    }

    public static void setCurrentSpanContext( final SpanContext context )
    {
        TL.set( context );
    }

    @Override
    public String getId()
    {
        return "honeycomb.parent-span";
    }

    @Override
    public Object extractCurrentContext()
    {
        return TL.get();
    }

    @Override
    public void setChildContext( final Object parentContext )
    {
        TL.set( (SpanContext) parentContext );
    }

    @Override
    public void clearContext()
    {
        TL.remove();
    }
}
