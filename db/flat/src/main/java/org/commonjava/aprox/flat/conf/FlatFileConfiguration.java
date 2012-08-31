package org.commonjava.aprox.flat.conf;

import java.io.File;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.aprox.conf.AproxFeatureConfig;
import org.commonjava.shelflife.store.flat.FlatShelflifeStoreConfiguration;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigNames;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "db-flat" )
@Alternative
@Named( "unused" )
public class FlatFileConfiguration
{

    @Singleton
    public static final class FlatFileFeatureConfig
        extends AproxFeatureConfig<FlatFileConfiguration, FlatFileConfiguration>
    {
        @Inject
        private FlatFileConfigInfo info;

        public FlatFileFeatureConfig()
        {
            super( FlatFileConfiguration.class );
        }

        @Produces
        @Default
        public FlatFileConfiguration getFlatFileConfig()
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

    @Singleton
    public static final class FlatFileConfigInfo
        extends AproxConfigInfo
    {
        public FlatFileConfigInfo()
        {
            super( FlatFileConfiguration.class );
        }
    }

    public static final File DEFAULT_BASEDIR = new File( "/var/lib/aprox/db/aprox" );

    private File definitionsDir;

    public FlatFileConfiguration()
    {
    }

    @ConfigNames( "definitions.dir" )
    public FlatFileConfiguration( final File definitionsDir )
    {
        this.definitionsDir = definitionsDir;
    }

    public File getDefinitionsDir()
    {
        return definitionsDir == null ? DEFAULT_BASEDIR : definitionsDir;
    }

    public void setDefinitionsDir( final File definitionsDir )
    {
        this.definitionsDir = definitionsDir;
    }

    @Produces
    @Default
    public FlatShelflifeStoreConfiguration getShelflifeConfig()
    {
        return new FlatShelflifeStoreConfiguration( new File( getDefinitionsDir().getParentFile(), "shelflife" ) );
    }

}
