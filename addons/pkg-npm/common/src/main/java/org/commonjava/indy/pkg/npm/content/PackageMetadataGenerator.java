/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.maven.parse.XMLInfrastructure;
import org.commonjava.maven.galley.maven.spi.type.TypeMapper;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.maven.galley.util.PathUtils.normalize;
import static org.commonjava.maven.galley.util.PathUtils.parentPath;

public class PackageMetadataGenerator
                extends AbstractMergedContentGenerator
{

    @Inject
    private TypeMapper typeMapper;

    @Inject
    private PackageMetadataMerger merger;

    protected PackageMetadataGenerator()
    {
    }

    public PackageMetadataGenerator( final DirectContentAccess fileManager, final StoreDataManager storeManager,
                                     final XMLInfrastructure xml, final TypeMapper typeMapper,
                                     final PackageMetadataMerger merger, final GroupMergeHelper mergeHelper,
                                     final NotFoundCache nfc, final MergedContentAction... mergedContentActions )
    {
        super( fileManager, storeManager, mergeHelper, nfc, mergedContentActions );
        this.typeMapper = typeMapper;
        this.merger = merger;
    }

    @Override
    public Transfer generateGroupFileContent( Group group, List<ArtifactStore> members, String path,
                                              EventMetadata eventMetadata ) throws IndyWorkflowException
    {
        {
            if ( !canProcess( path ) )
            {
                return null;
            }

            final Transfer target = fileManager.getTransfer( group, path );

            logger.debug( "Working on metadata file: {} (already exists? {})", target,
                          target != null && target.exists() );

            if ( !target.exists() )
            {
                String toMergePath = path;
                if ( !path.endsWith( PackageMetadataMerger.METADATA_NAME ) )
                {
                    toMergePath = normalize( normalize( parentPath( toMergePath ) ),
                                             PackageMetadataMerger.METADATA_NAME );
                }

                final List<Transfer> sources = fileManager.retrieveAllRaw( members, toMergePath, new EventMetadata() );
                final byte[] merged = merger.merge( sources, group, toMergePath );
                if ( merged != null )
                {
                    OutputStream fos = null;
                    try
                    {
                        fos = target.openOutputStream( TransferOperation.GENERATE, true, eventMetadata );
                        fos.write( merged );

                    }
                    catch ( final IOException e )
                    {
                        throw new IndyWorkflowException( "Failed to write merged metadata to: {}.\nError: {}", e,
                                                         target, e.getMessage() );
                    }
                    finally
                    {
                        closeQuietly( fos );
                    }

                    helper.writeMergeInfo( merged, sources, group, toMergePath );
                }
            }

            if ( target.exists() )
            {
                return target;
            }

            return null;
        }
    }

    @Override
    public boolean canProcess( String path )
    {
        if ( path.endsWith( PackageMetadataMerger.METADATA_NAME ) )
        {
            return true;
        }
        return false;
    }

    @Override
    protected String getMergedMetadataName()
    {
        return PackageMetadataMerger.METADATA_NAME;
    }
}
