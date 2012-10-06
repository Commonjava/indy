package org.commonjava.aprox.rest.admin;

import javax.ws.rs.core.Response;

public interface MaintenanceResource
{

    Response rescan( String type, String store );

    Response rescanAll();

    Response delete( String type, String store, String path );

    Response deleteAll( String path );

}
