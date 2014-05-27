package org.commonjava.aprox.depgraph.vertx.util;

import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_wsid;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.q_wsid;

import org.vertx.java.core.http.HttpServerRequest;

public final class DepgraphParamUtils
{

    private DepgraphParamUtils()
    {
    }

    public static String getWorkspaceId( final HttpServerRequest request )
    {
        String wsid = request.params()
                             .get( p_wsid.key() );
        if ( wsid == null )
        {
            wsid = request.params()
                          .get( q_wsid.key() );
        }

        return wsid;
    }

}
