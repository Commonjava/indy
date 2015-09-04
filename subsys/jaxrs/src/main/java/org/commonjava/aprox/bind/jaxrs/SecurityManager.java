package org.commonjava.aprox.bind.jaxrs;

import org.commonjava.aprox.subsys.keycloak.conf.KeycloakConfig;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

/**
 * Created by jdcasey on 9/3/15.
 */
public class SecurityManager
{

    @Inject
    private KeycloakConfig config;

    public String getUser( SecurityContext context, HttpServletRequest request )
    {
        if ( !config.isEnabled())
        {
            return request.getRemoteHost();
        }

        if ( context == null )
        {
            return request.getRemoteHost();
        }

        Principal userPrincipal = context.getUserPrincipal();
        if ( userPrincipal == null )
        {
            return request.getRemoteHost();
        }

        String user = userPrincipal.getName();
        return user == null ? request.getRemoteHost() : user;
    }
}
