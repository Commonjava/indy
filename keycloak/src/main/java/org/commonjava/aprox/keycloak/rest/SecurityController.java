package org.commonjava.aprox.keycloak.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.keycloak.conf.KeycloakConfig;
import org.keycloak.representations.adapters.config.AdapterConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class SecurityController
{

    private static final String KEYCLOAK_INIT_JS = "keycloak-init.js";

    private static final String KEYCLOAK_JS = "keycloak.js";

    @Inject
    private KeycloakConfig config;

    @Inject
    private ObjectMapper mapper;

    private String keycloakInitJs;

    public synchronized String getKeycloakInit()
        throws AproxWorkflowException
    {
        if ( keycloakInitJs == null )
        {
            final String raw = loadClasspathJs( KEYCLOAK_INIT_JS );

            final Properties props = new Properties();
            props.setProperty( "json", getKeycloakUiJson() );
            props.setProperty( "realm", config.getRealm() );

            final StringSearchInterpolator interpolator = new StringSearchInterpolator( "@", "@" );
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

        return keycloakInitJs;
    }

    public String getKeycloakJs()
        throws AproxWorkflowException
    {
        return loadClasspathJs( KEYCLOAK_JS );
    }

    public String getKeycloakUiJson()
        throws AproxWorkflowException
    {
        final String keycloakUiJson = config.getKeycloakUiJson();
        final File f = new File( keycloakUiJson );
        try
        {
            final AdapterConfig config = mapper.readValue( f, AdapterConfig.class );
            config.setRealm( config.getRealm() );

            return mapper.writeValueAsString( config );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read keycloak-ui.json from : %s. Reason: %s.", e,
                                              keycloakUiJson, e.getMessage() );
        }
    }

    private String loadClasspathJs( final String jsFile )
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
