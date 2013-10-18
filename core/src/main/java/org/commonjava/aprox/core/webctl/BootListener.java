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
