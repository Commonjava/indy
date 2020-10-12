package org.commonjava.indy.subsys.honeycomb;

import org.commonjava.o11yphant.honeycomb.CustomTraceIdProvider;
import org.commonjava.o11yphant.metrics.RequestContextHelper;

import javax.enterprise.context.ApplicationScoped;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.commonjava.o11yphant.honeycomb.util.TraceIdUtils.getUUIDTraceId;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.TRACE_ID;

@ApplicationScoped
public class IndyCustomTraceIdProvider implements CustomTraceIdProvider
{
    @Override
    public String generateId()
    {
        String traceId = RequestContextHelper.getContext( TRACE_ID );
        if ( isNotBlank(traceId ))
        {
            return traceId;
        }
        return getUUIDTraceId();
    }
}
