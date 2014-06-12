'use strict';

// Declare app level module which depends on filters, and services

var aprox = angular.module('aprox', [
  'ngRoute',
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
  
  $routeProvider.when('/remote', {templateUrl: 'partials/remote-list.html', controller: 'RemoteListCtl'});
  $routeProvider.when('/remote/view/:name', {templateUrl: 'partials/remote-detail.html', controller: 'RemoteDetailCtl'});
  $routeProvider.when('/remote/new', {templateUrl: 'partials/remote-edit.html', controller: 'RemoteEditCtl'});
  $routeProvider.when('/remote/edit/:name', {templateUrl: 'partials/remote-edit.html', controller: 'RemoteEditCtl'});

  $routeProvider.when('/hosted', {templateUrl: 'partials/hosted-list.html', controller: 'HostedListCtl'});
  $routeProvider.when('/hosted/view/:name', {templateUrl: 'partials/hosted-detail.html', controller: 'HostedDetailCtl'});
  $routeProvider.when('/hosted/new', {templateUrl: 'partials/hosted-edit.html', controller: 'HostedEditCtl'});
  $routeProvider.when('/hosted/edit/:name', {templateUrl: 'partials/hosted-edit.html', controller: 'HostedEditCtl'});

  $routeProvider.when('/group', {templateUrl: 'partials/group-list.html', controller: 'GroupListCtl'});
  $routeProvider.when('/group/view/:name', {templateUrl: 'partials/group-detail.html', controller: 'GroupDetailCtl'});
  $routeProvider.when('/group/new', {templateUrl: 'partials/group-edit.html', controller: 'GroupEditCtl'});
  $routeProvider.when('/group/edit/:name', {templateUrl: 'partials/group-edit.html', controller: 'GroupEditCtl'});

  if ( addons !== undefined ){
    addons.items.each( function(addon){
      if( addon.sections !== undefined ){
        addon.sections.each(function(section){
          var options = {};
          options.templateUrl= 'cp/layover/' + section.templateHref;

          if (section.controller !== undefined){
            options.controller= section.controller;
          }

          $routeProvider.when(section.route, options);
        });
      }
    });
  }

  $routeProvider.otherwise({redirectTo: '/remote'});
}]);

