package org.commonjava.aprox.core.rest.admin;

import javax.ws.rs.core.Response;

public interface GroupAdminResource
{

    Response create();

    Response store( final String name );

    Response getAll();

    Response get( final String name );

    Response delete( final String name );

}