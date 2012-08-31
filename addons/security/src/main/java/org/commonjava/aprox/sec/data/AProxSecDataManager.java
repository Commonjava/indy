package org.commonjava.aprox.sec.data;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.auth.couch.data.UserDataException;
import org.commonjava.auth.couch.data.UserDataManager;
import org.commonjava.auth.couch.inject.UserData;
import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.couch.rbac.Permission;
import org.commonjava.couch.util.ChangeSynchronizer;
import org.commonjava.util.logging.Logger;

@Singleton
public class AProxSecDataManager
{

    private static final String[] READONLY_PERMS = { Permission.ADMIN, Permission.READ };

    private static final String[] ALL_PERMS = { Permission.ADMIN, Permission.READ, Permission.CREATE };

    private final Logger logger = new Logger( getClass() );

    @Inject
    private UserDataManager userManager;

    @Inject
    @UserData
    private CouchDBConfiguration userConfig;

    @Inject
    private ChangeSynchronizer changeSync;

    public void install()
    {
        try
        {
            userManager.install();
            userManager.setupAdminInformation();

            userManager.storePermission( new Permission( StoreType.repository.name(), Permission.ADMIN ) );

            userManager.storePermission( new Permission( StoreType.group.name(), Permission.ADMIN ) );

            userManager.storePermission( new Permission( StoreType.repository.name(), Permission.READ ) );

            userManager.storePermission( new Permission( StoreType.group.name(), Permission.READ ) );
        }
        catch ( final UserDataException e )
        {
            logger.error( "Failed to initialize admin user/privilege information in database: %s. Reason: %s", e,
                          userConfig.getDatabaseUrl(), e.getMessage() );
        }
    }

    public void createStorePermissions( final ArtifactStore store )
    {
        try
        {
            final String[] verbs = store.getDoctype()
                                        .isWritable() ? ALL_PERMS : READONLY_PERMS;

            logger.info( "Creating permissions for new store: %s", store );
            userManager.createPermissions( store.getDoctype()
                                                .name(), store.getName(), verbs );
        }
        catch ( final UserDataException e )
        {
            logger.error( "Failed to create permissions for store: %s. Error: %s", e, store.getKey(), e.getMessage() );
        }
    }

    public void deleteStorePermissions( final StoreType type, final String name )
    {
        try
        {
            logger.info( "\n\n\n\nDeleting permissions for store: %s:%s\n\n\n\n", type.name(), name );
            userManager.deletePermission( Permission.name( type.name(), name, Permission.ADMIN ) );
            userManager.deletePermission( Permission.name( type.name(), name, Permission.READ ) );

            changeSync.setChanged();
        }
        catch ( final UserDataException e )
        {
            logger.error( "Failed to remove permissions for deleted store: %s:%s. Error: %s", e, type.name(), name,
                          e.getMessage() );
        }
    }

    public void deleteStorePermissions( final String storeId )
    {
        try
        {
            logger.info( "\n\n\n\nDeleting permissions for group: %s\n\n\n\n", storeId );
            userManager.deletePermission( Permission.name( storeId, Permission.ADMIN ) );
            userManager.deletePermission( Permission.name( storeId, Permission.READ ) );

            changeSync.setChanged();
        }
        catch ( final UserDataException e )
        {
            logger.error( "Failed to remove permissions for deleted store: %s. Error: %s", e, storeId, e.getMessage() );
        }
    }

}
