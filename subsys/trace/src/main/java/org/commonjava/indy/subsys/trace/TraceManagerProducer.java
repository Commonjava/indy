/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.subsys.trace;

import org.commonjava.indy.subsys.metrics.IndyTrafficClassifier;
import org.commonjava.indy.subsys.trace.config.IndyTraceConfiguration;
import org.commonjava.o11yphant.otel.OtelTracePlugin;
import org.commonjava.o11yphant.trace.SpanFieldsDecorator;
import org.commonjava.o11yphant.trace.TraceManager;
import org.commonjava.o11yphant.trace.spi.O11yphantTracePlugin;
import org.commonjava.o11yphant.trace.spi.SpanFieldsInjector;
import org.commonjava.o11yphant.trace.thread.TraceThreadContextualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class TraceManagerProducer
{
    private TraceManager traceManager;

    private TraceThreadContextualizer<?> traceThreadContextualizer;

    @Inject
    private IndyTraceConfiguration config;

    @Inject
    private IndyTrafficClassifier trafficClassifier;

    @Inject
    private Instance<SpanFieldsInjector> rsfInstance;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @PostConstruct
    public void init()
    {
        logger.info( "Initializing Opentelemetry trace plugin" );
        O11yphantTracePlugin<?> plugin = new OtelTracePlugin( config, config );

        traceManager = new TraceManager<>( plugin, new SpanFieldsDecorator( getRootSpanFields() ), config );
        traceThreadContextualizer = traceManager.getTraceThreadContextualizer();
    }

    @Produces
    @Default
    public TraceThreadContextualizer getTraceThreadContextualizer()
    {
        return traceThreadContextualizer;
    }

    @Produces
    @Default
    public TraceManager getTraceManager()
    {
        return traceManager;
    }

    private List<SpanFieldsInjector> getRootSpanFields()
    {
        List<SpanFieldsInjector> result = new ArrayList<>();
        if ( !rsfInstance.isUnsatisfied() )
        {
            rsfInstance.forEach( result::add );
        }
        return result;
    }
}
