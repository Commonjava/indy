/* Keycloak authentication is disabled. */
var auth={loggedIn: false, keycloak: {authenticated: false, loginRequired: false}};

angular.element(document).ready(function () {
  angular.bootstrap(document, ['indy']);
});
