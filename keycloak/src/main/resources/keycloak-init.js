var auth = {};

angular.element(document).ready(function () {
  var keycloak = new Keycloak(@json@);
  auth.loggedIn = false;

  keycloak.init({ onLoad: 'login-required' }).success(function () {
    auth.loggedIn = true;
    auth.keycloak = keycloak;
    auth.logout = function() {
      auth.loggedIn = false;
      auth.keycloak = null;
      window.location = keycloak.authServerUrl + '/realms/@realm@/tokens/logout?redirect_uri=/index.html';
    };
    angular.bootstrap(document, ['aprox']);
  }).error(function () {
    window.location.reload();
  });

});

aprox.factory('Auth', function () {
  return auth;
});

aprox.factory('authInterceptor', function ($q, $log, Auth) {
  return {
    request: function (config) {
      var deferred = $q.defer();

      if (Auth.keycloak && Auth.keycloak.token) {
        Auth.keycloak.updateToken(5).success(function () {
          config.headers = config.headers || {};
          config.headers.Authorization = 'Bearer ' + Auth.keycloak.token;

          deferred.resolve(config);
        }).error(function () {
          deferred.reject('Failed to refresh token');
        });
      }
      return deferred.promise;
    }
  };
});

aprox.config(function ($httpProvider) {
  $httpProvider.interceptors.push('authInterceptor');
});
