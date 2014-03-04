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
