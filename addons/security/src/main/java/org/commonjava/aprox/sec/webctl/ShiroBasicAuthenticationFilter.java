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
package org.commonjava.aprox.sec.webctl;

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
