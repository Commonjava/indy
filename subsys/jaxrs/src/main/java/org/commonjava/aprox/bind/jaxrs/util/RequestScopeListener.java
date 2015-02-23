package org.commonjava.aprox.bind.jaxrs.util;

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
