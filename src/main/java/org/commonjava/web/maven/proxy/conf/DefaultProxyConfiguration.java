package org.commonjava.web.maven.proxy.conf;

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

    @ConfigName( "repositoy.root.dir" )
    public void setRepositoryRootDirectory( final File repositoryRootDirectory )
    {
        this.repositoryRootDirectory = repositoryRootDirectory;
    }

}
