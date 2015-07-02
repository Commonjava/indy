package org.commonjava.aprox.bind.jaxrs;

import java.util.Arrays;
import java.util.List;

public class SecurityConstraint {

	private String role;
	private String urlPattern;
	private List<String> methods;	
	
	public SecurityConstraint(String role, String urlPattern,
			List<String> methods) {
		super();
		this.role = role;
		this.urlPattern = urlPattern;
		this.methods = methods;
	}
	
	public SecurityConstraint(String role, String urlPattern,
			String[] methods) {
		super();
		this.role = role;
		this.urlPattern = urlPattern;
		this.methods = Arrays.asList(methods);
	}
	
	public SecurityConstraint() {
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
