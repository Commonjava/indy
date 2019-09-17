/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.httprox.util;

import org.apache.http.HttpRequest;
import org.commonjava.indy.sli.metrics.GoldenSignalsMetricSet;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.net.SocketAddress;

import static java.lang.Integer.parseInt;
import static org.commonjava.indy.bind.jaxrs.RequestContextHelper.HTTP_METHOD;
import static org.commonjava.indy.bind.jaxrs.RequestContextHelper.HTTP_STATUS;
import static org.commonjava.indy.bind.jaxrs.RequestContextHelper.REQUEST_LATENCY_NS;
import static org.commonjava.indy.bind.jaxrs.RequestContextHelper.REQUEST_PHASE;
import static org.commonjava.indy.bind.jaxrs.RequestContextHelper.REQUEST_PHASE_END;
import static org.commonjava.indy.bind.jaxrs.RequestContextHelper.getContext;
import static org.commonjava.indy.bind.jaxrs.RequestContextHelper.setContext;

public class ProxyMeter
{
    private boolean summaryReported;

    private final String method;

    private final String requestLine;

    private final long startNanos;

    private final GoldenSignalsMetricSet sliMetricSet;

    private final Logger restLogger;

    private final SocketAddress peerAddress;

    public ProxyMeter( final String method, final String requestLine, final long startNanos, final GoldenSignalsMetricSet sliMetricSet, final Logger restLogger,
                       final SocketAddress peerAddress )
    {
        this.method = method;
        this.requestLine = requestLine;
        this.startNanos = startNanos;
        this.sliMetricSet = sliMetricSet;
        this.restLogger = restLogger;
        this.peerAddress = peerAddress;
    }

    public void reportResponseSummary()
    {
        /*
         Here, we make this call idempotent to make the logic easier in the doHandleEvent method.
         This way, for content-transfer requests we will call this JUST BEFORE the transfer begins,
         while for all other requests we will handle it in the finally block of the doHandleEvent() method.

         NOTE: This will probably result in incorrect latency measurements for any client using HTTPS via the
         CONNECT method.
        */
        if ( !summaryReported )
        {
            summaryReported = true;

            long latency = System.nanoTime() - startNanos;

            MDC.put( REQUEST_LATENCY_NS, String.valueOf( latency ) );
            setContext( HTTP_METHOD, method );

            // log SLI metrics
            if ( sliMetricSet != null )
            {
                sliMetricSet.function( GoldenSignalsMetricSet.FN_CONTENT_GENERIC ).ifPresent( ms ->{
                    ms.latency( latency ).call();

                    if ( getContext( HTTP_STATUS, 200 ) > 499 )
                    {
                        ms.error();
                    }
                } );
            }

            MDC.put( REQUEST_PHASE, REQUEST_PHASE_END );
            restLogger.info( "END {} (from: {})", requestLine, peerAddress );
            MDC.remove( REQUEST_PHASE );
        }
    }

    public ProxyMeter copy( final long startNanos, final String method, final String requestLine )
    {
        return new ProxyMeter( method, requestLine, startNanos, sliMetricSet, restLogger, peerAddress );
    }
}
