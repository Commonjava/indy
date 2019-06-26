package org.commonjava.indy.sli.jaxrs;

import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.indy.metrics.IndyMetricsManager;
import org.commonjava.indy.sli.metrics.GoldenSignalsMetricSet;
import org.slf4j.MDC;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

import static org.commonjava.indy.bind.jaxrs.RequestContextConstants.REQUEST_LATENCY_NS;

@ApplicationScoped
public class GoldenSignalsFilter
    implements Filter
{
    @Inject
    private GoldenSignalsMetricSet metricSet;

    @Override
    public void init( final FilterConfig filterConfig )
    {
    }

    @Override
    public void doFilter( final ServletRequest servletRequest, final ServletResponse servletResponse,
                          final FilterChain filterChain )
            throws IOException, ServletException
    {
        long start = System.nanoTime();

        try
        {
            filterChain.doFilter( servletRequest, servletResponse );
        }
        finally
        {
            long end = System.nanoTime();
            MDC.put( REQUEST_LATENCY_NS, String.valueOf( end - start ) );

            // TODO: Determine the function in play here, and whether it was an error.
            String function = getFunction();
            boolean error = isError();

            metricSet.function( function ).ifPresent( ms -> {
                ms.latency( end-start ).call();
                if ( error )
                {
                    ms.error();
                }
            } );
        }
    }

    private boolean isError()
    {
        ThreadContext context = ThreadContext.getContext( false );

        return false;
    }

    private String getFunction()
    {
        ThreadContext context = ThreadContext.getContext( false );

        
    }

    @Override
    public void destroy()
    {
    }
}
