package org.commonjava.aprox.bind.jaxrs;

import java.util.List;

public class SecurityConstraintConfig {
	
	private List<SecurityConstraint> securityContraints;

	public List<SecurityConstraint> getSecurityContraints() {
		return securityContraints;
	}

	public void setSecurityContraints(List<SecurityConstraint> securityContraints) {
		this.securityContraints = securityContraints;
	}
	

}
