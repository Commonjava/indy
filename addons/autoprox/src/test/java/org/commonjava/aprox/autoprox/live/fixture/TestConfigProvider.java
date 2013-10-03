package org.commonjava.aprox.autoprox.live.fixture;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.commonjava.aprox.autoprox.conf.AutoProxConfiguration;
import org.commonjava.aprox.autoprox.conf.AutoProxModel;
import org.commonjava.aprox.conf.AproxConfiguration;
import org.commonjava.aprox.core.conf.DefaultAproxConfiguration;
import org.commonjava.aprox.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.aprox.inject.TestData;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.web.json.test.WebFixture;

@javax.enterprise.context.ApplicationScoped
public class TestConfigProvider
{
    public static String REPO_ROOT_DIR = "repo.root.dir";

    private AutoProxModel model;

    private AutoProxConfiguration config;

    private AproxConfiguration proxyConfig;

    private final WebFixture http = new WebFixture();

    private DefaultStorageProviderConfiguration storageConfig;

    //    private MavenPluginDefaults pluginDefaults;
    //
    //    @Produces
    //    @Default
    //    public synchronized MavenPluginDefaults getPluginDefaults()
    //    {
    //        if ( pluginDefaults == null )
    //        {
    //            pluginDefaults = new StandardMaven304PluginDefaults();
    //        }
    //
    //        return pluginDefaults;
    //    }

    @Produces
    @Default
    public synchronized AproxConfiguration getProxyConfig()
    {
        if ( proxyConfig == null )
        {
            proxyConfig = new DefaultAproxConfiguration();
        }
        return proxyConfig;
    }

    @Produces
    @Default
    @TestData
    public synchronized AutoProxConfiguration getAutoProxConfiguration()
    {
        if ( config == null )
        {
            config = new AutoProxConfiguration();
            config.setEnabled( true );
            config.setDeployEnabled( true );
        }

        return config;
    }

    @Produces
    @Default
    public synchronized AutoProxModel getAutoProxModel()
        throws MalformedURLException
    {
        if ( model == null )
        {
            model = new AutoProxModel();
            model.setRepo( new Repository( "repo", http.resourceUrl( "target/${name}" ) ) );
            model.setGroup( new Group( "group", new StoreKey( StoreType.repository, "first" ), new StoreKey( StoreType.repository, "second" ) ) );

            System.out.println( "\n\n\n\nSet Autoprox URL: " + model.getRepo()
                                                                    .getUrl() + "\n\n\n\n" );
        }

        return model;
    }

    @Produces
    @Default
    public synchronized DefaultStorageProviderConfiguration getStorageProviderConfiguration()
        throws IOException
    {
        if ( storageConfig == null )
        {
            final String path = System.getProperty( REPO_ROOT_DIR );
            File dir;
            if ( path == null )
            {
                dir = File.createTempFile( "repo.root", ".dir" );
                dir.delete();
                dir.mkdirs();
            }
            else
            {
                dir = new File( path );
            }
            storageConfig = new DefaultStorageProviderConfiguration( dir );
        }

        return storageConfig;
    }

}