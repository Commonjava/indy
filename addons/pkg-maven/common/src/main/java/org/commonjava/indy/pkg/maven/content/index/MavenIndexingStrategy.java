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
package org.commonjava.indy.pkg.maven.content.index;

import org.commonjava.indy.content.index.PackageIndexingStrategy;
import org.commonjava.indy.util.PathUtils;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;

@ApplicationScoped
@Named
public class MavenIndexingStrategy
        implements PackageIndexingStrategy
{
    @Inject
    private SpecialPathManager specialPathManager;

    protected MavenIndexingStrategy(){}

    public MavenIndexingStrategy( SpecialPathManager specialPathManager )
    {
        this.specialPathManager = specialPathManager;
    }

    @Override
    public String getPackageType()
    {
        return PKG_TYPE_MAVEN;
    }

    @Override
    public String getIndexPath( final String rawPath )
    {
        final SpecialPathInfo info = specialPathManager.getSpecialPathInfo( rawPath );
        if ( info == null || !info.isMergable() )
        {
            return PathUtils.getCurrentDirPath( rawPath );
        }
        else
        {
            return rawPath;
        }
    }
}
