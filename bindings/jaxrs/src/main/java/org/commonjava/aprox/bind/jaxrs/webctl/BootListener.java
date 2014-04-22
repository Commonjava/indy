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
package org.commonjava.aprox.bind.jaxrs.webctl;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.commonjava.aprox.conf.AproxConfigFactory;
import org.commonjava.aprox.core.rest.AdminController;
import org.commonjava.web.config.ConfigurationException;

@WebListener
public class BootListener
    implements ServletContextListener
{

    @Inject
    private AdminController adminController;

    @Inject
    private AproxConfigFactory configFactory;

    @Override
    public void contextInitialized( final ServletContextEvent sce )
    {
        try
        {
            configFactory.load( System.getProperty( AproxConfigFactory.CONFIG_PATH_PROP, AproxConfigFactory.DEFAULT_CONFIG_PATH ) );
        }
        catch ( final ConfigurationException e )
        {
            throw new RuntimeException( "Failed to configure AProx: " + e.getMessage(), e );
        }
        adminController.started();
    }

    @Override
    public void contextDestroyed( final ServletContextEvent sce )
    {
        adminController.stopped();

    }

}
