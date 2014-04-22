/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.sec.webctl;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.commonjava.aprox.sec.data.AProxSecDataManager;
import org.commonjava.badgr.data.BadgrDataException;
import org.commonjava.util.logging.Logger;

@WebListener
public class InstallerListener
    implements ServletContextListener
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private AProxSecDataManager dataManager;

    @Override
    public void contextInitialized( final ServletContextEvent sce )
    {
        logger.info( "Verfiying that User CouchDB + applications + basic data is installed..." );
        try
        {
            dataManager.install();
        }
        catch ( final BadgrDataException e )
        {
            throw new RuntimeException( "Failed to install proxy database: " + e.getMessage(), e );
        }

        logger.info( "...done." );
    }

    @Override
    public void contextDestroyed( final ServletContextEvent sce )
    {
    }

}
