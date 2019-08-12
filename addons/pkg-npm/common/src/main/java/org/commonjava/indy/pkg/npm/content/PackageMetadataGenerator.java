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
package org.commonjava.indy.pkg.npm.content;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.content.MergedContentAction;
import org.commonjava.indy.core.content.AbstractMergedContentGenerator;
import org.commonjava.indy.core.content.group.GroupMergeHelper;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.pkg.npm.content.group.PackageMetadataMerger;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.maven.spi.type.TypeMapper;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.spi.io.PathGenerator;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static org.commonjava.maven.galley.util.PathUtils.normalize;
import static org.commonjava.maven.galley.util.PathUtils.parentPath;

public class PackageMetadataGenerator
                extends AbstractMergedContentGenerator
{

    @Inject
    private TypeMapper typeMapper;

    @Inject
    private PackageMetadataMerger merger;

    @Inject
    private PathGenerator pathGenerator;

    protected PackageMetadataGenerator()
    {
    }

    public PackageMetadataGenerator( final DirectContentAccess fileManager, final StoreDataManager storeManager,
                                     final TypeMapper typeMapper, final PackageMetadataMerger merger,
                                     final GroupMergeHelper mergeHelper, final NotFoundCache nfc,
                                     final PathGenerator pathGenerator,
                                     final MergedContentAction... mergedContentActions )
    {
        super( fileManager, storeManager, mergeHelper, nfc, mergedContentActions );
        this.typeMapper = typeMapper;
        this.pathGenerator = pathGenerator;
        this.merger = merger;
    }

    @Override
    public Transfer generateGroupFileContent( Group group, List<ArtifactStore> members, String path,
                                              EventMetadata eventMetadata ) throws IndyWorkflowException
    {
        String realPath = path;
        boolean canProcess;
        if ( canProcess( realPath ) )
        {
            canProcess = true;
        }
        else
        {
            realPath = pathGenerator.getPath( new ConcreteResource( LocationUtils.toLocation( group ), path ) );
            canProcess = canProcess( realPath );
        }
        if ( !canProcess )
        {
            return null;
        }

        final Transfer target = fileManager.getTransfer( group, realPath );

        logger.debug( "Working on metadata file: {} (already exists? {})", target, exists( target ) );

        if ( !exists( target ) )
        {
            String toMergePath = realPath;
            if ( !realPath.endsWith( PackageMetadataMerger.METADATA_NAME ) )
            {
                toMergePath = normalize( normalize( parentPath( toMergePath ) ), PackageMetadataMerger.METADATA_NAME );
            }

            final List<Transfer> sources = fileManager.retrieveAllRaw( members, toMergePath, new EventMetadata() );
            final byte[] merged = merger.merge( sources, group, toMergePath );
            if ( merged != null )
            {
                try (OutputStream fos = target.openOutputStream( TransferOperation.GENERATE, true, eventMetadata ))
                {
                    fos.write( merged );
                }
                catch ( final IOException e )
                {
                    throw new IndyWorkflowException( "Failed to write merged metadata to: {}.\nError: {}", e, target,
                                                     e.getMessage() );
                }

                helper.writeMergeInfo( helper.generateMergeInfo( sources ), group, toMergePath );
            }
        }

        if ( target.exists() )
        {
            return target;
        }

        return null;
    }

    private boolean exists( final Transfer transfer )
    {
        return transfer != null && transfer.exists();
    }

    @Override
    public boolean canProcess( String path )
    {
        return path.endsWith( PackageMetadataMerger.METADATA_NAME );
    }

    @Override
    protected String getMergedMetadataName()
    {
        return PackageMetadataMerger.METADATA_NAME;
    }
}
