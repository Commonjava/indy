package org.commonjava.aprox.flat.conf;

import java.io.File;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;

import org.commonjava.web.config.annotation.ConfigNames;
import org.commonjava.web.config.annotation.SectionName;
import org.commonjava.web.config.section.ConfigurationSectionListener;

@SectionName( ConfigurationSectionListener.DEFAULT_SECTION )
@Alternative
@Named( "unused" )
public class FlatFileConfiguration
{

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
