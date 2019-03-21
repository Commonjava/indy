package org.commonjava.indy.boot.jaxrs;

import org.commonjava.indy.conf.IndyConfigFactory;
import org.commonjava.propulsor.boot.BootOptions;
import org.commonjava.propulsor.config.Configurator;
import org.commonjava.propulsor.config.ConfiguratorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class IndyConfigurator
                implements Configurator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private IndyConfigFactory configFactory;

    @Override
    public void load( BootOptions options ) throws ConfiguratorException
    {
        try
        {
            logger.info( "\n\nLoading Indy configuration factory: {}\n", configFactory );
            configFactory.load( options.getConfig() );
        }
        catch ( final Exception e )
        {
            logger.error( "Failed to configure Indy: {}", e.getMessage(), e );
            throw new ConfiguratorException( "Failed to configure Indy", e );
        }
    }

}
