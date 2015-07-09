package org.commonjava.aprox.keycloak.rest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.keycloak.conf.KeycloakConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SecurityControllerTest
{
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void replaceUiJsonRealmFromConfig()
        throws Exception
    {
        final File keycloakJsonFile = temp.newFile();

        try (InputStream in = Thread.currentThread()
                                    .getContextClassLoader()
                                    .getResourceAsStream( "keycloak-ui-realm-replace.json" );
                        FileOutputStream out = new FileOutputStream( keycloakJsonFile ))
        {
            IOUtils.copy( in, out );
        }

        final String realm = "myRealm";
        final KeycloakConfig config = new KeycloakConfig();
        config.setRealm( realm );
        config.setKeycloakUrl( "http://localhost:11111/auth" );
        config.setEnabled( true );
        config.setKeycloakUiJson( keycloakJsonFile.getPath() );

        final SecurityController controller = new SecurityController( config );

        final String json = controller.getKeycloakUiJson();
        assertThat( json, containsString( realm ) );
    }

    @Test
    public void replaceKeycloakInitJsRealmFromConfig()
        throws Exception
    {
        final File keycloakJsonFile = temp.newFile();

        try (InputStream in = Thread.currentThread()
                                    .getContextClassLoader()
                                    .getResourceAsStream( "keycloak-ui-realm-replace.json" );
                        FileOutputStream out = new FileOutputStream( keycloakJsonFile ))
        {
            IOUtils.copy( in, out );
        }

        final String realm = "myRealm";
        final KeycloakConfig config = new KeycloakConfig();
        config.setRealm( realm );
        config.setKeycloakUrl( "http://localhost:11111/auth" );
        config.setEnabled( true );
        config.setKeycloakUiJson( keycloakJsonFile.getPath() );

        final SecurityController controller = new SecurityController( config );

        final String json = controller.getKeycloakInit();
        assertThat( json, containsString( realm ) );
    }

}
