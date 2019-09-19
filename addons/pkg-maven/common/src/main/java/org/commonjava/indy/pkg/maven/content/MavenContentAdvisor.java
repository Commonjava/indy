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

import org.commonjava.indy.spi.pkg.ContentAdvisor;
import org.commonjava.indy.spi.pkg.ContentQuality;
import org.commonjava.atlas.maven.ident.util.ArtifactPathInfo;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 */
@ApplicationScoped
public class MavenContentAdvisor
        implements ContentAdvisor
{
    @Inject
    private SpecialPathManager specialPathManager;

    protected MavenContentAdvisor(){}

    public MavenContentAdvisor( SpecialPathManager specialPathManager )
    {
        this.specialPathManager = specialPathManager;
    }

    @Override
    public ContentQuality getContentQuality( String path )
    {
        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        if ( pathInfo != null )
        {
            final SpecialPathInfo info = specialPathManager.getSpecialPathInfo( path );
            if ( info != null && info.isMetadata() )
            {
                return ContentQuality.METADATA;
            }

            if ( pathInfo.isSnapshot() )
            {
                return ContentQuality.SNAPSHOT;
            }

            return ContentQuality.RELEASE;
        }
        //FIXME: needs further think here if null and RELEASE have the same meaning?
        return null;
    }
}
