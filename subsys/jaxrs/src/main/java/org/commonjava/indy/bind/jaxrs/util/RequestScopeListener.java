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
package org.commonjava.indy.bind.jaxrs.util;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

import org.jboss.weld.context.bound.BoundRequestContext;

public class RequestScopeListener
    implements ServletRequestListener
{

    private static final String CDI_CONTEXT = "cdiContext";

    private static final String CDI_INSTANCE_MAP = "cdiInstances";

    @Override
    public void requestDestroyed( final ServletRequestEvent evt )
    {
        final ServletRequest req = evt.getServletRequest();

        @SuppressWarnings( "unchecked" )
        final Map<String, Object> instanceMap = (Map<String, Object>) req.getAttribute( CDI_INSTANCE_MAP );
        final BoundRequestContext ctx = (BoundRequestContext) req.getAttribute( CDI_CONTEXT );

        ctx.invalidate();
        ctx.deactivate();
        ctx.dissociate( instanceMap );

        req.removeAttribute( CDI_CONTEXT );
        req.removeAttribute( CDI_INSTANCE_MAP );
    }

    @Override
    public void requestInitialized( final ServletRequestEvent evt )
    {
        final BoundRequestContext ctx = CDI.current()
                                           .select( BoundRequestContext.class )
                                           .get();
        final Map<String, Object> instanceMap = new HashMap<>();

        final ServletRequest req = evt.getServletRequest();
        req.setAttribute( CDI_CONTEXT, ctx );
        req.setAttribute( CDI_INSTANCE_MAP, instanceMap );

        ctx.associate( instanceMap );
        ctx.activate();
    }

}
