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

