package org.commonjava.indy.core.conf;

import org.commonjava.propulsor.config.ConfigurationException;
import org.commonjava.propulsor.config.ConfigurationRegistry;
import org.commonjava.propulsor.config.DefaultConfigurationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class ConfigRegistryFixture
{
    private ConfigurationRegistry registry;

    @PostConstruct
    public void setupRegistry()
    {
        try
        {
            this.registry = new DefaultConfigurationRegistry();
        }
        catch ( ConfigurationException e )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.error( "Failed to initialize empty registry" );
        }
    }
    @Produces
    public ConfigurationRegistry getRegistry()
    {
        return registry;
    }
}
