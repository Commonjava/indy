/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.core.webctl;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.stats.AProxVersioning;
import org.commonjava.shelflife.ExpirationManager;
import org.commonjava.shelflife.ExpirationManagerException;
import org.commonjava.util.logging.Logger;

@WebListener
public class BootListener
    implements ServletContextListener
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private StoreDataManager dataManager;

    @Inject
    private ExpirationManager expirationManager;

    @Inject
    private AProxVersioning versioning;

    @Override
    public void contextInitialized( final ServletContextEvent sce )
    {
        logger.info( "\n\n\n\n\n STARTING AProx\n    Version: %s\n    Built-By: %s\n    Commit-ID: %s\n    Built-On: %s\n\n\n\n\n",
                     versioning.getVersion(), versioning.getBuilder(), versioning.getCommitId(), versioning.getTimestamp() );

        logger.info( "Verfiying that AProx DB + basic data is installed..." );
        try
        {
            dataManager.install();

            // make sure the expiration manager is running...
            expirationManager.loadNextExpirations();
        }
        catch ( final ProxyDataException | ExpirationManagerException e )
        {
            throw new RuntimeException( "Failed to boot aprox components: " + e.getMessage(), e );
        }

        logger.info( "...done." );
    }

    @Override
    public void contextDestroyed( final ServletContextEvent sce )
    {
        logger.info( "\n\n\n\n\n SHUTTING DOWN AProx\n    Version: %s\n    Built-By: %s\n    Commit-ID: %s\n    Built-On: %s\n\n\n\n\n",
                     versioning.getVersion(), versioning.getBuilder(), versioning.getCommitId(), versioning.getTimestamp() );

    }

}
