package org.commonjava.aprox.depbase.webctl;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.commonjava.depbase.data.DepbaseDataException;
import org.commonjava.depbase.data.DepbaseDataManager;
import org.commonjava.util.logging.Logger;

@WebListener
public class DepbaseInstallerListener
    implements ServletContextListener
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private DepbaseDataManager dataManager;

    @Override
    public void contextInitialized( final ServletContextEvent sce )
    {
        logger.info( "Verfiying that DepBase CouchDB + applications is installed..." );

        try
        {
            dataManager.install();
        }
        catch ( DepbaseDataException e )
        {
            throw new RuntimeException( "Failed to install depbase database: " + e.getMessage(), e );
        }

        logger.info( "...done" );
    }

    @Override
    public void contextDestroyed( final ServletContextEvent sce )
    {
        // NOP
    }

}
