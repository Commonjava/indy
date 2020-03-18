/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.subsys.honeycomb;

import io.honeycomb.beeline.tracing.Span;
import io.honeycomb.beeline.tracing.sampling.TraceSampler;
import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.indy.metrics.TrafficClassifier;
import org.commonjava.indy.subsys.honeycomb.config.HoneycombConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.commonjava.indy.subsys.honeycomb.interceptor.HoneycombInterceptorUtils.SAMPLE_OVERRIDE;

@ApplicationScoped
public class IndyTraceSampler
        implements TraceSampler<String>
{
    private final Random random = new Random();

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private TrafficClassifier classifier;

    @Inject
    private HoneycombConfiguration configuration;

    @Override
    public int sample( final String input )
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        if ( ctx == null )
        {
            logger.debug( "No ThreadContext for functional diagnosis; skipping span: {}", input );
            return 0;
        }

        if ( ctx.get( SAMPLE_OVERRIDE ) != null )
        {
            logger.debug( "Including span via override (span: {})", input );
            return 1;
        }

        List<String> functionClassifiers = classifier.getCachedFunctionClassifiers();
        Integer rate = configuration.getBaseSampleRate();

        rate = configuration.getSampleRate( input );

        if ( rate == configuration.getBaseSampleRate() && functionClassifiers != null )
        {
            Optional<Integer> mostAggressive = functionClassifiers.stream()
                                                                  .map( classifier -> configuration.getSampleRate(
                                                                          classifier ) )
                                                                  .filter( theRate -> theRate > 0 )
                                                                  .min( ( one, two ) -> two - one );

            if ( mostAggressive.isPresent() )
            {
                rate = mostAggressive.get();
            }
        }

        if ( rate == 1 || Math.abs( random.nextInt() ) % rate == 0 )
        {
            logger.debug( "Including span due to sampling rate: {} (span: {})", rate, input );
            return 1;
        }

        logger.debug( "Skipping span due to sampling rate: {} (span: {})", rate, input );
        return 0;
    }
}
