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

import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.StoreDataManager;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreKey;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.aprox.core.stats.AProxVersioning;
import org.commonjava.util.logging.Logger;

@WebListener
public class InstallerListener
    implements ServletContextListener
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private StoreDataManager dataManager;

    @Inject
    private AProxVersioning versioning;

    @Override
    public void contextInitialized( final ServletContextEvent sce )
    {
        logger.info( "\n\n\n\n\n STARTING AProx\n    Version: %s\n    Built-By: %s\n    Commit-ID: %s\n    Built-On: %s\n\n\n\n\n",
                     versioning.getVersion(), versioning.getBuilder(), versioning.getCommitId(),
                     versioning.getTimestamp() );

        logger.info( "Verfiying that AProx DB + basic data is installed..." );
        try
        {
            dataManager.install();
            dataManager.storeRepository( new Repository( "central", "http://repo1.maven.apache.org/maven2/" ), true );

            dataManager.storeGroup( new Group( "public", new StoreKey( StoreType.repository, "central" ) ), true );
        }
        catch ( final ProxyDataException e )
        {
            throw new RuntimeException( "Failed to install proxy database: " + e.getMessage(), e );
        }

        logger.info( "...done." );
    }

    @Override
    public void contextDestroyed( final ServletContextEvent sce )
    {
        logger.info( "\n\n\n\n\n SHUTTING DOWN AProx\n    Version: %s\n    Built-By: %s\n    Commit-ID: %s\n    Built-On: %s\n\n\n\n\n",
                     versioning.getVersion(), versioning.getBuilder(), versioning.getCommitId(),
                     versioning.getTimestamp() );

    }

}
