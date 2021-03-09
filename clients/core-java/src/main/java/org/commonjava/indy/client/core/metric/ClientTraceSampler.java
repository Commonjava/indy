/**
 * Copyright (C) 2020 Red Hat, Inc. (nos-devel@redhat.com)
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
package org.commonjava.indy.client.core.metric;

import io.honeycomb.beeline.tracing.sampling.TraceSampler;
import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.o11yphant.honeycomb.config.HoneycombConfiguration;
import org.commonjava.o11yphant.metrics.TrafficClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ClientTraceSampler
                implements TraceSampler<String>
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final String SAMPLE_OVERRIDE = "honeycomb.sample-override";

    private TrafficClassifier classifier;

    private HoneycombConfiguration configuration;

    public ClientTraceSampler( TrafficClassifier classifier, HoneycombConfiguration configuration ) {
        this.classifier = classifier;
        this.configuration = configuration;
    }

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
        return configuration.getSampleRate( input );
    }
}