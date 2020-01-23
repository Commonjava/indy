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
package org.commonjava.indy.bind.jaxrs;

import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.metrics.IndyMetricsConstants;
import org.commonjava.indy.metrics.IndyMetricsManager;
import org.commonjava.indy.metrics.RequestContextHelper;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.commonjava.indy.metrics.RequestContextHelper.CLIENT_ADDR;
import static org.commonjava.indy.metrics.RequestContextHelper.CUMULATIVE_COUNTS;
import static org.commonjava.indy.metrics.RequestContextHelper.CUMULATIVE_TIMINGS;
import static org.commonjava.indy.metrics.RequestContextHelper.EXTERNAL_ID;
import static org.commonjava.indy.metrics.RequestContextHelper.INTERNAL_ID;
import static org.commonjava.indy.metrics.RequestContextHelper.IS_METERED;
import static org.commonjava.indy.metrics.RequestContextHelper.PREFERRED_ID;
import static org.commonjava.indy.metrics.RequestContextHelper.REQUEST_PHASE;
import static org.commonjava.indy.metrics.RequestContextHelper.REQUEST_PHASE_START;
import static org.commonjava.indy.metrics.RequestContextHelper.X_FORWARDED_FOR;

@ApplicationScoped
public class ThreadContextFilter
        implements Filter
{
    @Inject
    private MDCManager mdcManager;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public void init( final FilterConfig filterConfig )
            throws ServletException
    {
    }

    @Override
    @Measure
    public void doFilter( final ServletRequest request, final ServletResponse response, final FilterChain chain )
            throws IOException, ServletException
    {
        try
        {
            ThreadContext.clearContext();
            ThreadContext.getContext( true );

            chain.doFilter( request, response );
        }
        finally
        {
            ThreadContext.clearContext();
            mdcManager.clear();
        }
    }

    @Override
    public void destroy()
    {
    }

}
