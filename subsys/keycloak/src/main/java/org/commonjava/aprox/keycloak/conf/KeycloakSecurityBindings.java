package org.commonjava.aprox.keycloak.conf;

import java.util.List;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;

@Alternative
@Named
public class KeycloakSecurityBindings {
	
    private List<KeycloakSecurityConstraint> constraints;

    public List<KeycloakSecurityConstraint> getConstraints()
    {
        return constraints;
	}

    public void setConstraints( final List<KeycloakSecurityConstraint> securityContraints )
    {
        this.constraints = securityContraints;
	}
	

}
