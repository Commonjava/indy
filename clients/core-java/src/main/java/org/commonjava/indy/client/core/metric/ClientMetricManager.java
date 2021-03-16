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

import io.honeycomb.beeline.tracing.Span;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.commonjava.indy.client.core.inject.ClientMetricSet;
import org.commonjava.o11yphant.metrics.RequestContextHelper;
import org.commonjava.o11yphant.metrics.sli.GoldenSignalsFunctionMetrics;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.commonjava.indy.client.core.metric.ClientMetricConstants.HEADER_CLIENT_SPAN_ID;
import static org.commonjava.o11yphant.metrics.MetricsConstants.NANOS_PER_MILLISECOND;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.REQUEST_LATENCY_MILLIS;

import static org.commonjava.o11yphant.metrics.RequestContextConstants.REQUEST_LATENCY_NS;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.TRACE_ID;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.TRAFFIC_TYPE;

public class ClientMetricManager {

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private ClientHoneycombConfiguration configuration;

    private ClientTraceSampler traceSampler;

    private ClientHoneycombManager honeycombManager;

    private final ClientTrafficClassifier classifier = new ClientTrafficClassifier();

    @ClientMetricSet
    private final ClientGoldenSignalsMetricSet metricSet = new ClientGoldenSignalsMetricSet();

    private HttpUriRequest request;

    private HttpResponse response;

    private Span rootSpan;

    private Collection<String> functions = new ArrayList<>();

    private long start = 0l;

    private long end = 0l;

    public ClientMetricManager() {
    }

    public ClientMetricManager( SiteConfig siteConfig, String traceId )
    {
        this.configuration = buildConfig( siteConfig );
        this.traceSampler = new ClientTraceSampler( classifier, configuration );
        this.honeycombManager = new ClientHoneycombManager( configuration, traceSampler );
        RequestContextHelper.setContext( TRACE_ID, traceId );
    }

    public ClientMetricManager register( HttpUriRequest request ) {
        logger.debug( "Client honey register: {}", request.getURI().getPath() );
        defaultTraceMetrics();

        this.request = request;
        functions = classifier.calculateClassifiers( request );
        addTrafficField();

        honeycombManager.init();
        rootSpan = honeycombManager.startRootTracer( getEndpointName( request.getMethod(), request.getURI().getPath() ) );
        if ( rootSpan != null )
        {
            request.setHeader( HEADER_CLIENT_SPAN_ID, rootSpan.getSpanId() );
        }
        return this;
    }

    public void registerStart() {
        logger.debug( "Client honey registerStart: {}", request.getURI().getPath() );
        start = System.nanoTime();
        functions.forEach( function -> metricSet.function( function ).ifPresent(
                GoldenSignalsFunctionMetrics::started ) );
    }

    public void registerErr() {
        logger.debug( "Client honey registerErr: {}", request.getURI().getPath() );
        functions.forEach( function -> metricSet.function( function )
                .ifPresent( GoldenSignalsFunctionMetrics::error ) );
    }

    public void registerEnd( HttpResponse response ) {
        logger.debug( "Client honey registerEnd: {}", request.getURI().getPath() );
        this.response = response;
        end = RequestContextHelper.getRequestEndNanos() - RequestContextHelper.getRawIoWriteNanos();
        RequestContextHelper.setContext( REQUEST_LATENCY_NS, String.valueOf( end - start ) );
        RequestContextHelper.setContext( REQUEST_LATENCY_MILLIS, ( end - start ) / NANOS_PER_MILLISECOND );
        boolean error = ( response != null && response.getStatusLine() != null ) && ( response.getStatusLine().getStatusCode() > 499 );

        functions.forEach( function -> metricSet.function( function ).ifPresent( functionMetrics -> {
            functionMetrics.latency( end - start ).call();
            if ( error ) {
                functionMetrics.error();
            }
        } ) );
    }

    public void process() {
        logger.trace( "Client honey process: {}", request.getURI().getPath() );
        if ( metricSet.getFunctionMetrics().isEmpty() ) {
            logger.trace( "Client honey metricSet is empty: {}", request.getURI().getPath() );
            return;
        }
        String pathInfo = request.getURI().getPath();
        logger.debug( "Client honey process START: {}", pathInfo );

        ClientGoldenSignalsRootSpanFields fields = new ClientGoldenSignalsRootSpanFields( metricSet );
        honeycombManager.registerRootSpanFields( fields );

        if ( rootSpan != null ) {
            rootSpan.addField( "path_info", pathInfo );
            rootSpan.addField( "status_code", response.getStatusLine().getStatusCode() );
            honeycombManager.addFields( rootSpan );
            rootSpan.close();
        }
        honeycombManager.endTrace();
        defaultTraceMetrics();
        logger.debug( "Client honey process END: {}", pathInfo );
    }

    private ClientHoneycombConfiguration buildConfig( SiteConfig siteConfig ) {
        ClientHoneycombConfiguration config = new ClientHoneycombConfiguration();
        config.setEnabled( siteConfig.isMetricEnabled() );
        config.setDataset( siteConfig.getHoneycombDataset() );
        config.setWriteKey( siteConfig.getHoneycombWriteKey() );
        config.setBaseSampleRate( siteConfig.getBaseSampleRate() );
        return config;
    }

    private void addTrafficField()
    {
        Set<String> classifierTokens = new LinkedHashSet<>();
        functions.forEach( function -> {
            String[] parts = function.split( "\\." );
            for ( int i = 0; i < parts.length - 1; i++ )
            {
                classifierTokens.add( parts[i] );
            }
        } );
        RequestContextHelper.setContext( TRAFFIC_TYPE, StringUtils.join( classifierTokens, "," ));
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

    private void defaultTraceMetrics() {
        metricSet.clear();
        functions = new ArrayList<>();
        start = 0l;
        end = 0l;
    }
}
