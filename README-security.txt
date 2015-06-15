APROX security
==============
Aprox installation may be configured with enabled security. Security on APROX is delivered via Keycloak project
http://keycloak.jboss.org/
There are basically 2 points, which are being secured.
1. Security of Aprox REST endpoints
Secured Aprox REST endpoints do expect the HttpRequest with "Authorization: Bearer + access_token" header.
Validity of authentication and authorization to such REST enpoint is being checked 

2. Security of Aprox Web UI
Secured Aprox UI comes with ability to redirect user to Keycloak SSO login page, where user provides his credentials
and is redirected back to Aprox default welcome page. During the login process the user obtained the acess_token, which is 
available during whole UI session and is provided with every REST call to Aprox REST endpoints

Build Aprox with enabled security
----------------------------------
Use below mvn command
mvn clean install -Dauth=true -Dkeycloak.server.location=<keycloak-server>

For example:
mvn clean install -Dauth=true -Dkeycloak.server.location=keycloak2-pncauth.rhcloud.com

After building of such command you will get following

1. In the s file there is a section called [security]
with 2 commented out properties to be defined by you.
#aprox.boot.secure.config=<path_to_keycloak_json_config_file>
#aprox.boot.secure.realm=<keycloak_realm>
#aprox.boot.security.constraint.config.path=<path_to_security_constraints_json_config_file>

aprox.boot.secure.config points to location (full path) of Aprox REST keycloak.json file
Such keycloak.json template (empty) file is located inside $APROX_HOME/bin folder
and this file should be filled out with something as bellow for example:

{
  "realm": "pncredhat",
  "realm-public-key": "...",
  "auth-server-url": ...",
  "ssl-required": "none",
  "bearer-only" : true,
  "use-resource-role-mappings" : false,
  "resource": "pncaproxrest",
  "credentials": {
    "secret": "..."
  }
}
The following *) applies 

aprox.boot.secure.realm is the value of "realm" property so in our case would be "pncredhat"

aprox.boot.security.constraint.config.path points to location of Aprox security constraints
Such security-constraints.json template file (with some example config) is located in $APROX_HOME/bin folder

2.In the $APROX_HOME/lib/thirdparty were placed the Keycloak related libs (adapter), which enables the security for Aprox REST

3.In the $APROX_HOME/vav/lib/aprox/ui there are following files touched/added
	-keycloak.json for Aprox Web UI  -> this is the diferrent one from the one mentioned above, but again the following *) applies 
	-index.html -> source for keycloak.js adapter file is added 
	-js/app.js -> added section for Keycloak authentication for UI 
	
4.In the $APROX_HOME/bin modify security-constraints.json file and define the security constraints as you need.



*) The configuration will be different for different Keycloak server installations and its up to you to install&configure Keycloak server and configure
the "aprox-rest" application on Keycloak server to be able to get the correct keycloak.json file config.


What to change after build summary
------------------------------------
1. Update your $APROX_HOME/bin/boot.properties
2. Update your $APROX_HOME/bin/keycloak.json file with correct configuration
3. Update your $APROX_HOME/var/lib/aprox/ui/keycloak.json file with correct configuration
