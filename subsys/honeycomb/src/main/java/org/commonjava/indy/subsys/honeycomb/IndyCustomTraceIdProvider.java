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

import org.commonjava.o11yphant.honeycomb.CustomTraceIdProvider;
import org.commonjava.o11yphant.metrics.RequestContextHelper;

import javax.enterprise.context.ApplicationScoped;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.commonjava.o11yphant.honeycomb.util.TraceIdUtils.getUUIDTraceId;
import static org.commonjava.o11yphant.metrics.RequestContextHelper.TRACE_ID;

@ApplicationScoped
public class IndyCustomTraceIdProvider implements CustomTraceIdProvider
{
    @Override
    public String generateId()
    {
        String traceId = RequestContextHelper.getContext( TRACE_ID );
        if ( isNotBlank(traceId ))
        {
            return traceId;
        }
        return getUUIDTraceId();
    }
}
