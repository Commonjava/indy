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

import org.apache.http.client.methods.HttpUriRequest;
import org.commonjava.indy.client.core.inject.ClientMetricSet;
import org.commonjava.o11yphant.honeycomb.HoneycombTracePlugin;
import org.commonjava.o11yphant.trace.SpanFieldsDecorator;
import org.commonjava.o11yphant.trace.TraceManager;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class ClientMetricManager {

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private ClientTracerConfiguration configuration;

    private Optional<TraceManager> traceManager;

    private final ClientTrafficClassifier classifier = new ClientTrafficClassifier();

    @ClientMetricSet
    private final ClientGoldenSignalsMetricSet metricSet = new ClientGoldenSignalsMetricSet();

    public ClientMetricManager() {
    }

    public ClientMetricManager( SiteConfig siteConfig )
    {
        this.configuration = buildConfig( siteConfig );
        if ( this.configuration.isEnabled() )
        {
            this.traceManager = Optional.of( new TraceManager(
                            new HoneycombTracePlugin( configuration, configuration, Optional.of( classifier ) ),
                            new SpanFieldsDecorator(
                                            Arrays.asList( new ClientGoldenSignalsSpanFieldsInjector( metricSet ) ) ),
                            configuration ) );
        }
        else
        {
            this.traceManager = Optional.empty();
        }
    }

    public ClientMetrics register( HttpUriRequest request ) {
        logger.debug( "Client honey register: {}", request.getURI().getPath() );
        List<String> functions = classifier.calculateClassifiers( request );

        return new ClientMetrics( configuration.isEnabled(), request, functions, metricSet );
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

    public Optional<TraceManager> getTraceManager()
    {
        return traceManager;
    }
}
