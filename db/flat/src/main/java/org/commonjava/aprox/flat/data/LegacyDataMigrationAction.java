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
package org.commonjava.aprox.flat.data;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.action.start.MigrationAction;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.subsys.flatfile.conf.FlatFile;
import org.commonjava.aprox.subsys.flatfile.conf.FlatFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named( "legacy-storedb-migration" )
public class LegacyDataMigrationAction
    implements MigrationAction
{

    private static final String LEGACY_HOSTED_REPO_PREFIX = "deploy_point";

    private static final String LEGACY_REMOTE_REPO_PREFIX = "repository";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private FlatFileManager fileManager;

    @Inject
    private FlatFileStoreDataManager data;

    @Override
    public String getId()
    {
        return "Legacy storage-location migrator";
    }

    @Override
    public boolean execute()
    {
        final FlatFile basedir = fileManager.getDataFile( FlatFileStoreDataManager.APROX_STORE );
        if ( !basedir.exists() )
        {
            return false;
        }

        final String[] dirs = basedir.list();
        if ( dirs == null || dirs.length < 1 )
        {
            return false;
        }

        boolean changed = false;
        for ( final String name : dirs )
        {
            final FlatFile dir = basedir.getChild( name );
            String newName = null;
            if ( name.startsWith( LEGACY_HOSTED_REPO_PREFIX ) )
            {
                newName = StoreType.hosted.singularEndpointName() + name.substring( LEGACY_HOSTED_REPO_PREFIX.length() );
            }
            else if ( name.startsWith( LEGACY_REMOTE_REPO_PREFIX ) )
            {
                newName = StoreType.remote.singularEndpointName() + name.substring( LEGACY_REMOTE_REPO_PREFIX.length() );
            }

            if ( newName != null )
            {
                logger.info( "Migrating storage: '{}' to '{}'", name, newName );

                final FlatFile newDir = basedir.getChild( newName );
                dir.renameTo( newDir );

                changed = true;
            }
        }

        try
        {
            data.reload();

            final List<HostedRepository> hosted = data.getAllHostedRepositories();
            for ( final HostedRepository repo : hosted )
            {
                data.storeHostedRepository( repo );
            }

            final List<RemoteRepository> remotes = data.getAllRemoteRepositories();
            for ( final RemoteRepository repo : remotes )
            {
                data.storeRemoteRepository( repo );
            }

            data.reload();
        }
        catch ( final ProxyDataException e )
        {
            throw new RuntimeException( "Failed to reload artifact-store definitions: " + e.getMessage(), e );
        }

        return changed;

    }

}
