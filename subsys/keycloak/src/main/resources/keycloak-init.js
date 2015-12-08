var auth = {};

angular.element(document).ready(function () {
  var keycloak = new Keycloak('api/security/keycloak.json');
  auth.loggedIn = false;

  keycloak.init({ onLoad: 'login-required' }).success(function () {
    auth.loggedIn = true;
    auth.keycloak = keycloak;
    auth.logout = function() {
      auth.loggedIn = false;
      auth.keycloak = null;
      window.location = buildUrl( keycloak.authServerUrl, '/realms/' + keycloak.realm + '/tokens/logout?redirect_uri=' + appUrl('/index.html') );
    };
    angular.bootstrap(document, ['indy']);
  }).error(function () {
    alert("Reloading window after login error");
    window.location.reload();
  });

});

indy.factory('Auth', function () {
  return auth;
});

indy.factory('authInterceptor', function ($q, $log, Auth) {
  return {
    request: function (config) {
      var deferred = $q.defer();
      
      if (Auth.keycloak && Auth.keycloak.token) {
        Auth.keycloak.updateToken(5).success(function () {
          config.headers = config.headers || {};
          config.headers.Authorization = 'Bearer ' + Auth.keycloak.token;

//          alert("injecting authorization: " + JSON.stringify(config));
          deferred.resolve(config);
        }).error(function () {
//          alert("rejected token! request abandoned: " + JSON.stringify(config));
          deferred.reject('Failed to refresh token');
        });
      }
      else{
//        alert("NOT injecting authorization: " + JSON.stringify(config));
        deferred.resolve(config);
      }
      return deferred.promise;
    }
  };
});

indy.config(function ($httpProvider) {
  $httpProvider.interceptors.push('authInterceptor');
});
