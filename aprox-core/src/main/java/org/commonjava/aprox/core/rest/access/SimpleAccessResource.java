package org.commonjava.aprox.core.rest.access;

import javax.ws.rs.core.Response;

public interface SimpleAccessResource
{

    Response getContent( final String name, final String path );
}
