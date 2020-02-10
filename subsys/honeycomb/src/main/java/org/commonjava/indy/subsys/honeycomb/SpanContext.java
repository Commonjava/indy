package org.commonjava.indy.subsys.honeycomb;

import io.honeycomb.beeline.tracing.Span;

public class SpanContext
{
    private String traceId;

    private String parentSpanId;

    public SpanContext( final String traceId, final String parentSpanId )
    {
        this.traceId = traceId;
        this.parentSpanId = parentSpanId;
    }

    public SpanContext( final Span span )
    {
        this.traceId = span.getTraceId();
        this.parentSpanId = span.getSpanId();
    }

    public String getParentSpanId()
    {
        return parentSpanId;
    }

    public String getTraceId()
    {
        return traceId;
    }

    @Override
    public String toString()
    {
        return "SpanContext{" + "traceId='" + traceId + '\'' + ", parentSpanId='" + parentSpanId + '\'' + '}';
    }
}
