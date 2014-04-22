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
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.commonjava.badgr.data.PasswordManager;
import org.commonjava.util.logging.Logger;

@WebFilter( urlPatterns = "/*", filterName = "shiro-basic-authc" )
public class ShiroBasicAuthenticationFilter
    extends BasicHttpAuthenticationFilter
{

    public static final String APPLICATION_NAME_KEY = "aprox-application-name";

    public static final String DEFAULT_APPLICATION_NAME = "aprox";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private PasswordManager passwordManager;

    public ShiroBasicAuthenticationFilter()
    {
        setApplicationName( "aprox" );
    }

    @Override
    protected AuthenticationToken createToken( final String username, final String password, final boolean rememberMe,
                                               final String host )
    {
        return new UsernamePasswordToken( username, passwordManager.digestPassword( password ), rememberMe, host );
    }

    @Override
    protected void onFilterConfigSet()
        throws Exception
    {
        logger.info( "Initializing authentication filter..." );
        Object appName = getFilterConfig().getServletContext()
                                          .getAttribute( APPLICATION_NAME_KEY );
        if ( appName == null )
        {
            appName = DEFAULT_APPLICATION_NAME;
        }

        logger.info( "  setting application name: '{}'", appName );

        setApplicationName( String.valueOf( appName ) );

        processPathConfig( "/**", Boolean.TRUE.toString() );

        logger.info( "...done." );
    }

    @Override
    protected boolean onAccessDenied( final ServletRequest request, final ServletResponse response )
        throws Exception
    {
        // TODO Auto-generated method stub
        return super.onAccessDenied( request, response );
    }

}
