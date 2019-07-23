/*
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

var indy = angular.module('indy', [
  'ngRoute',
  'ngDialog',
  'indy.filters',
  'indy.directives',
  'indy.services',
  'indy.controllers',
  'indy.addons'
]);


// NOTE: In the routes below, the '#' route prefix is implied.
indy.config(['$routeProvider', '$controllerProvider', '$compileProvider', '$filterProvider', '$provide',
              function($routeProvider, $controllerProvider, $compileProvider, $filterProvider, $provide) {
  
  indy.routeProvider = $routeProvider;
  indy.controllerProvider = $controllerProvider;
  indy.compileProvider = $compileProvider;
  indy.filterProvider = $filterProvider;
  indy.provide = $provide;
  
  $routeProvider.when('/remote', {templateUrl: 'partials/remote-list.html'});
  $routeProvider.when('/remote/:packageType/view/:name', {templateUrl: 'partials/remote-detail.html'});
  $routeProvider.when('/remote/new', {templateUrl: 'partials/remote-edit.html'});
  $routeProvider.when('/remote/:packageType/edit/:name', {templateUrl: 'partials/remote-edit.html'});

  $routeProvider.when('/hosted', {templateUrl: 'partials/hosted-list.html'});
  $routeProvider.when('/hosted/:packageType/view/:name', {templateUrl: 'partials/hosted-detail.html'});
  $routeProvider.when('/hosted/new', {templateUrl: 'partials/hosted-edit.html'});
  $routeProvider.when('/hosted/:packageType/edit/:name', {templateUrl: 'partials/hosted-edit.html'});

  $routeProvider.when('/group', {templateUrl: 'partials/group-list.html'});
  $routeProvider.when('/group/:packageType/view/:name', {templateUrl: 'partials/group-detail.html'});
  $routeProvider.when('/group/new', {templateUrl: 'partials/group-edit.html'});
  $routeProvider.when('/group/:packageType/edit/:name', {templateUrl: 'partials/group-edit.html'});
  
  $routeProvider.when('/nfc', {templateUrl: 'partials/nfc.html'});
  $routeProvider.when('/nfc/view/all', {templateUrl: 'partials/nfc.html'});
  $routeProvider.when('/nfc/view/:packageType/:type/:name', {templateUrl: 'partials/nfc.html'});

  $routeProvider.when('/cache/delete', {templateUrl: 'partials/cache-delete.html'});

  $routeProvider.when('/rest-api', {templateUrl: 'partials/rest-api.html'})
  
  $routeProvider.when('/logout', {template: " ", controller: 'LogoutCtl'})

  
  if ( typeof addons !== 'undefined' ){
    addons.items.each( function(addon){
      if( typeof addon.routes !== 'undefined' ){
        addon.routes.each(function(route){
          var options = {};
          options.templateUrl= 'layover/' + route.templateHref;

          $routeProvider.when(route.route, options);
        });
      }
    });
  }

//  $routeProvider.otherwise({redirectTo: '/remote'});
}]);
