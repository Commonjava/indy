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
package org.commonjava.indy.bind.jaxrs.keycloak;

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.SecurityInfo.EmptyRoleSemantic;
import io.undertow.servlet.api.WebResourceCollection;
import io.undertow.util.ImmediateAuthenticationMechanismFactory;
import org.commonjava.indy.bind.jaxrs.IndyDeploymentProvider;
import org.commonjava.indy.bind.jaxrs.ui.UIServlet;
import org.commonjava.indy.subsys.keycloak.conf.KeycloakConfig;
import org.commonjava.indy.subsys.keycloak.conf.KeycloakSecurityBindings;
import org.commonjava.indy.subsys.keycloak.conf.KeycloakSecurityConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.Application;
import java.io.File;

public class KeycloakDeploymentProvider
    extends IndyDeploymentProvider
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String KEYCLOAK_CONFIG_FILE_PARAM = "keycloak.config.file";

    private static final String KEYCLOAK_LOGIN_MECHANISM = "KEYCLOAK";

    private static final String BASIC_LOGIN_MECHANISM = "BASIC";

    @Inject
    private KeycloakConfig config;

    @Inject
    private KeycloakSecurityBindings bindings;

    @Inject
    private BasicAuthenticationOAuthTranslator basicAuthInjector;

    @Override
    public DeploymentInfo getDeploymentInfo( String contextRoot, Application application )
    {
        logger.debug( "Keycloak deployment provider triggered." );

        final DeploymentInfo di = new DeploymentInfo();
        if ( config.isEnabled() )
        {
            di.addAuthenticationMechanism( BASIC_LOGIN_MECHANISM,
                                           new ImmediateAuthenticationMechanismFactory( basicAuthInjector ) );

            logger.debug( "Adding keycloak security constraints" );

            final SecurityConstraint ui = new SecurityConstraint();
            ui.setEmptyRoleSemantic( EmptyRoleSemantic.PERMIT );
            final WebResourceCollection uiCollection = new WebResourceCollection();
            uiCollection.addUrlPatterns( UIServlet.PATHS );
            uiCollection.addHttpMethods( UIServlet.METHODS );
            ui.addWebResourceCollection( uiCollection );
            di.addSecurityConstraint( ui );

            for ( final KeycloakSecurityConstraint constraint : bindings.getConstraints() )
            {
                final SecurityConstraint sc = new SecurityConstraint();
                sc.setEmptyRoleSemantic( EmptyRoleSemantic.PERMIT );
                final WebResourceCollection collection = new WebResourceCollection();
                collection.addUrlPattern( constraint.getUrlPattern() );
                logger.debug( "new constraint>>> URL pattern: {}", constraint.getUrlPattern() );
                if ( constraint.getMethods() != null )
                {
                    logger.debug( "methods: {}", constraint.getMethods() );
                    collection.addHttpMethods( constraint.getMethods() );
                }

                sc.addWebResourceCollection( collection );

                if ( constraint.getRole() != null )
                {
                    logger.debug( "role: {}", constraint.getRole() );
                    sc.addRoleAllowed( constraint.getRole() );
                }

                logger.debug( "Keycloak Security Constraint: {}", sc );
                di.addSecurityConstraint( sc );
            }

            logger.debug( "Using keycloak.json: {} (exists? {})", config.getKeycloakJson(),
                          new File( config.getKeycloakJson() ).exists() );
            di.addInitParameter( KEYCLOAK_CONFIG_FILE_PARAM, config.getKeycloakJson() );

            logger.debug( "login realm: {}", config.getRealm() );
            final LoginConfig loginConfig = new LoginConfig( KEYCLOAK_LOGIN_MECHANISM, config.getRealm() );
            loginConfig.addFirstAuthMethod( BASIC_LOGIN_MECHANISM );

            di.setLoginConfig( loginConfig );
        }

        return di;
    }

}
