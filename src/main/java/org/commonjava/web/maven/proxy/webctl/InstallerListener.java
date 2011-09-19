package org.commonjava.web.maven.proxy.webctl;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.commonjava.couch.change.CouchChangeListener;
import org.commonjava.couch.db.CouchDBException;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.maven.proxy.data.ProxyDataException;
import org.commonjava.web.maven.proxy.data.ProxyDataManager;
import org.commonjava.web.maven.proxy.model.Group;
import org.commonjava.web.maven.proxy.model.Repository;

@WebListener
public class InstallerListener
    implements ServletContextListener
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private ProxyDataManager dataManager;

    @Inject
    private CouchChangeListener changeListener;

    @Override
    public void contextInitialized( final ServletContextEvent sce )
    {
        logger.info( "Verfiying that CouchDB + applications + basic data is installed..." );
        try
        {
            dataManager.install();
            dataManager.storeRepository( new Repository( "central",
                                                         "http://repo1.maven.apache.org/maven2/" ),
                                         true );

            dataManager.storeGroup( new Group( "public", "central" ), true );

            changeListener.startup();
        }
        catch ( ProxyDataException e )
        {
            throw new RuntimeException( "Failed to install proxy database: " + e.getMessage(), e );
        }
        catch ( CouchDBException e )
        {
            throw new RuntimeException( "Failed to start CouchDB changes listener: "
                + e.getMessage(), e );
        }

        logger.info( "...done." );
    }

    @Override
    public void contextDestroyed( final ServletContextEvent sce )
    {
        try
        {
            changeListener.shutdown();
        }
        catch ( CouchDBException e )
        {
            throw new RuntimeException( "Failed to shutdown CouchDB changes listener: "
                + e.getMessage(), e );
        }
    }

}
