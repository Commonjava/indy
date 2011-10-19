package org.commonjava.aprox.core.rest.access;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

public interface GroupAccessResource
{

    Response getProxyContent( final String name, final String path );

    Response createContent( final String name, final String path, final HttpServletRequest request );

}