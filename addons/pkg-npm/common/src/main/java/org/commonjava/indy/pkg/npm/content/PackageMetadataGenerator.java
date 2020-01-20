/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
import org.commonjava.indy.content.StoreResource;
import org.commonjava.indy.core.content.AbstractMergedContentGenerator;
import org.commonjava.indy.core.content.group.GroupMergeHelper;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.pkg.npm.content.group.PackageMetadataMerger;
import org.commonjava.indy.pkg.npm.model.DistTag;
import org.commonjava.indy.pkg.npm.model.PackageMetadata;
import org.commonjava.indy.pkg.npm.model.VersionMetadata;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

            final List<Transfer> sources = new ArrayList<>(  );

            for ( ArtifactStore member : members )
            {

                logger.debug( "Retrieve raw file from the member store: {}", member );
                final Transfer source = fileManager.retrieveRaw( member, toMergePath, eventMetadata );
                if ( source == null )
                {
                    // Skip to generate for remote, it does not support to get the tgz list from remote registry
                    // and will report the MethodNotAllowedError and then get the remote repo disabled.
                    if ( StoreType.remote == member.getKey().getType() )
                    {
                        continue;
                    }
                    logger.debug( "Package metadata missing in store: {}, try to generate.", member );
                    final Transfer generated = generateFileContent( member, toMergePath, eventMetadata );
                    if ( generated != null )
                    {
                        sources.add( generated );
                    }
                }
                else
                {
                    sources.add( source );
                }
            }

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

    @Override
    public Transfer generateFileContent( final ArtifactStore store, final String path, final EventMetadata eventMetadata )
                    throws IndyWorkflowException
    {
        // metadata merging is something else...don't handle it here.
        if ( StoreType.group == store.getKey().getType() )
        {
            return null;
        }

        if ( !canProcess( path ) )
        {
            return null;
        }

        boolean generated;

        // regardless, we will need this first level of listings. What we do with it will depend on the logic below...
        final String parentPath = Paths.get( path )
                                       .getParent()
                                       .toString();

        List<StoreResource> firstLevel;
        try
        {
            logger.debug( "List first level resources(package tgz list) from path: {}/-", parentPath );
            firstLevel =fileManager.listRaw( store, normalize( parentPath, "-" ) );
        }
        catch ( final IndyWorkflowException e )
        {
            logger.error( String.format( "SKIP: Failed to list .tgz from listing of directory contents for: %s under path: %s/-",
                                         store, parentPath ), e );
            return null;
        }

        String toGenPath = path;
        if ( !path.endsWith( PackageMetadataMerger.METADATA_NAME ) )
        {
            toGenPath = normalize( normalize( parentPath( toGenPath ) ), PackageMetadataMerger.METADATA_NAME );
        }

        logger.info( "Generating package metadata package.json in store: {}", store.getKey() );
        generated = writePackageMetadata( firstLevel, store, toGenPath, eventMetadata );

        logger.debug( "[Result] Generating package.json for store: {}, result: {}", store.getKey(), generated );
        return generated ? fileManager.getTransfer( store, path ) : null;
    }

    private boolean writePackageMetadata( List<StoreResource> firstLevelFiles, ArtifactStore store, String path, EventMetadata eventMetadata ) throws IndyWorkflowException
    {
        logger.debug( "writePackageMetadata, firstLevelFiles:{}, store:{}", firstLevelFiles, store.getKey() );

        // Parse the path of the tar (e.g.: jquery/-/jquery-7.6.1.tgz or @types/jquery/-/jquery-2.2.3.tgz)
        // to get the version, then try to get the version metadata by the path (@scoped/)package/version
        List<String> versionPaths = firstLevelFiles.stream()
                       .map( (res) -> {
                           String tarPath = res.getPath();
                           String[] pathParts = tarPath.split( "/" );
                           if ( tarPath.startsWith( "@" ) )
                           {
                               String scopedName = pathParts[0];
                               String packageName = pathParts[1];
                               String tarName = pathParts[3];
                               return normalize( scopedName, packageName, tarName.substring( packageName.length() + 1, tarName.length() - 4 ) );
                           }
                           else
                           {
                               String packageName = pathParts[0];
                               String tarName = pathParts[2];
                               return normalize( packageName, tarName.substring( packageName.length() + 1, tarName.length() - 4 ) );
                           }
                       } )
                       .sorted()
                       .collect( Collectors.toList());

        if ( versionPaths.size() == 0 )
        {
            return false;
        }

        final Transfer metadataFile = fileManager.getTransfer( store, path );

        final PackageMetadata packageMetadata = new PackageMetadata();
        final IndyObjectMapper mapper = new IndyObjectMapper( true );
        List<String> keywords = new ArrayList<>(  );

        DistTag distTags = new DistTag();
        Map<String, VersionMetadata> versions = new LinkedHashMap<>(  );
        for ( int i = 0; i < versionPaths.size(); i++ )
        {
            String versionPath = versionPaths.get( i );
            logger.debug( "Retrieving the version file {} from store {}", versionPath, store );
            Transfer metaFile = fileManager.retrieveRaw( store, versionPath, eventMetadata );

            if ( metaFile == null )
            {
                //TODO Do we need to get the package.json from .tgz if there
                // is no file in the path package/version ?
                continue;
            }

            try ( InputStream input = metaFile.openInputStream() )
            {
                VersionMetadata versionMetadata = mapper.readValue( input, VersionMetadata.class );

                if ( versionMetadata == null )
                {
                    continue;
                }

                versions.put( versionMetadata.getVersion(), versionMetadata );

                if ( versionMetadata.getKeywords() != null )
                {
                    for ( String keyword : versionMetadata.getKeywords() )
                    {
                        if ( !keywords.contains( keyword ) )
                        {
                            keywords.add( keyword );
                        }
                    }
                }

                // Set couple of attributes based on the latest version metadata
                if ( i == versionPaths.size() - 1 )
                {
                    packageMetadata.setName( versionMetadata.getName() );
                    packageMetadata.setDescription( versionMetadata.getDescription() );
                    packageMetadata.setAuthor( versionMetadata.getAuthor() );
                    packageMetadata.setLicense( versionMetadata.getLicense() );
                    packageMetadata.setRepository( versionMetadata.getRepository() );
                    packageMetadata.setBugs( versionMetadata.getBugs() );
                    distTags.setLatest( versionMetadata.getVersion() );
                }
            }
            catch ( IOException e )
            {
                logger.error( "Get the version metadata error from path {}", versionPath, e );
                throw new IndyWorkflowException( "Get the version metadata error from path {}", versionPath );
            }
        }

        if ( !keywords.isEmpty() )
        {
            packageMetadata.setKeywords( keywords );
        }

        packageMetadata.setVersions( versions );
        packageMetadata.setDistTags( distTags );

        OutputStream stream = null;
        try
        {
            String output = mapper.writeValueAsString( packageMetadata );
            stream = metadataFile.openOutputStream( TransferOperation.GENERATE, true, eventMetadata );
            stream.write( output.getBytes( "UTF-8" ) );
        }
        catch ( IOException e )
        {
            throw new IndyWorkflowException( "Generating package metadata failure in store {}", store.getKey() );
        }
        finally
        {
            closeQuietly( stream );
        }

        logger.debug( "writePackageMetadata, DONE, store: {}", store.getKey() );
        return true;
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
