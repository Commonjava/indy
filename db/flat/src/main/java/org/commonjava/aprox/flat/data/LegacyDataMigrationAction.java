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

import org.commonjava.aprox.action.MigrationAction;
import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.subsys.datafile.DataFile;
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
    private StoreDataManager data;

    @Override
    public String getId()
    {
        return "Legacy store-definition data migrator";
    }

    @Override
    public boolean migrate()
    {
        if ( !( data instanceof DataFileStoreDataManager ) )
        {
            return true;
        }

        final DataFileStoreDataManager data = (DataFileStoreDataManager) this.data;

        final DataFile basedir = data.getFileManager()
                                     .getDataFile( DataFileStoreDataManager.APROX_STORE );
        final ChangeSummary summary =
            new ChangeSummary( ChangeSummary.SYSTEM_USER, "Migrating legacy store definitions." );

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
            final DataFile dir = basedir.getChild( name );
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

                final DataFile newDir = basedir.getChild( newName );
                dir.renameTo( newDir, summary );

                changed = true;
            }
        }

        try
        {
            data.reload();

            final List<HostedRepository> hosted = data.getAllHostedRepositories();
            for ( final HostedRepository repo : hosted )
            {
                data.storeHostedRepository( repo, summary );
            }

            final List<RemoteRepository> remotes = data.getAllRemoteRepositories();
            for ( final RemoteRepository repo : remotes )
            {
                data.storeRemoteRepository( repo, summary );
            }

            data.reload();
        }
        catch ( final AproxDataException e )
        {
            throw new RuntimeException( "Failed to reload artifact-store definitions: " + e.getMessage(), e );
        }

        return changed;
    }

    @Override
    public int getPriority()
    {
        return 85;
    }

}
