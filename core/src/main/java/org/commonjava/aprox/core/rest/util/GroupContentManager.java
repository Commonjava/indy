package org.commonjava.aprox.core.rest.util;

import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.StoreDataManager;
import org.commonjava.aprox.core.io.StorageItem;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.rest.AproxWorkflowException;
import org.commonjava.aprox.core.rest.util.retrieve.GroupHandlerChain;
import org.commonjava.util.logging.Logger;

@Singleton
public class GroupContentManager
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private GroupHandlerChain handlerChain;

    public StorageItem retrieve( final String name, final String path )
        throws AproxWorkflowException
    {
        // TODO:
        // 1. directory request (ends with "/")...browse somehow??
        // 2. empty path (directory request for proxy root)

        List<? extends ArtifactStore> stores = null;
        Group group = null;

        try
        {
            group = storeManager.getGroup( name );
            if ( group == null )
            {
                return null;
            }
            else
            {
                stores = storeManager.getOrderedConcreteStoresInGroup( name );
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to retrieve repository-group information: %s. Reason: %s", e, name, e.getMessage() );
            throw new AproxWorkflowException( Response.serverError()
                                                      .build(),
                                              "Failed to retrieve repository-group information: %s. Reason: %s", e,
                                              name, e.getMessage() );
        }

        // logger.info( "Download: %s\nFrom: %s", path, stores );
        final StorageItem item = handlerChain.retrieve( group, stores, path );
        if ( item == null || item.isDirectory() )
        {
            return null;
        }

        return item;
    }

    public DeployPoint store( final String name, final String path, final InputStream stream )
        throws AproxWorkflowException
    {
        List<? extends ArtifactStore> stores = null;
        Group group = null;

        try
        {
            group = storeManager.getGroup( name );
            if ( group == null )
            {
                return null;
            }
            else
            {
                stores = storeManager.getOrderedConcreteStoresInGroup( name );
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to retrieve repository-group information: %s. Reason: %s", e, name, e.getMessage() );
            throw new AproxWorkflowException( Response.serverError()
                                                      .build(),
                                              "Failed to retrieve repository-group information: %s. Reason: %s", e,
                                              name, e.getMessage() );
        }

        return handlerChain.store( group, stores, path, stream );
    }

}
