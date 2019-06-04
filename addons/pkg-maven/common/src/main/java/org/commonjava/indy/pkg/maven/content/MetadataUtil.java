package org.commonjava.indy.pkg.maven.content;

import static org.commonjava.indy.pkg.maven.content.group.MavenMetadataMerger.METADATA_NAME;
import static org.commonjava.maven.galley.util.PathUtils.normalize;
import static org.commonjava.maven.galley.util.PathUtils.parentPath;

public class MetadataUtil
{
    /**
     * Get the path/to/metadata.xml given a pom or jar file path.
     */
    public static String getMetadataPath( String pomPath )
    {
        final String versionPath = normalize( parentPath( pomPath ) );
        return normalize( normalize( parentPath( versionPath ) ), METADATA_NAME );
    }
}
