package org.commonjava.aprox.bind.jaxrs.keycloak;

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.WebResourceCollection;

import javax.inject.Inject;

import org.commonjava.aprox.bind.jaxrs.AproxDeploymentProvider;
import org.commonjava.aprox.keycloak.conf.KeycloakConfig;
import org.commonjava.aprox.keycloak.conf.KeycloakSecurityBindings;
import org.commonjava.aprox.keycloak.conf.KeycloakSecurityConstraint;

public class KeycloakDeploymentProvider
    extends AproxDeploymentProvider
{

    private static final String KEYCLOAK_CONFIG_FILE_PARAM = "keycloak.config.file";

    private static final String KEYCLOAK_LOGIN_MECHANISM = "KEYCLOAK";

    @Inject
    private KeycloakConfig config;

    @Inject
    private KeycloakSecurityBindings bindings;

    @Override
    public DeploymentInfo getDeploymentInfo()
    {
        final DeploymentInfo di = new DeploymentInfo();
        if ( config.isEnabled() )
        {
            for ( final KeycloakSecurityConstraint constraint : bindings.getConstraints() )
            {
                final SecurityConstraint sc = new SecurityConstraint();
                final WebResourceCollection collection = new WebResourceCollection();
                collection.addUrlPattern( constraint.getUrlPattern() );
                if ( constraint.getMethods() != null )
                {
                    collection.addHttpMethods( constraint.getMethods() );
                }

                sc.addWebResourceCollection( collection );

                if ( constraint.getRole() != null )
                {
                    sc.addRoleAllowed( constraint.getRole() );
                }

                di.addSecurityConstraint( sc );
            }

            di.addInitParameter( KEYCLOAK_CONFIG_FILE_PARAM, config.getKeycloakJson() );
            di.setLoginConfig( new LoginConfig( KEYCLOAK_LOGIN_MECHANISM, config.getRealm() ) );
        }

        return di;
    }

}
