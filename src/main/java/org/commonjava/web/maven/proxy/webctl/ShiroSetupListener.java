package org.commonjava.web.maven.proxy.webctl;

import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;

import org.commonjava.auth.shiro.couch.web.CouchShiroSetupListener;
import org.commonjava.util.logging.Logger;

@WebListener
public class ShiroSetupListener
    extends CouchShiroSetupListener
{

    private final Logger logger = new Logger( getClass() );

    @Override
    public void contextInitialized( final ServletContextEvent sce )
    {
        logger.info( "Initializing CouchDB Shiro authentication/authorization realm..." );
        super.contextInitialized( sce );
        logger.info( "...done." );
    }

}
