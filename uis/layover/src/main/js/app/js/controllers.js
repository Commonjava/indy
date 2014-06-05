'use strict';

/* Controllers */

var aproxControllers = angular.module('aprox.controllers', []);

aproxControllers.controller('RemoteListCtl', ['$scope', 'RemoteSvc', 'StoreUtilSvc', function($scope, RemoteSvc, StoreUtilSvc) {
    $scope.listing = RemoteSvc.query({}, function(listing){
      for(var i=0; i<listing.items.length; i++){
        var item = listing.items[i];
        item.detailHref = StoreUtilSvc.detailHref(item.key);
        item.storeHref = StoreUtilSvc.storeHref(item.key);
        item.name = StoreUtilSvc.nameFromKey(item.key);
      }
    });
    $scope.storeUtils = StoreUtilSvc;
    $scope.orderProp = 'key';
  }]);

aproxControllers.controller('RemoteDetailCtl', ['$scope', '$routeParams', '$location', 'RemoteSvc', 'StoreUtilSvc', function($scope, $routeParams, $location, RemoteSvc, StoreUtilSvc) {
  $scope.raw = {
    name: '',
  };

  $scope.store = RemoteSvc.get({name: $routeParams.name}, function(store){
      $scope.raw.name = StoreUtilSvc.nameFromKey(store.key);
      $scope.raw.editHref = StoreUtilSvc.editHref(store.key);
      $scope.raw.storeHref = StoreUtilSvc.storeHref(store.key);

      var useX509 = store.server_certificate_pem !== undefined;
      useX509 = store.key_certificate_pem !== undefined || useX509;

      $scope.raw.use_x509 = useX509;

      var useProxy = store.proxy_host !== undefined;
      $scope.raw.use_proxy = useProxy;

      var useAuth = (useProxy && store.proxy_user !== undefined);
      useAuth = store.user !== undefined || useAuth;

      $scope.raw.use_auth = useAuth;
  });

  $scope.storeUtils = StoreUtilSvc;

  $scope.delete = function(){
    if ( confirm( "Really delete '" + $scope.raw.name + "'??") )
    {
      RemoteSvc.delete({name: $scope.raw.name}, function(){
        $location.path( '/remote' );
      });
    }
    else{
      $location.path( '/remote' );
    }
  };

}]);

aproxControllers.controller('RemoteEditCtl', ['$scope', '$routeParams', '$location', 'RemoteSvc', 'StoreUtilSvc', function($scope, $routeParams, $location, RemoteSvc, StoreUtilSvc) {
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
      $scope.raw.storeHref = StoreUtilSvc.storeHref(store.key);
      $scope.raw.cache_timeout_seconds = StoreUtilSvc.secondsToDuration(store.cache_timeout_seconds);
      $scope.raw.timeout_seconds = StoreUtilSvc.secondsToDuration(store.timeout_seconds);

      var useX509 = store.server_certificate_pem !== undefined;
      useX509 = store.key_certificate_pem !== undefined || useX509;

      $scope.raw.use_x509 = useX509;

      var useProxy = store.proxy_host !== undefined;
      $scope.raw.use_proxy = useProxy;

      var useAuth = useProxy && store.proxy_user !== undefined;
      useAuth = store.user !== undefined || useAuth;

      $scope.raw.use_auth = useAuth;
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

  $scope.save = function(){
    if ( $scope.is_passthrough ){
      delete $scope.store.cache_timeout_seconds;
    }
    else{
      $scope.store.cache_timeout_seconds = StoreUtilSvc.durationToSeconds($scope.raw.cache_timeout_seconds);
    }

    $scope.store.timeout_seconds = StoreUtilSvc.durationToSeconds($scope.raw.timeout_seconds);

    if ( $scope.editMode ){
      RemoteSvc.update({name: $scope.raw.name}, $scope.store, function(){
        $location.path( '/remote' );
      });
    }
    else{
      $scope.store.key = StoreUtilSvc.formatKey('remote', $scope.raw.name);
      RemoteSvc.create({}, $scope.store, function(){
        $location.path( '/remote' );
      });
    }
  };

  $scope.delete = function(){
    if ( confirm( "Really delete '" + $scope.raw.name + "'??") )
    {
      RemoteSvc.delete({name: $scope.raw.name}, function(){
        $location.path( '/remote' );
      });
    }
    else{
      $location.path( '/remote' );
    }
  };

  $scope.cancel = function(){
    $location.path( '/remote' );
  };

}]);

aproxControllers.controller('HostedListCtl', ['$scope', 'HostedSvc', 'StoreUtilSvc', function($scope, HostedSvc, StoreUtilSvc) {
    $scope.listing = HostedSvc.query({}, function(listing){
      for(var i=0; i<listing.items.length; i++){
        var item = listing.items[i];
        item.detailHref = StoreUtilSvc.detailHref(item.key);
        item.storeHref = StoreUtilSvc.storeHref(item.key);
        item.name = StoreUtilSvc.nameFromKey(item.key);
        item.hostedOptions = StoreUtilSvc.hostedOptions(item);
      }
    });

    $scope.storeUtils = StoreUtilSvc;
    $scope.orderProp = 'key';
  }]);

