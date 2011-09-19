package org.commonjava.web.maven.proxy.fixture;

import java.io.File;

import javax.enterprise.inject.Produces;

import org.commonjava.web.maven.proxy.conf.DefaultProxyConfiguration;
import org.commonjava.web.maven.proxy.conf.ProxyConfiguration;

public class ProxyConfigProvider
{

    public static final String REPO_ROOT_DIR = "repo.root.dir";

    @Produces
    public ProxyConfiguration getProxyConfiguration()
    {
        DefaultProxyConfiguration config = new DefaultProxyConfiguration();

        config.setRepositoryRootDirectory( new File( System.getProperty( REPO_ROOT_DIR ),
                                                     "target/repo-downloads" ) );

        return config;
    }

}
