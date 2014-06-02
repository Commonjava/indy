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

aproxControllers.controller('RemoteDetailCtl', ['$scope', '$routeParams', 'RemoteSvc', 'StoreUtilSvc', function($scope, $routeParams, RemoteSvc, StoreUtilSvc) {
    $scope.store = RemoteSvc.get({name: $routeParams.name});
    $scope.storeUtils = StoreUtilSvc;
  }]);

aproxControllers.controller('RemoteNewCtl', ['$scope', '$routeParams', 'RemoteSvc', 'StoreUtilSvc', 'StoreControllerSvc',
                                             function($scope, $routeParams, RemoteSvc, StoreUtilSvc, StoreControllerSvc) {
  StoreControllerSvc.initRemoteModification( $scope, false, RemoteSvc, StoreUtilSvc, $routeParams );
}]);

aproxControllers.controller('RemoteEditCtl', ['$scope', '$routeParams', 'RemoteSvc', 'StoreUtilSvc', 'StoreControllerSvc',
                                              function($scope, $routeParams, RemoteSvc, StoreUtilSvc, StoreControllerSvc) {
  StoreControllerSvc.initRemoteModification( $scope, true, RemoteSvc, StoreUtilSvc, $routeParams );
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

aproxControllers.controller('HostedDetailCtl', ['$scope', '$routeParams', 'HostedSvc', 'StoreUtilSvc', function($scope, $routeParams, HostedSvc, StoreUtilSvc) {
    $scope.store = HostedSvc.get({name: $routeParams.name});
    $scope.storeUtils = StoreUtilSvc;

    $scope.allowUploads = function(store){
      return store.allow_snapshots || store.allow_releases;
    };

    $scope.showSnapshotTimeout = function(store){
      return store.allow_snapshots;
    };
  }]);

aproxControllers.controller('HostedNewCtl', ['$scope', '$routeParams', 'HostedSvc', 'StoreUtilSvc', 'StoreControllerSvc',
                                             function($scope, $routeParams, HostedSvc, StoreUtilSvc, StoreControllerSvc) {
  StoreControllerSvc.initHostedModification( $scope, false, HostedSvc, StoreUtilSvc, $routeParams );

    $scope.allowUploads = function(store){
      return store.allow_snapshots || store.allow_releases;
    };

    $scope.showSnapshotTimeout = function(store){
      return store.allow_snapshots;
    };
}]);

aproxControllers.controller('HostedEditCtl', ['$scope', '$routeParams', 'HostedSvc', 'StoreUtilSvc', 'StoreControllerSvc',
                                              function($scope, $routeParams, HostedSvc, StoreUtilSvc, StoreControllerSvc) {
  StoreControllerSvc.initHostedModification( $scope, true, HostedSvc, StoreUtilSvc, $routeParams );

    $scope.allowUploads = function(store){
      return store.allow_snapshots || store.allow_releases;
    };

    $scope.showSnapshotTimeout = function(store){
      return store.allow_snapshots;
    };
}]);

aproxControllers.controller('GroupListCtl', ['$scope', 'GroupSvc', 'StoreUtilSvc', function($scope, GroupSvc, StoreUtilSvc) {
    $scope.listing = GroupSvc.query();
    $scope.storeUtils = StoreUtilSvc;
    $scope.orderProp = 'key';
  }]);

aproxControllers.controller('GroupDetailCtl', ['$scope', '$routeParams', 'GroupSvc', 'StoreUtilSvc', function($scope, $routeParams, GroupSvc, StoreUtilSvc) {
    $scope.store = GroupSvc.get({name: $routeParams.name});
    $scope.storeUtils = StoreUtilSvc;
  }]);

aproxControllers.controller('FooterCtl', ['$scope', '$http', 'FooterSvc', function($scope, $http, FooterSvc){
    $scope.stats = FooterSvc.query();
  }]);

