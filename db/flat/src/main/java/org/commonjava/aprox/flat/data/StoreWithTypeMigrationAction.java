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

import java.io.IOException;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Named( "store-with-type-migration" )
public class StoreWithTypeMigrationAction
    implements MigrationAction
{

    private static final String TYPE_ATTR = "type";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager data;

    public StoreWithTypeMigrationAction()
    {
    }

    public StoreWithTypeMigrationAction( final StoreDataManager data )
    {
        this.data = data;
    }

    @Override
    public String getId()
    {
        return "Store type-attribute data migrator";
    }

    @Override
    public boolean migrate()
    {
        if ( !( data instanceof DataFileStoreDataManager ) )
        {
            logger.info( "Store manager: {} is not based on DataFile's. Skipping migration.", data.getClass()
                                                                                                  .getName() );
            return true;
        }

        final DataFileStoreDataManager data = (DataFileStoreDataManager) this.data;

        final DataFile basedir = data.getFileManager()
                                     .getDataFile( DataFileStoreDataManager.APROX_STORE );

        if ( !basedir.exists() )
        {
            logger.info( "Base directory: {} does not exist. Skipping store type-attribute migration.",
                         basedir.getPath() );
            return false;
        }

        final ChangeSummary summary =
            new ChangeSummary( ChangeSummary.SYSTEM_USER,
                               "Migrating store definitions to incorporate new type attribute." );

        final ObjectMapper om = new ObjectMapper();
        boolean changed = false;
        for ( final StoreType type : StoreType.values() )
        {
            final DataFile dir = basedir.getChild( type.singularEndpointName() );
            if ( !dir.exists() )
            {
                continue;
            }

            logger.info( "Scanning {} for definitions to migrate...", dir.getPath() );
            for ( final String fname : dir.list() )
            {
                if ( fname.endsWith( ".json" ) )
                {
                    final DataFile jsonFile = dir.getChild( fname );
                    try
                    {
                        logger.info( "Migrating definition {}", jsonFile.getPath() );
                        String json = jsonFile.readString();

                        final JsonNode tree = om.readTree( json );
                        final JsonNode field = tree.get( TYPE_ATTR );
                        if ( field == null )
                        {
                            logger.info( "Patching store definition: {} with type attribute: {}", jsonFile.getPath(),
                                         type.singularEndpointName() );

                            ( (ObjectNode) tree ).put( TYPE_ATTR, type.singularEndpointName() );

                            json = om.writeValueAsString( tree );
                            jsonFile.writeString( json, summary );
                            changed = true;
                        }
                    }
                    catch ( final IOException e )
                    {
                        throw new RuntimeException( "Failed to migrate artifact-store definition for: "
                            + jsonFile.getPath() + ". Reason: " + e.getMessage(), e );
                    }
                }
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
        return 86;
    }

}
