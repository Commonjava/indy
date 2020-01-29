package org.commonjava.indy.subsys.honeycomb;

import io.honeycomb.beeline.tracing.Span;
import io.honeycomb.beeline.tracing.sampling.TraceSampler;
import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.indy.metrics.TrafficClassifier;
import org.commonjava.indy.subsys.honeycomb.config.HoneycombConfiguration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.List;
import java.util.Optional;

import static org.commonjava.indy.subsys.honeycomb.interceptor.HoneycombInterceptorUtils.SAMPLE_OVERRIDE;

@ApplicationScoped
public class IndyTraceSampler
        implements TraceSampler<Span>
{
    @Inject
    private TrafficClassifier classifier;

    @Inject
    private HoneycombConfiguration configuration;

    @Override
    public int sample( final Span input )
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        if ( ctx == null )
        {
            return 0;
        }

        List<String> functionClassifiers = classifier.getFunctionClassifiers();
        if ( functionClassifiers != null )
        {
            Optional<Integer> mostAggressive = functionClassifiers.stream()
                                                         .map( classifier -> configuration.getSampleRate( classifier ) )
                                                         .filter( rate -> rate > 0 )
                                                         .sorted( ( one, two ) -> two - one )
                                                         .findFirst();

            if ( mostAggressive.isPresent() )
            {
                return mostAggressive.get();
            }
        }

        if ( Boolean.TRUE == ctx.get( SAMPLE_OVERRIDE ) )
        {
            return 1;
        }

        return configuration.getBaseSampleRate();
    }
}
