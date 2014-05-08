package org.commonjava.aprox.depgraph.jaxrs.util;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

public final class DepgraphParamUtils
{

    public static final String WSID_PARAM = "wsid";

    private DepgraphParamUtils()
    {
    }

    public static String getWorkspaceId( final UriInfo info )
    {
        final MultivaluedMap<String, String> pathParameters = info.getPathParameters();
        String wsid = pathParameters.getFirst( WSID_PARAM );
        if ( wsid == null )
        {
            final MultivaluedMap<String, String> queryParameters = info.getQueryParameters();
            wsid = queryParameters.getFirst( WSID_PARAM );
        }

        return wsid;
    }

}
