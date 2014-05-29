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
  $routeProvider.when('/hosted', {templateUrl: 'partials/hosted-list.html', controller: 'HostedListCtl'});
  $routeProvider.when('/group', {templateUrl: 'partials/group-list.html', controller: 'GroupListCtl'});
  $routeProvider.otherwise({redirectTo: '/remote'});
}]);
