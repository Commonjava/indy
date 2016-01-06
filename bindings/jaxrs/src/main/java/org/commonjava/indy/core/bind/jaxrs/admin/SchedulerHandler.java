package org.commonjava.indy.core.bind.jaxrs.admin;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.util.ResponseUtils;
import org.commonjava.indy.core.ctl.SchedulerController;
import org.commonjava.indy.core.expire.Expiration;
import org.commonjava.indy.core.expire.ExpirationSet;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.util.ApplicationContent;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.throwError;

@Path( "/api/admin/schedule" )
@Produces( ApplicationContent.application_json )
public class SchedulerHandler
        implements IndyResources
{
    @Inject
    private SchedulerController controller;

    @Path( "store/{type}/{name}/disable-timeout" )
    @GET
    public Expiration getStoreDisableTimeout( @PathParam( "type" ) String storeType,
                                              @PathParam( "name" ) String storeName )
    {
        StoreKey storeKey = new StoreKey( StoreType.get( storeType ), storeName );
        Expiration timeout = null;
        try
        {
            timeout = controller.getStoreDisableTimeout( storeKey );
        }
        catch ( IndyWorkflowException e )
        {
            throwError( e );
        }

        if ( timeout == null )
        {
            throw new WebApplicationException( Response.Status.NOT_FOUND );
        }

        return timeout;
    }

    @Path( "store/all/disable-timeout" )
    @GET
    public ExpirationSet getDisabledStores()
    {
        try
        {
            return controller.getDisabledStores();
        }
        catch ( IndyWorkflowException e )
        {
            throwError( e );
        }

        throw new WebApplicationException( "Impossible Error", 500 );
    }
}
