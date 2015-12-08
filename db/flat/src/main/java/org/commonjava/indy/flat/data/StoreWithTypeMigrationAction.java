/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.flat.data;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.indy.action.MigrationAction;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named( "store-with-type-migration" )
public class StoreWithTypeMigrationAction
    implements MigrationAction
{

    public static final String STORE_TYPE_MIGRATION = "store-type-migration";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager data;

    @Inject
    private IndyObjectMapper mapper;

    public StoreWithTypeMigrationAction()
    {
    }

    public StoreWithTypeMigrationAction( final StoreDataManager data, final IndyObjectMapper mapper )
    {
        this.data = data;
        this.mapper = mapper;
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
                        final String json = jsonFile.readString();

                        final String migrated = mapper.patchLegacyStoreJson( json );
                        if ( !json.equals( migrated ) )
                        {
                            jsonFile.writeString( migrated, summary );
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
                data.storeArtifactStore( repo, summary, false, true,
                                         new EventMetadata().set( StoreDataManager.EVENT_ORIGIN, STORE_TYPE_MIGRATION ) );
            }

            final List<RemoteRepository> remotes = data.getAllRemoteRepositories();
            for ( final RemoteRepository repo : remotes )
            {
                data.storeArtifactStore( repo, summary, false, true,
                                         new EventMetadata().set( StoreDataManager.EVENT_ORIGIN, STORE_TYPE_MIGRATION ) );
            }

            data.reload();
        }
        catch ( final IndyDataException e )
        {
            throw new RuntimeException( "Failed to reload artifact-store definitions: " + e.getMessage(), e );
        }

        return changed;
    }

    @Override
    public int getMigrationPriority()
    {
        return 86;
    }

}
