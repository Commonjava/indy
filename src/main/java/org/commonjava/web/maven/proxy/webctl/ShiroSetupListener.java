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
