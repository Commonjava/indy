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
package org.commonjava.indy.koji.content;

import com.redhat.red.build.koji.model.xmlrpc.KojiArchiveInfo;
import org.apache.commons.lang.StringUtils;
import org.commonjava.atlas.maven.ident.ref.ArtifactRef;
import org.commonjava.atlas.maven.ident.ref.SimpleProjectRef;
import org.commonjava.indy.koji.util.KojiUtils;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.maven.galley.TransferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.commonjava.indy.pkg.maven.content.group.MavenMetadataMerger.METADATA_NAME;
import static org.commonjava.maven.galley.maven.util.ArtifactPathUtils.formatMetadataPath;

/** Common logic used to set / correct path masks for koji repositories. */
@ApplicationScoped
public class KojiPathPatternFormatter
{
    @Inject
    private KojiUtils kojiUtils;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public Set<String> getPatterns( final StoreKey inStore, ArtifactRef artifactRef, List<KojiArchiveInfo> archives )
    {
        return getPatterns( inStore, artifactRef, archives, false );
    }

    public Set<String> getPatterns( final StoreKey inStore, ArtifactRef artifactRef, List<KojiArchiveInfo> archives, boolean skipVersionTest )
    {
        Set<String> patterns = new HashSet<>();
        for ( KojiArchiveInfo a : archives )
        {
            if ( !inStore.getPackageType().equals( a.getBuildType() ) )
            {
                logger.info( "Discarding non-{} archive from path patterns: {}", inStore.getPackageType(), a );
                continue;
            }

            ArtifactRef ar = a.asArtifact();

            if ( !skipVersionTest && !kojiUtils.isVersionSignatureAllowedWithVersion( a.getVersion() ) )
            {
                logger.warn(
                        "Cannot use Koji archive for path_mask_patterns: {}. Version '{}' is not allowed from Koji.", a,
                        a.getVersion() );
                continue;
            }

            String pattern = getPatternString( ar, a );

            if ( !skipVersionTest && !kojiUtils.isVersionSignatureAllowedWithVersion( a.getVersion() ) )
            {
                logger.warn(
                        "Cannot use Koji archive for path_mask_patterns: {}. Version '{}' is not allowed from Koji.", a,
                        a.getVersion() );
                continue;
            }

            if ( pattern != null )
            {
                patterns.add( pattern );
            }
        }

        if ( !patterns.isEmpty() )
        {
            String meta = getMetaString( artifactRef ); // Add metadata.xml to path mask patterns
            if ( meta != null )
            {
                patterns.add( meta );
            }
        }
        return patterns;
    }

    private String getPatternString( ArtifactRef artifact, KojiArchiveInfo a )
    {
        String gId = a.getGroupId();
        String artiId = a.getArtifactId();
        String ver = a.getVersion();

        if ( gId == null || artiId == null || ver == null )
        {
            logger.trace( "Pattern ignored, gId: {}, artiId: {}, ver: {}", gId, artiId, ver );
            return null;
        }

        // NOTE: This is not completely precise, but the trade-off in speed should make it worthwhile.
        String pattern = "r|" + StringUtils.replace( gId, ".", "\\/" ) + "\\/.+\\/" + ver + "\\/.+|";
        logger.trace( "Pattern: {}", pattern );

        return pattern;
    }

    private String getMetaString( ArtifactRef artifact )
    {
        String gId = artifact.getGroupId();
        String artiId = artifact.getArtifactId();

        if ( gId == null || artiId == null )
        {
            logger.trace( "Meta ignored, gId: {}, artiId: {}", gId, artiId );
            return null;
        }
        String meta = null;
        try
        {
            meta = formatMetadataPath( new SimpleProjectRef( gId, artiId ), METADATA_NAME );
            logger.trace( "Meta: {}", meta );
        }
        catch ( TransferException e )
        {
            logger.error( "Format metadata path failed", e );
        }
        return meta;
    }

}
