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

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.subsys.keycloak.conf.KeycloakConfig;
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
        config.setUrl( "http://localhost:11111/auth" );
        config.setEnabled( true );
        config.setKeycloakUiJson( keycloakJsonFile.getPath() );
        config.setRealmPublicKey( "FOOBARR" );

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
        config.setUrl( "http://localhost:11111/auth" );
        config.setEnabled( true );
        config.setKeycloakUiJson( keycloakJsonFile.getPath() );
        config.setRealmPublicKey( "FOOBARR" );

        final SecurityController controller = new SecurityController( config );

        final String json = controller.getKeycloakInit();
        assertThat( json, containsString( "api/security/keycloak.js" ) );
    }

}
