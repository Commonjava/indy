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
package org.commonjava.indy.content;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.commonjava.indy.model.core.PathStyle;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.indy.util.PathUtils;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.spi.io.PathGenerator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;

import static org.commonjava.indy.model.core.PathStyle.hashed;

/**
 * {@link PathGenerator} implementation that assumes the locations it sees will be {@link KeyedLocation}, and translates them into storage locations
 * on disk for related content.
 */
@Default
@ApplicationScoped
public class IndyPathGenerator
    implements PathGenerator
{

    @Override
    public String getFilePath( final ConcreteResource resource )
    {
        final KeyedLocation kl = (KeyedLocation) resource.getLocation();
        final StoreKey key = kl.getKey();

        final String name = key.getType()
                               .name() + "-" + key.getName();

        String path = resource.getPath();
        if ( hashed == kl.getAttribute( LocationUtils.PATH_STYLE, PathStyle.class ) )
        {
            String digest = DigestUtils.sha256Hex( path );
            String ext = FilenameUtils.getExtension( path );
            path = String.format( "%s/%s/%s.%s", digest.substring( 0, 2 ), digest.substring( 2, 4 ), digest, ext );
        }
        // else it's plain and we leave it alone.

        return PathUtils.join( name, path );
    }

}
