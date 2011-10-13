/*******************************************************************************
 * Copyright (C) 2011  John Casey
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see 
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.core.conf;

import java.io.File;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;

import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;
import org.commonjava.web.config.section.ConfigurationSectionListener;

@SectionName( ConfigurationSectionListener.DEFAULT_SECTION )
@Alternative
@Named( "unused" )
public class DefaultProxyConfiguration
    implements ProxyConfiguration
{

    protected static final File DEFAULT_REPO_ROOT_DIR =
        new File( "/var/lib/artifact-proxy/repositories" );

    private File repositoryRootDirectory = DEFAULT_REPO_ROOT_DIR;

    @Override
    public File getRepositoryRootDirectory()
    {
        return repositoryRootDirectory;
    }

    @ConfigName( "repository.root.dir" )
    public void setRepositoryRootDirectory( final File repositoryRootDirectory )
    {
        this.repositoryRootDirectory = repositoryRootDirectory;
    }

}
