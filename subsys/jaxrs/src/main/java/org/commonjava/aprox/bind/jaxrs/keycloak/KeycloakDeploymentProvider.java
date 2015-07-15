package org.commonjava.aprox.bind.jaxrs.keycloak;

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.WebResourceCollection;
import io.undertow.util.ImmediateAuthenticationMechanismFactory;

import java.io.File;

import javax.inject.Inject;

import org.commonjava.aprox.bind.jaxrs.AproxDeploymentProvider;
import org.commonjava.aprox.keycloak.conf.KeycloakConfig;
import org.commonjava.aprox.keycloak.conf.KeycloakSecurityBindings;
import org.commonjava.aprox.keycloak.conf.KeycloakSecurityConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeycloakDeploymentProvider
    extends AproxDeploymentProvider
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
    public DeploymentInfo getDeploymentInfo()
    {
        logger.debug( "Keycloak deployment provider triggered." );

        final DeploymentInfo di = new DeploymentInfo();
        if ( config.isEnabled() )
        {
            //            di.addOuterHandlerChainWrapper( new BasicAuthWrapper( "outer" ) );
            di.addAuthenticationMechanism( BASIC_LOGIN_MECHANISM,
                                           new ImmediateAuthenticationMechanismFactory( basicAuthInjector ) );

            logger.debug( "Adding keycloak security constraints" );
            for ( final KeycloakSecurityConstraint constraint : bindings.getConstraints() )
            {
                final SecurityConstraint sc = new SecurityConstraint();
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
