'use strict';

/* Controllers */

var aproxControllers = angular.module('aprox.controllers', []);

aproxControllers.controller('RemoteListCtl', ['$scope', 'RemoteSvc', 'StoreUtilSvc', function($scope, RemoteSvc, StoreUtilSvc) {
    $scope.listing = RemoteSvc.query();
    /*{}, function(listing){
      for( var i=0; i<listing.items.length; i++){
        alert(JSON.stringify(listing.items[i], undefined, 2));
      }
    });*/
    $scope.storeUtils = StoreUtilSvc;
    $scope.orderProp = 'key';
  }]);

aproxControllers.controller('HostedListCtl', ['$scope', 'HostedSvc', 'StoreUtilSvc', function($scope, HostedSvc, StoreUtilSvc) {
    $scope.listing = HostedSvc.query();
    /*{}, function(listing){
      for( var i=0; i<listing.items.length; i++){
        alert(JSON.stringify(listing.items[i], undefined, 2));
      }
    });*/
    $scope.storeUtils = StoreUtilSvc;
    $scope.orderProp = 'key';
  }]);

aproxControllers.controller('GroupListCtl', ['$scope', 'GroupSvc', 'StoreUtilSvc', function($scope, GroupSvc, StoreUtilSvc) {
    $scope.listing = GroupSvc.query();
    $scope.storeUtils = StoreUtilSvc;
    $scope.orderProp = 'key';
  }]);

aproxControllers.controller('FooterCtl', ['$scope', '$http', 'FooterSvc', function($scope, $http, FooterSvc){
    $scope.stats = FooterSvc.query();
  }]);

