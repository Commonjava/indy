/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.core.conf;

import java.io.File;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;

import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.couch.conf.DefaultCouchDBConfiguration;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;
import org.commonjava.web.config.section.ConfigurationSectionListener;

@SectionName( ConfigurationSectionListener.DEFAULT_SECTION )
@Alternative
@Named( "unused" )
public class DefaultProxyConfiguration
    extends DefaultCouchDBConfiguration
    implements ProxyConfiguration
{

    protected static final File DEFAULT_REPO_ROOT_DIR =
        new File( "/var/lib/artifact-proxy/repositories" );

    private File repositoryRootDirectory = DEFAULT_REPO_ROOT_DIR;

    private CouchDBConfiguration dbConfig;

    public DefaultProxyConfiguration()
    {}

    public DefaultProxyConfiguration( final String dbUrl )
    {
        setDatabaseUrl( dbUrl );
    }

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

    @Override
    public synchronized CouchDBConfiguration getDatabaseConfig()
    {
        if ( dbConfig == null )
        {
            dbConfig = new DefaultCouchDBConfiguration( getDatabaseUrl(), getMaxConnections() );
        }

        return dbConfig;
    }
}
