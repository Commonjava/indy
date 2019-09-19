/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.filer.def.migrate;

import org.apache.commons.io.FileUtils;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.MigrationAction;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.indy.model.core.ArtifactStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Migrate storage from old storage/[type]/[name] directory structure to storage/[packageType]/[type]/[name].
 * Created by jdcasey on 5/12/17.
 */
@ApplicationScoped
public class PackageTypedStorageMigrationAction
        implements MigrationAction
{
    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private DefaultStorageProviderConfiguration config;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public String getId()
    {
        return "package-typed-storage";
    }

    @Override
    public boolean migrate()
            throws IndyLifecycleException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "Disabled." );
        return true;
    }

    private boolean doMigrate()
            throws IndyLifecycleException
    {
        Set<ArtifactStore> stores;
        try
        {
            stores = storeDataManager.getAllArtifactStores();
        }
        catch ( IndyDataException e )
        {
            throw new IndyLifecycleException(
                    "Cannot retrieve list of repositories and groups in order to review storage locations. Reason: %s",
                    e, e.getMessage() );
        }

        File storageRoot = config.getStorageRootDirectory();
        File nfsStorageRoot = config.getNFSStorageRootDirectory();

        int migrations = 0;
        Map<File, File> unmigratedNfs = new HashMap<>();
        for ( ArtifactStore store : stores )
        {
            File old = deprecatedStoragePath( storageRoot, store );
            File migrated = packageTypedStoragePath( storageRoot, store );

            if ( old.exists() )
            {
                logger.info( "Attempting to migrate existing storage from old directory structure: {} "
                                     + "to package-typed structure: {}", old, migrated );

                try
                {
                    if ( migrated.exists() )
                    {
                        FileUtils.copyDirectory( old, migrated );
                        FileUtils.forceDelete( old );
                    }
                    else
                    {
                        FileUtils.moveDirectory( old, migrated );
                    }

                    migrations++;
                }
                catch ( IOException e )
                {
                    throw new IndyLifecycleException( "Failed to migrate: %s to: %s. Reason: %s", e, old, migrated );
                }
            }

            if ( nfsStorageRoot != null )
            {
                File oldNfs = deprecatedStoragePath( nfsStorageRoot, store );
                File migratedNfs = packageTypedStoragePath( nfsStorageRoot, store );
                if ( oldNfs.exists() && !migratedNfs.exists() )
                {
                    unmigratedNfs.put( oldNfs, migratedNfs );
                }
            }
        }

        if ( !unmigratedNfs.isEmpty() )
        {
            StringBuilder sb = new StringBuilder();
            sb.append( "ERROR: Un-migrated directories detected on NFS storage!!!!" );
            sb.append( "\n\nThese directories still use the old <type>/<name> directory format. Indy now supports" );
            sb.append( "\nmultiple package types, and the storage format has changed accordingly. The new format is:" );
            sb.append( "\n\n    <package-type>/<type>/<name>" );
            sb.append( "\n\nPlease migrate these NFS directories manually. For Maven repositories:" );
            sb.append( "\n\n    maven/<type>/<name>" );
            sb.append( "\n\nFor HTTProx repositories (httprox_*):" );
            sb.append( "\n\n    generic-http/<type>/<name>" );
            sb.append( "\n\nThe following directories were detected:\n" );
            unmigratedNfs.forEach( ( o, n ) -> sb.append( "\n    " ).append( o ).append( "  =>  " ).append( n ) );
            sb.append( "\n\n" );

            logger.error( sb.toString() );

            throw new IndyLifecycleException(
                    "Un-migrated NFS directories detected. Indy cannot start until this has been resolved." );
        }

        return migrations > 0;
    }

    private File deprecatedStoragePath( File rootDir, ArtifactStore store )
    {
        return rootDir.toPath()
                      .resolve( Paths.get( store.getType().singularEndpointName() + "-" + store.getName() ) )
                      .toFile();
    }

    private File packageTypedStoragePath( File rootDir, ArtifactStore store )
    {
        return rootDir.toPath()
                      .resolve( Paths.get( store.getPackageType(),
                                           store.getType().singularEndpointName() + "-" + store.getName() ) )
                      .toFile();
    }

    @Override
    public int getMigrationPriority()
    {
        return 90;
    }
}
