/*
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

// Declare app level module which depends on filters, and services

var aprox = angular.module('aprox', [
  'ngRoute',
  'ngDialog',
  'aprox.filters',
  'aprox.directives',
  'aprox.services',
  'aprox.controllers'
]);


// NOTE: In the routes below, the '#' route prefix is implied.
aprox.config(['$routeProvider', '$controllerProvider', '$compileProvider', '$filterProvider', '$provide', 
              function($routeProvider, $controllerProvider, $compileProvider, $filterProvider, $provide) {
  
  aprox.routeProvider = $routeProvider;
  aprox.controllerProvider = $controllerProvider;
  aprox.compileProvider = $compileProvider;
  aprox.filterProvider = $filterProvider;
  aprox.provide = $provide;
  
  $routeProvider.when('/remote', {templateUrl: 'partials/remote-list.html'});
  $routeProvider.when('/remote/view/:name', {templateUrl: 'partials/remote-detail.html'});
  $routeProvider.when('/remote/new', {templateUrl: 'partials/remote-edit.html'});
  $routeProvider.when('/remote/edit/:name', {templateUrl: 'partials/remote-edit.html'});

  $routeProvider.when('/hosted', {templateUrl: 'partials/hosted-list.html'});
  $routeProvider.when('/hosted/view/:name', {templateUrl: 'partials/hosted-detail.html'});
  $routeProvider.when('/hosted/new', {templateUrl: 'partials/hosted-edit.html'});
  $routeProvider.when('/hosted/edit/:name', {templateUrl: 'partials/hosted-edit.html'});

  $routeProvider.when('/group', {templateUrl: 'partials/group-list.html'});
  $routeProvider.when('/group/view/:name', {templateUrl: 'partials/group-detail.html'});
  $routeProvider.when('/group/new', {templateUrl: 'partials/group-edit.html'});
  $routeProvider.when('/group/edit/:name', {templateUrl: 'partials/group-edit.html'});
  
  $routeProvider.when('/nfc', {templateUrl: 'partials/nfc.html'});
  $routeProvider.when('/nfc/view/all', {templateUrl: 'partials/nfc.html'});
  $routeProvider.when('/nfc/view/:type/:name', {templateUrl: 'partials/nfc.html'});

  
  if ( addons !== undefined ){
    addons.items.each( function(addon){
      if( addon.routes !== undefined ){
        addon.routes.each(function(route){
          var options = {};
          options.templateUrl= 'layover/' + route.templateHref;

          $routeProvider.when(route.route, options);
        });
      }
    });
  }

  $routeProvider.otherwise({redirectTo: '/remote'});
}]);

//Declare Auth for Keycloak

var auth = {};

angular.element(document).ready(function () {
  var keycloak = new Keycloak('keycloak.json');
  auth.loggedIn = false;

  keycloak.init({ onLoad: 'login-required' }).success(function () {
    auth.loggedIn = true;
    auth.keycloak = keycloak;
    auth.logout = function() {
      auth.loggedIn = false;
      auth.keycloak = null;
      window.location = keycloak.authServerUrl + '/realms/PNC.REDHAT.COM/tokens/logout?redirect_uri=/index.html';
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


// >>> end auth
