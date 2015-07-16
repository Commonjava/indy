package org.commonjava.aprox.keycloak.conf;

import java.util.Arrays;
import java.util.List;

public class KeycloakSecurityConstraint {

	private String role;
	private String urlPattern;
	private List<String> methods;	
	
	public KeycloakSecurityConstraint(String role, String urlPattern,
			List<String> methods) {
		super();
		this.role = role;
		this.urlPattern = urlPattern;
		this.methods = methods;
	}
	
	public KeycloakSecurityConstraint(String role, String urlPattern,
			String[] methods) {
		super();
		this.role = role;
		this.urlPattern = urlPattern;
		this.methods = Arrays.asList(methods);
	}
	
	public KeycloakSecurityConstraint() {
		// keep default constructor
	}
	

	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	public String getUrlPattern() {
		return urlPattern;
	}
	public void setUrlPattern(String urlPattern) {
		this.urlPattern = urlPattern;
	}
	public List<String> getMethods() {
		return methods;
	}
	public void setMethods(List<String> methods) {
		this.methods = methods;
	}

	@Override
	public String toString() {
		return "SecurityConstraint [role=" + role + ", urlPattern="
				+ urlPattern + ", methods=" + methods + "]";
	}
	
	
}
