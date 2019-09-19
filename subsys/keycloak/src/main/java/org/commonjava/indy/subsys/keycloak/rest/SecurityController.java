/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.subsys.keycloak.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.subsys.keycloak.conf.KeycloakConfig;
import org.commonjava.maven.galley.util.UrlUtils;

@ApplicationScoped
public class SecurityController
{

    private static final String KEYCLOAK_INIT_JS = "keycloak-init.js";

    private static final String DISABLED_KEYCLOAK_INIT_JS = "disabled-keycloak-init.js";

    @Inject
    private KeycloakConfig config;

    private String keycloakInitJs;

    private String keycloakUiJson;

    protected SecurityController()
    {
    }

    public SecurityController( final KeycloakConfig config )
    {
        this.config = config;
    }

    public synchronized String getKeycloakInit()
        throws IndyWorkflowException
    {
        if ( keycloakInitJs == null )
        {
            if ( !config.isEnabled() )
            {
                keycloakInitJs = loadClasspathContent( DISABLED_KEYCLOAK_INIT_JS );
            }
            else
            {
                final String raw = loadClasspathContent( KEYCLOAK_INIT_JS );

                try
                {
                    keycloakInitJs = getInterpolator().interpolate( raw );
                }
                catch ( final InterpolationException e )
                {
                    throw new IndyWorkflowException( "Failed to resolve expressions in keycloak-ui.json: %s", e,
                                                      e.getMessage() );
                }
            }
        }

        return keycloakInitJs;
    }

    private StringSearchInterpolator getInterpolator()
    {
        final Properties props = new Properties();
        props.setProperty( KeycloakConfig.KEYCLOAK_REALM, config.getRealm() );
        props.setProperty( KeycloakConfig.KEYCLOAK_URL, config.getUrl() );

        if ( config.getUiResource() != null )
        {
            props.setProperty( KeycloakConfig.KEYCLOAK_UI_RESOURCE, config.getUiResource() );
        }

        if ( config.getRealmPublicKey() != null )
        {
            props.setProperty( KeycloakConfig.KEYCLOAK_REALM_PUBLIC_KEY, config.getRealmPublicKey() );
        }

        final StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.addValueSource( new PropertiesBasedValueSource( props ) );

        return interpolator;
    }

    public String getKeycloakJs()
        throws IndyWorkflowException
    {
        if ( !config.isEnabled() )
        {
            return null;
        }

        try
        {
            return UrlUtils.buildUrl( config.getUrl(), "/js/keycloak.js" );
        }
        catch ( final MalformedURLException e )
        {
            throw new IndyWorkflowException( "Keycloak URL is invalid: %s", e, config.getUrl() );
        }
    }

    public synchronized String getKeycloakUiJson()
        throws IndyWorkflowException
    {
        if ( !config.isEnabled() )
        {
            return null;
        }

        if ( keycloakUiJson == null )
        {
            final String raw = loadFileContent( config.getKeycloakUiJson() );
            try
            {
                keycloakUiJson = getInterpolator().interpolate( raw );
            }
            catch ( final InterpolationException e )
            {
                throw new IndyWorkflowException( "Failed to resolve expressions in keycloak-ui.json: %s", e,
                                                  e.getMessage() );
            }
        }

        return keycloakUiJson;
    }

    private String loadFileContent( final String path )
        throws IndyWorkflowException
    {
        final File f = new File( path );
        if ( !f.exists() )
        {
            throw new IndyWorkflowException( "Path: %s does not exist!", path );
        }

        try
        {
            return FileUtils.readFileToString( f );
        }
        catch ( final IOException e )
        {
            throw new IndyWorkflowException( "Cannot read path: %s. Reason: %s", e, path, e.getMessage() );
        }
    }

    private String loadClasspathContent( final String jsFile )
        throws IndyWorkflowException
    {
        try (InputStream jsStream = Thread.currentThread()
                                          .getContextClassLoader()
                                          .getResourceAsStream( jsFile ))
        {
            if ( jsStream == null )
            {
                throw new IndyWorkflowException( "Failed to load javascript from classpath: %s. Resource not found.",
                                                  jsFile );
            }

            return IOUtils.toString( jsStream );
        }
        catch ( final IOException e )
        {
            throw new IndyWorkflowException( "Failed to read javascript from classpath: %s. Reason: %s.", e, jsFile,
                                              e.getMessage() );
        }
    }

}
