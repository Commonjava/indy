'use strict';


// Declare app level module which depends on filters, and services
angular.module('aprox', [
  'ngRoute',
  'aprox.services',
  'aprox.controllers'
]).

// NOTE: In the routes below, the '#' route prefix is implied.
config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/remote', {templateUrl: 'partials/remote-list.html', controller: 'RemoteListCtl'});
  $routeProvider.when('/remote/:name', {templateUrl: 'partials/remote-detail.html', controller: 'RemoteDetailCtl'});
  $routeProvider.when('/hosted', {templateUrl: 'partials/hosted-list.html', controller: 'HostedListCtl'});
  $routeProvider.when('/hosted/:name', {templateUrl: 'partials/hosted-detail.html', controller: 'HostedDetailCtl'});
  $routeProvider.when('/group', {templateUrl: 'partials/group-list.html', controller: 'GroupListCtl'});
  $routeProvider.when('/group/:name', {templateUrl: 'partials/group-detail.html', controller: 'GroupDetailCtl'});
  $routeProvider.otherwise({redirectTo: '/remote'});
}]);
