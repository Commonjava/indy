package org.commonjava.aprox.core.rest.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.core.rest.util.retrieve.GroupHandlerChain;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.GroupContentManager;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.util.logging.Logger;

@javax.enterprise.context.ApplicationScoped
public class DefaultGroupContentManager
    implements GroupContentManager
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private GroupHandlerChain handlerChain;

    /* (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.GroupContentManager#retrieve(java.lang.String, java.lang.String)
     */
    @Override
    public Transfer retrieve( final String name, final String path )
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
        final Transfer item = handlerChain.retrieve( group, stores, path );
        if ( item == null || item.isDirectory() )
        {
            return null;
        }

        return item;
    }

    /* (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.GroupContentManager#store(java.lang.String, java.lang.String, java.io.InputStream)
     */
    @Override
    public Transfer store( final String name, final String path, final InputStream stream )
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

    @Override
    public boolean delete( final String name, final String path )
        throws AproxWorkflowException, IOException
    {
        List<? extends ArtifactStore> stores = null;
        Group group = null;

        try
        {
            group = storeManager.getGroup( name );
            if ( group == null )
            {
                return false;
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

        return handlerChain.delete( group, stores, path );
    }

}
