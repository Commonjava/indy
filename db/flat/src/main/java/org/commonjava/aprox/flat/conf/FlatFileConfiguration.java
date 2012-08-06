package org.commonjava.aprox.flat.conf;

import java.io.File;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;
import javax.inject.Singleton;

import org.commonjava.aprox.core.conf.AproxConfigSet;
import org.commonjava.web.config.annotation.ConfigNames;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "db-flat" )
@Alternative
@Named( "unused" )
public class FlatFileConfiguration
{

    @Singleton
    public static final class ConfigSet
        extends AproxConfigSet<FlatFileConfiguration, FlatFileConfiguration>
    {
        public ConfigSet()
        {
            super( FlatFileConfiguration.class );
        }
    }

    public static final File DEFAULT_BASEDIR = new File( "/var/lib/aprox/definitions" );

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
        return definitionsDir;
    }

    public void setDefinitionsDir( final File definitionsDir )
    {
        this.definitionsDir = definitionsDir;
    }

}
