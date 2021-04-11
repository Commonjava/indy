/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.client.core.metric;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.commonjava.indy.client.core.inject.ClientMetricSet;
import org.commonjava.o11yphant.metrics.RequestContextHelper;
import org.commonjava.o11yphant.metrics.sli.GoldenSignalsFunctionMetrics;
import org.commonjava.o11yphant.otel.OtelTracePlugin;
import org.commonjava.o11yphant.trace.SpanFieldsDecorator;
import org.commonjava.o11yphant.trace.TraceManager;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.commonjava.indy.client.core.metric.ClientMetricConstants.HEADER_CLIENT_SPAN_ID;
import static org.commonjava.o11yphant.metrics.MetricsConstants.NANOS_PER_MILLISECOND;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.REQUEST_LATENCY_MILLIS;

import static org.commonjava.o11yphant.metrics.RequestContextConstants.REQUEST_LATENCY_NS;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.TRAFFIC_TYPE;

public class ClientMetricManager {

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private ClientTracerConfiguration configuration;

    private TraceManager traceManager;

    private final ClientTrafficClassifier classifier = new ClientTrafficClassifier();

    @ClientMetricSet
    private final ClientGoldenSignalsMetricSet metricSet = new ClientGoldenSignalsMetricSet();

    public ClientMetricManager() {
    }

    public ClientMetricManager( SiteConfig siteConfig )
    {
        this.configuration = buildConfig( siteConfig );
        this.traceManager = new TraceManager( new OtelTracePlugin( configuration, configuration ), new SpanFieldsDecorator(
                        Arrays.asList( new ClientGoldenSignalsSpanFieldsInjector( metricSet ) ) ), configuration );
    }

    public ClientMetrics register( HttpUriRequest request ) {
        logger.debug( "Client honey register: {}", request.getURI().getPath() );
        List<String> functions = classifier.calculateClassifiers( request );

        SpanAdapter clientSpan = traceManager.startClientRequestSpan( getEndpointName( request.getMethod(), request.getURI().getPath() ), request );
        if ( clientSpan != null )
        {
            request.setHeader( HEADER_CLIENT_SPAN_ID, clientSpan.getSpanId() );
        }

        return new ClientMetrics( configuration.isEnabled(), request, clientSpan, functions, metricSet );
    }

    private ClientTracerConfiguration buildConfig( SiteConfig siteConfig ) {
        ClientTracerConfiguration config = new ClientTracerConfiguration();
        config.setEnabled( siteConfig.isMetricEnabled() );
        config.setBaseSampleRate( siteConfig.getBaseSampleRate() );
        return config;
    }

    private String getEndpointName( String method, String pathInfo ) {
        StringBuilder sb = new StringBuilder( method + "_" );
        String[] toks = pathInfo.split( "/" );
        for ( String s : toks ) {
            if ( isBlank( s ) || "api".equals( s ) ) {
                continue;
            }
            sb.append( s );
            if ( "admin".equals( s ) ) {
                sb.append( "_" );
            } else {
                break;
            }
        }
        return sb.toString();
    }

}
