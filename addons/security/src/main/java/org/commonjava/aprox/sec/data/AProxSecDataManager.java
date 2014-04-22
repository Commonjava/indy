/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.sec.data;

import javax.inject.Inject;

import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.util.ChangeSynchronizer;
import org.commonjava.badgr.data.BadgrDataException;
import org.commonjava.badgr.data.BadgrDataManager;
import org.commonjava.badgr.data.BadgrFactory;
import org.commonjava.badgr.model.Permission;
import org.commonjava.util.logging.Logger;

@javax.enterprise.context.ApplicationScoped
public class AProxSecDataManager
{

    private static final String[] READONLY_PERMS = { Permission.ADMIN, Permission.READ };

    private static final String[] ALL_PERMS = { Permission.ADMIN, Permission.READ, Permission.CREATE };

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private BadgrDataManager userManager;

    @Inject
    private BadgrFactory userFactory;

    @Inject
    private ChangeSynchronizer changeSync;

    public void install()
        throws BadgrDataException
    {
        try
        {
            //            userFactory.setupAdminInformation();

            userManager.storePermission( new Permission( StoreType.repository.name(), Permission.ADMIN ) );

            userManager.storePermission( new Permission( StoreType.group.name(), Permission.ADMIN ) );

            userManager.storePermission( new Permission( StoreType.repository.name(), Permission.READ ) );

            userManager.storePermission( new Permission( StoreType.group.name(), Permission.READ ) );
        }
        catch ( final BadgrDataException e )
        {
            logger.error( "Failed to initialize admin user/privilege data. Reason: {}", e, e.getMessage() );
            throw e;
        }
    }

    public void createStorePermissions( final ArtifactStore store )
    {
        try
        {
            final String[] verbs = store.getDoctype()
                                        .isWritable() ? ALL_PERMS : READONLY_PERMS;

            logger.info( "Creating permissions for new store: {}", store );
            userFactory.createPermissions( store.getDoctype()
                                                .name(), store.getName(), verbs );
        }
        catch ( final BadgrDataException e )
        {
            logger.error( "Failed to create permissions for store: {}. Error: {}", e, store.getKey(), e.getMessage() );
        }
    }

    public void deleteStorePermissions( final StoreType type, final String name )
    {
        try
        {
            logger.info( "\n\n\n\nDeleting permissions for store: {}:{}\n\n\n\n", type.name(), name );
            userManager.deletePermission( Permission.name( type.name(), name, Permission.ADMIN ) );
            userManager.deletePermission( Permission.name( type.name(), name, Permission.READ ) );

            changeSync.setChanged();
        }
        catch ( final BadgrDataException e )
        {
            logger.error( "Failed to remove permissions for deleted store: {}:{}. Error: {}", e, type.name(), name,
                          e.getMessage() );
        }
    }

    public void deleteStorePermissions( final String storeId )
    {
        try
        {
            logger.info( "\n\n\n\nDeleting permissions for group: {}\n\n\n\n", storeId );
            userManager.deletePermission( Permission.name( storeId, Permission.ADMIN ) );
            userManager.deletePermission( Permission.name( storeId, Permission.READ ) );

            changeSync.setChanged();
        }
        catch ( final BadgrDataException e )
        {
            logger.error( "Failed to remove permissions for deleted store: {}. Error: {}", e, storeId, e.getMessage() );
        }
    }

}