aproxControllers.controller('HostedDetailCtl', ['$scope', '$routeParams', '$location', 'HostedSvc', 'StoreUtilSvc', function($scope, $routeParams, $location, HostedSvc, StoreUtilSvc) {
  $scope.raw = {};

  $scope.store = HostedSvc.get({name: $routeParams.name}, function(store){
    $scope.raw.name = StoreUtilSvc.nameFromKey(store.key);
    $scope.raw.storeHref = StoreUtilSvc.storeHref(store.key);
    $scope.raw.editHref = StoreUtilSvc.editHref(store.key);
  });
  $scope.storeUtils = StoreUtilSvc;

  $scope.allowUploads = function(store){
    return store.allow_snapshots || store.allow_releases;
  };

  $scope.showSnapshotTimeout = function(store){
    return store.allow_snapshots;
  };

  $scope.delete = function(){
    if ( confirm( "Really delete '" + $scope.raw.name + "'??") )
    {
      HostedSvc.delete({name: $scope.raw.name}, function(){
        $location.path( '/hosted' );
      });
    }
    else{
      $location.path( '/hosted' );
    }
  };

}]);

aproxControllers.controller('HostedEditCtl', ['$scope', '$routeParams', '$location', 'HostedSvc', 'StoreUtilSvc', function($scope, $routeParams, $location, HostedSvc, StoreUtilSvc) {
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

  $scope.save = function(){
    if (!$scope.store.allow_snapshots){
      delete $scope.store.snapshotTimeoutSeconds;
    }
    else{
      $scope.store.snapshotTimeoutSeconds = StoreUtilSvc.durationToSeconds($scope.raw.snapshotTimeoutSeconds);
    }

    if ( $scope.editMode ){
      HostedSvc.update({name: $scope.raw.name}, $scope.store, function(){
        $location.path( '/hosted' );
      });
    }
    else{
      $scope.store.key = StoreUtilSvc.formatKey('hosted', $scope.raw.name);
      HostedSvc.create({}, $scope.store, function(){
        $location.path( '/hosted' );
      });
    }
  };

  $scope.delete = function(){
    if ( confirm( "Really delete '" + $scope.raw.name + "'??") )
    {
      HostedSvc.delete({name: $scope.raw.name}, function(){
        $location.path( '/hosted' );
      });
    }
    else{
      $location.path( '/hosted' );
    }
  };

  $scope.cancel = function(){
    $location.path( '/hosted' );
  };

}]);

aproxControllers.controller('GroupListCtl', ['$scope', 'GroupSvc', 'StoreUtilSvc', function($scope, GroupSvc, StoreUtilSvc) {
    $scope.listing = GroupSvc.query({}, function(listing){
      for(var i=0; i<listing.items.length; i++){
        var item = listing.items[i];
        item.detailHref = StoreUtilSvc.detailHref(item.key);
        item.storeHref = StoreUtilSvc.storeHref(item.key);
        item.name = StoreUtilSvc.nameFromKey(item.key);
      }
    });

    $scope.storeUtils = StoreUtilSvc;
    $scope.orderProp = 'key';
  }]);

aproxControllers.controller('GroupDetailCtl', ['$scope', '$routeParams', '$location', 'GroupSvc', 'StoreUtilSvc', function($scope, $routeParams, $location, GroupSvc, StoreUtilSvc) {
  $scope.raw = {
    constituentHrefs: {},
  };

  $scope.store = GroupSvc.get({name: $routeParams.name}, function(store){
    $scope.raw.name = StoreUtilSvc.nameFromKey(store.key);
    $scope.raw.editHref = StoreUtilSvc.editHref(store.key);
    $scope.raw.storeHref = StoreUtilSvc.storeHref(store.key);

    for(var i=0; i<store.constituents.length; i++){
      var item = store.constituents[i];
      $scope.raw.constituentHrefs[item] = {
        detailHref: StoreUtilSvc.detailHref(item),
      };
    }
  });
  $scope.storeUtils = StoreUtilSvc;

  $scope.delete = function(){
    if ( confirm( "Really delete '" + $scope.raw.name + "'??") )
    {
      GroupSvc.delete({name: $scope.raw.name}, function(){
        $location.path( '/group' );
      });
    }
    else{
      $location.path( '/group' );
    }
  };

}]);

aproxControllers.controller('GroupEditCtl', ['$scope', '$routeParams', '$location', 'GroupSvc', 'StoreUtilSvc', 'AllEndpointsSvc', function($scope, $routeParams, $location, GroupSvc, StoreUtilSvc, AllEndpointsSvc) {
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
      constituents: [],
    };
  }

  AllEndpointsSvc.query(function(listing){
    $scope.raw.available = StoreUtilSvc.sortEndpoints( listing.items );
  });

  $scope.addConstituent = function(constituent){
    alert("FOO" + constituent);
  };

  $scope.save = function(){
    if ( $scope.editMode ){
      GroupSvc.update({name: $scope.raw.name}, $scope.store, function(){
        $location.path( '/group' );
      });
    }
    else{
      $scope.store.key = StoreUtilSvc.formatKey('group', $scope.raw.name);
      GroupSvc.create({}, $scope.store, function(){
        $location.path( '/group' );
      });
    }
  };

  $scope.delete = function(){
    if ( confirm( "Really delete '" + $scope.raw.name + "'??") )
    {
      GroupSvc.delete({name: $scope.raw.name}, function(){
        $location.path( '/group' );
      });
    }
    else{
      $location.path( '/group' );
    }
  };

  $scope.cancel = function(){
    $location.path( '/group' );
  };

}]);

aproxControllers.controller('FooterCtl', ['$scope', '$http', 'FooterSvc', function($scope, $http, FooterSvc){
    $scope.stats = FooterSvc.query();
  }]);

