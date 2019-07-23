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
package org.commonjava.indy.flat.data;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.MigrationAction;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.commonjava.indy.flat.data.DataFileStoreUtils.INDY_STORE;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;

@Named( "legacy-storedb-migration" )
public class LegacyDataMigrationAction
        implements MigrationAction
{

    public static final String LEGACY_MIGRATION = "legacy-migration";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private DataFileManager dataFileManager;

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private IndyObjectMapper objectMapper;

    protected LegacyDataMigrationAction(){}

    public LegacyDataMigrationAction( final DataFileManager dataFileManager,
                                      final DataFileStoreDataManager dataFileStoreDataManager,
                                      final IndyObjectMapper mapper )
    {
        this.dataFileManager = dataFileManager;
        storeDataManager = dataFileStoreDataManager;
        objectMapper = mapper;
    }

    @Override
    public String getId()
    {
        return "Legacy store-definition data migrator";
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
        if ( !( storeDataManager instanceof DataFileStoreDataManager ) )
        {
            logger.info( "Store manager: {} is not based on DataFile's. Skipping migration.",
                         storeDataManager.getClass().getName() );
            return false;
        }

        final DataFile basedir = dataFileManager.getDataFile( INDY_STORE );

        final ChangeSummary summary =
                new ChangeSummary( ChangeSummary.SYSTEM_USER, "Migrating legacy store definitions." );

        if ( !basedir.exists() )
        {
            return false;
        }

        StoreType[] storeTypes = StoreType.values();

        final String[] dirs = basedir.list();
        if ( dirs == null || dirs.length < 1 )
        {
            return false;
        }

        Map<String, String> migrationCandidates = new HashMap<>();

        //noinspection ConstantConditions
        Stream.of( storeTypes )
              .forEach( type -> {
                  File[] files = basedir.getDetachedFile()
                                        .toPath()
                                        .resolve( type.singularEndpointName() )
                                        .toFile()
                                        .listFiles( ( dir, fname ) -> fname.endsWith( ".json" ) );
                  if ( files != null )
                  {
                      Stream.of( files )
                            .forEach( ( f ) ->
                                      {
                                          String src = Paths.get( type.singularEndpointName(), f.getName() )
                                                            .toString();

                                          String target =
                                                  Paths.get( MAVEN_PKG_KEY, type.singularEndpointName(),
                                                             f.getName() ).toString();

                                          migrationCandidates.put( src, target );
                                      } );
                  }
              } );

        boolean changed = false;
        for ( Map.Entry<String, String> entry : migrationCandidates.entrySet() )
        {
            DataFile src = dataFileManager.getDataFile( INDY_STORE, entry.getKey() );
            DataFile target = dataFileManager.getDataFile( INDY_STORE, entry.getValue() );
            if ( target.exists() )
            {
                continue;
            }

            DataFile targetDir = target.getParent();
            if ( !targetDir.exists() && !targetDir.mkdirs() )
            {
                throw new IndyLifecycleException( "Cannot make directory: %s.", targetDir.getPath() );
            }
            else if ( !targetDir.isDirectory() )
            {
                throw new IndyLifecycleException( "Not a directory: %s.", targetDir.getPath() );
            }

            try
            {
                logger.info( "Migrating definition {}", src.getPath() );
                final String json = src.readString();

                final String migrated = objectMapper.patchLegacyStoreJson( json );
                target.writeString( migrated, summary );
                changed = true;
            }
            catch ( final IOException e )
            {
                throw new IndyLifecycleException(
                        "Failed to migrate artifact-store definition from: %s to: %s. Reason: %s", e, src, target,
                        e.getMessage(), e );
            }
        }

        if ( changed )
        {
            try
            {
                storeDataManager.reload();
            }
            catch ( IndyDataException e )
            {
                throw new IndyLifecycleException( "Failed to reload migrated store definitions: %s", e,
                                                  e.getMessage() );
            }
        }

        return changed;
    }

    @Override
    public int getMigrationPriority()
    {
        return 99;
    }

}
