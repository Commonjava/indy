package org.commonjava.aprox.keycloak.rest;

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
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.keycloak.conf.KeycloakConfig;
import org.commonjava.maven.galley.util.UrlUtils;

@ApplicationScoped
public class SecurityController
{

    private static final String KEYCLOAK_INIT_JS = "keycloak-init.js";

    private static final String DISABLED_KEYCLOAK_INIT_JS = "disabled-keycloak-init.js";

    @Inject
    private KeycloakConfig config;

    private String keycloakInitJs;

    protected SecurityController()
    {
    }

    public SecurityController( final KeycloakConfig config )
    {
        this.config = config;
    }

    public synchronized String getKeycloakInit()
        throws AproxWorkflowException
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

                final Properties props = new Properties();
                props.setProperty( "realm", config.getRealm() );

                final String keycloakUiJson = loadFileContent( config.getKeycloakUiJson() );
                props.setProperty( "json", keycloakUiJson );

                final StringSearchInterpolator interpolator = new StringSearchInterpolator();
                interpolator.addValueSource( new PropertiesBasedValueSource( props ) );

                try
                {
                    keycloakInitJs = interpolator.interpolate( raw );
                }
                catch ( final InterpolationException e )
                {
                    throw new AproxWorkflowException( "Failed to resolve expressions in keycloak-ui.json: %s", e,
                                                      e.getMessage() );
                }
            }
        }

        return keycloakInitJs;
    }

    public String getKeycloakJs()
        throws AproxWorkflowException
    {
        if ( !config.isEnabled() )
        {
            return null;
        }

        try
        {
            return UrlUtils.buildUrl( config.getKeycloakUrl(), "/js/keycloak.js" );
        }
        catch ( final MalformedURLException e )
        {
            throw new AproxWorkflowException( "Keycloak URL is invalid: %s", e, config.getKeycloakUrl() );
        }
    }

    public String getKeycloakUiJson()
        throws AproxWorkflowException
    {
        final Properties props = new Properties();
        props.setProperty( "realm", config.getRealm() );

        final StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.addValueSource( new PropertiesBasedValueSource( props ) );

        final String raw = loadFileContent( config.getKeycloakUiJson() );

        try
        {
            return interpolator.interpolate( raw );
        }
        catch ( final InterpolationException e )
        {
            throw new AproxWorkflowException( "Failed to resolve expressions in keycloak-ui.json: %s", e,
                                              e.getMessage() );
        }
    }

    private String loadFileContent( final String path )
        throws AproxWorkflowException
    {
        final File f = new File( path );
        if ( !f.exists() )
        {
            throw new AproxWorkflowException( "Path: %s does not exist!", path );
        }

        try
        {
            return FileUtils.readFileToString( f );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Cannot read path: %s. Reason: %s", e, path, e.getMessage() );
        }
    }

    private String loadClasspathContent( final String jsFile )
        throws AproxWorkflowException
    {
        try (InputStream jsStream = Thread.currentThread()
                                          .getContextClassLoader()
                                          .getResourceAsStream( jsFile ))
        {
            if ( jsStream == null )
            {
                throw new AproxWorkflowException( "Failed to load javascript from classpath: %s. Resource not found.",
                                                  jsFile );
            }

            return IOUtils.toString( jsStream );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read javascript from classpath: %s. Reason: %s.", e, jsFile,
                                              e.getMessage() );
        }
    }

}
