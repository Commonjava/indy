package org.commonjava.aprox.filer.def.conf;

import java.io.File;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.aprox.conf.AproxFeatureConfig;
import org.commonjava.aprox.inject.Production;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigNames;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "storage-default" )
@Alternative
@Named( "unused" )
public class DefaultStorageProviderConfiguration
{

    @javax.enterprise.context.ApplicationScoped
    public static final class FlatFileFeatureConfig
        extends AproxFeatureConfig<DefaultStorageProviderConfiguration, DefaultStorageProviderConfiguration>
    {
        @Inject
        private FilerDefaultConfigInfo info;

        public FlatFileFeatureConfig()
        {
            super( DefaultStorageProviderConfiguration.class );
        }

        @Produces
        @Production
        @Default
        public DefaultStorageProviderConfiguration getFilerDefaultConfig()
            throws ConfigurationException
        {
            return getConfig();
        }

        @Override
        public AproxConfigInfo getInfo()
        {
            return info;
        }
    }

    @javax.enterprise.context.ApplicationScoped
    public static final class FilerDefaultConfigInfo
        extends AproxConfigInfo
    {
        public FilerDefaultConfigInfo()
        {
            super( DefaultStorageProviderConfiguration.class );
        }
    }

    public static final File DEFAULT_BASEDIR = new File( "/var/lib/aprox/storage" );

    private File storageBasedir;

    public DefaultStorageProviderConfiguration()
    {
    }

    @ConfigNames( "storage.dir" )
    public DefaultStorageProviderConfiguration( final File definitionsDir )
    {
        this.storageBasedir = definitionsDir;
    }

    public File getStorageRootDirectory()
    {
        return storageBasedir == null ? DEFAULT_BASEDIR : storageBasedir;
    }

    public void setStorageRootDirectory( final File storageBasedir )
    {
        this.storageBasedir = storageBasedir;
    }

}
