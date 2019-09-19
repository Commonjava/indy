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
