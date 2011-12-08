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
package org.commonjava.aprox.couch.webctl;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.commonjava.aprox.core.inject.AproxData;
import org.commonjava.couch.change.CouchChangeListener;
import org.commonjava.couch.db.CouchDBException;
import org.commonjava.util.logging.Logger;

@WebListener
public class CouchInstallerListener
    implements ServletContextListener
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    @AproxData
    private CouchChangeListener changeListener;

    @Override
    public void contextInitialized( final ServletContextEvent sce )
    {
        try
        {
            changeListener.startup( false );
        }
        catch ( final CouchDBException e )
        {
            throw new RuntimeException( "Failed to start CouchDB changes listener: " + e.getMessage(), e );
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
        catch ( final CouchDBException e )
        {
            throw new RuntimeException( "Failed to shutdown CouchDB changes listener: " + e.getMessage(), e );
        }
    }

}
