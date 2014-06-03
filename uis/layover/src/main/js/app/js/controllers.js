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

aproxControllers.controller('RemoteEditCtl', ['$scope', '$routeParams', 'RemoteSvc', 'StoreUtilSvc', function($scope, $routeParams, RemoteSvc, StoreUtilSvc) {
  $scope.editMode = window.location.hash.startsWith( "#/remote/edit" );
  $scope.storeUtils = StoreUtilSvc;

  $scope.raw = {
    name: '',
    timeout_seconds: '',
    cache_timeout_seconds: '',
  };

  if ( $scope.editMode ){
    $scope.store = RemoteSvc.get({name: $routeParams.name}, function(store){
      $scope.raw.name = StoreUtilSvc.nameFromKey(store.key);
      $scope.raw.cache_timeout_seconds = StoreUtilSvc.secondsToDuration(store.cache_timeout_seconds);
      $scope.raw.timeout_seconds = StoreUtilSvc.secondsToDuration(store.timeout_seconds);
    });
  }
  else{
    $scope.store = {
      url: '',
      timeout_seconds: 60,
      cache_timeout_seconds: 86400,
      is_passthrough: false
    };
  }

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

aproxControllers.controller('HostedEditCtl', ['$scope', '$routeParams', 'HostedSvc', 'StoreUtilSvc', function($scope, $routeParams, HostedSvc, StoreUtilSvc) {
  $scope.editMode = window.location.hash.startsWith( "#/hosted/edit" );
  $scope.storeUtils = StoreUtilSvc;

  $scope.raw = {
    name: '',
    snapshot_timeout_seconds: '',
  };

  if ( $scope.editMode ){
    $scope.store = HostedSvc.get({name: $routeParams.name}, function(store){
      $scope.raw.name = $scope.storeUtils.nameFromKey(store.key);
      $scope.raw.snapshotTimeoutSeconds = StoreUtilSvc.secondsToDuration(store.snapshotTimeoutSeconds);
    });
  }
  else{
    $scope.store = {
      allow_releases: true,
      allow_snapshots: true,
      snapshotTimeoutSeconds: 86400,
    };
  }

  $scope.allowUploads = function(store){
    return store.allow_snapshots || store.allow_releases;
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

aproxControllers.controller('GroupEditCtl', ['$scope', '$routeParams', 'GroupSvc', 'StoreUtilSvc', 'AllEndpointsSvc', function($scope, $routeParams, GroupSvc, StoreUtilSvc, AllEndpointsSvc) {
  $scope.editMode = window.location.hash.startsWith( "#/group/edit" );
  $scope.storeUtils = StoreUtilSvc;

  $scope.raw = {
    name: '',
    available: [],
  };

  if ( $scope.editMode ){
    $scope.store = GroupSvc.get({name: $routeParams.name}, function(store){
      $scope.raw.name = $scope.storeUtils.nameFromKey(store.key);
    });
  }
  else{
    $scope.store = {
    };
  }

  AllEndpointsSvc.query(function(listing){
    $scope.raw.available = StoreUtilSvc.sortEndpoints( listing.items );
  });
}]);

aproxControllers.controller('FooterCtl', ['$scope', '$http', 'FooterSvc', function($scope, $http, FooterSvc){
    $scope.stats = FooterSvc.query();
  }]);

