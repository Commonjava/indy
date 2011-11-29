/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
