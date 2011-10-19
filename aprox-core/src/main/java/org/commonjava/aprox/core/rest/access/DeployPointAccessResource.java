package org.commonjava.aprox.core.rest.access;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

public interface DeployPointAccessResource
    extends SimpleAccessResource
{

    Response createContent( final String name, final String path, final HttpServletRequest request );

}