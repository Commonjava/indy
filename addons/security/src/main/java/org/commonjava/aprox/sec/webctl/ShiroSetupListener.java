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

import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;

import org.commonjava.badgr.shiro.web.BadgrShiroSetupListener;
import org.commonjava.util.logging.Logger;

@WebListener
public class ShiroSetupListener
    extends BadgrShiroSetupListener
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public void contextInitialized( final ServletContextEvent sce )
    {
        logger.info( "Initializing BADGR Shiro authentication/authorization realm..." );
        super.contextInitialized( sce );
        logger.info( "...done." );
    }

}
