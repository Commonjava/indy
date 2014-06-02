'use strict';


// Declare app level module which depends on filters, and services
angular.module('aprox', [
  'ngRoute',
  'aprox.filters',
  'aprox.directives',
  'aprox.services',
  'aprox.controllers'
]).

// NOTE: In the routes below, the '#' route prefix is implied.
config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/remote', {templateUrl: 'partials/remote-list.html', controller: 'RemoteListCtl'});
  $routeProvider.when('/remote/view/:name', {templateUrl: 'partials/remote-detail.html', controller: 'RemoteDetailCtl'});
  $routeProvider.when('/remote/new', {templateUrl: 'partials/remote-edit.html', controller: 'RemoteNewCtl'});
  $routeProvider.when('/remote/edit/:name', {templateUrl: 'partials/remote-edit.html', controller: 'RemoteEditCtl'});

  $routeProvider.when('/hosted', {templateUrl: 'partials/hosted-list.html', controller: 'HostedListCtl'});
  $routeProvider.when('/hosted/:name', {templateUrl: 'partials/hosted-detail.html', controller: 'HostedDetailCtl'});
  $routeProvider.when('/hosted/new', {templateUrl: 'partials/hosted-edit.html', controller: 'HostedNewCtl'});
  $routeProvider.when('/hosted/:name/edit', {templateUrl: 'partials/hosted-edit.html', controller: 'HostedEditCtl'});

  $routeProvider.when('/group', {templateUrl: 'partials/group-list.html', controller: 'GroupListCtl'});
  $routeProvider.when('/group/:name', {templateUrl: 'partials/group-detail.html', controller: 'GroupDetailCtl'});
  $routeProvider.when('/group/new', {templateUrl: 'partials/group-edit.html', controller: 'GroupNewCtl'});
  $routeProvider.when('/group/:name/edit', {templateUrl: 'partials/group-edit.html', controller: 'GroupEditCtl'});

  $routeProvider.otherwise({redirectTo: '/remote'});
}]);
