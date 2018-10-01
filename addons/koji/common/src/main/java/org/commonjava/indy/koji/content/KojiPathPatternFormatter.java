package org.commonjava.indy.koji.content;

import com.redhat.red.build.koji.model.xmlrpc.KojiArchiveInfo;
import org.commonjava.indy.koji.conf.IndyKojiConfig;
import org.commonjava.indy.koji.util.KojiUtils;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
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

    public Set<String> getPatterns( ArtifactRef artifactRef, List<KojiArchiveInfo> archives )
    {
        Set<String> patterns = new HashSet<>();
        for ( KojiArchiveInfo a : archives )
        {
            if ( !kojiUtils.isVersionSignatureAllowedWithVersion( artifactRef.getVersionStringRaw() ) )
            {
                continue;
            }
            String pattern = getPatternString( artifactRef, a );
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
        String pattern = gId.replace( '.', '/' ) + "/" + artiId + "/" + ver + "/" + a.getFilename();
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
