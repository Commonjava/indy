/*******************************************************************************
 * Copyright (C) 2011  John Casey
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see 
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.sec.webctl;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.commonjava.auth.couch.data.UserDataException;
import org.commonjava.auth.couch.data.UserDataManager;
import org.commonjava.auth.couch.inject.UserData;
import org.commonjava.couch.change.CouchChangeListener;
import org.commonjava.couch.db.CouchDBException;
import org.commonjava.util.logging.Logger;

@WebListener
public class InstallerListener
    implements ServletContextListener
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private UserDataManager dataManager;

    @Inject
    @UserData
    private CouchChangeListener changeListener;

    @Override
    public void contextInitialized( final ServletContextEvent sce )
    {
        logger.info( "Verfiying that User CouchDB + applications + basic data is installed..." );
        try
        {
            dataManager.install();
            dataManager.setupAdminInformation();

            changeListener.startup( false );
        }
        catch ( UserDataException e )
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
