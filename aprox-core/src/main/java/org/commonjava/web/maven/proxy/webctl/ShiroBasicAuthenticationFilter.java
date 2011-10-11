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

import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.commonjava.auth.couch.data.PasswordManager;
import org.commonjava.util.logging.Logger;

@WebFilter( urlPatterns = "/*", filterName = "shiro-basic-authc" )
public class ShiroBasicAuthenticationFilter
    extends BasicHttpAuthenticationFilter
{

    public static final String APPLICATION_NAME_KEY = "aprox-application-name";

    public static final String DEFAULT_APPLICATION_NAME = "aprox";

    private final Logger logger = new Logger( getClass() );

    @Inject
    private PasswordManager passwordManager;

    public ShiroBasicAuthenticationFilter()
    {
        setApplicationName( "aprox" );
    }

    @Override
    protected AuthenticationToken createToken( final String username, final String password,
                                               final boolean rememberMe, final String host )
    {
        return new UsernamePasswordToken( username, passwordManager.digestPassword( password ),
                                          rememberMe, host );
    }

    @Override
    protected void onFilterConfigSet()
        throws Exception
    {
        logger.info( "Initializing authentication filter..." );
        Object appName = getFilterConfig().getServletContext().getAttribute( APPLICATION_NAME_KEY );
        if ( appName == null )
        {
            appName = DEFAULT_APPLICATION_NAME;
        }

        logger.info( "  setting application name: '%s'", appName );

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
