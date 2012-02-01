package org.commonjava.aprox.autoprox.conf;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.commonjava.couch.inject.Production;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.DefaultConfigurationListener;
import org.commonjava.web.config.io.SingleSectionConfigReader;

@Singleton
public class AutoProxConfigFactory
    extends DefaultConfigurationListener
{

    private static final String CONFIG_PATH = "/etc/aprox/autoprox.conf";

    private DefaultAutoProxConfiguration config;

    private File configFile = new File( CONFIG_PATH );

    public AutoProxConfigFactory()
        throws ConfigurationException
    {
        super( DefaultAutoProxConfiguration.class );
    }

    public AutoProxConfigFactory( final File configFile )
        throws ConfigurationException
    {
        super( DefaultAutoProxConfiguration.class );
        this.configFile = configFile;
        load();
    }

    @PostConstruct
    public void load()
        throws ConfigurationException
    {
        InputStream stream = null;
        try
        {
            stream = new FileInputStream( configFile );
            new SingleSectionConfigReader( this ).loadConfiguration( stream );
        }
        catch ( final IOException e )
        {
            throw new ConfigurationException( "Cannot open configuration file: %s. Reason: %s", e, CONFIG_PATH,
                                              e.getMessage() );
        }
        finally
        {
            closeQuietly( stream );
        }
    }

    @Produces
    @Production
    @Default
    public AutoProxConfiguration getConfiguration()
    {
        return config;
    }

    @Override
    public void configurationComplete()
        throws ConfigurationException
    {
        config = getConfiguration( DefaultAutoProxConfiguration.class );
    }

}
