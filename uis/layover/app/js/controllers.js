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

/* Controllers */

var indyControllers = angular.module('indy.controllers', []);

indyControllers.controller('DisplayHtmlCtl', ['$scope', '$element', function($scope, $element) {
  console.log($element);
  $scope.showHtml = function() {
      alert($element.html());
  };
}]);

indyControllers.controller('NavCtl', ['$scope', function($scope){
  $scope.addon_navs = [];
  if ( auth.loggedIn ){
    $scope.username = auth.keycloak.tokenParsed.name;
  }
  
  if ( addons !== undefined ){
    addons.items.each(function(addon){
      if ( addon.sections !== undefined ){
        addon.sections.each(function(section){
          $scope.addon_navs.push(section);
        });
      }
    });
  }
}]);

indyControllers.controller('RemoteListCtl', ['$scope', '$location', 'RemoteSvc', 'StoreUtilSvc', 'ControlSvc', 'StoreDisableSvc', function($scope, $location, RemoteSvc, StoreUtilSvc, ControlSvc, StoreDisableSvc) {
    ControlSvc.addListingControlHrefs($scope, $location);
  
    StoreDisableSvc.setDisabledMap($scope);

    $scope.remoteOptionLegend = StoreUtilSvc.remoteOptionLegend();
    
    $scope.listing = RemoteSvc.resource.query({}, function(listing){
      if ( listing.items ){
          for(var i=0; i<listing.items.length; i++){
            var item = listing.items[i];
            item.detailHref = StoreUtilSvc.detailHref(item.key);
            item.storeHref = StoreUtilSvc.storeHref(item.key);
            item.packageType = StoreUtilSvc.packageTypeFromKey(item.key);
            item.name = StoreUtilSvc.nameFromKey(item.key);
            item.remoteOptions = StoreUtilSvc.remoteOptions(item);
            item.description = StoreUtilSvc.defaultDescription(item.description);
          }
      }
    });
    $scope.storeUtils = StoreUtilSvc;
    $scope.orderProp = 'key';
  }]);

indyControllers.controller('RemoteCtl', ['$scope', '$routeParams', '$location', 'RemoteSvc', 'StoreUtilSvc', 'ControlSvc', 'StoreDisableSvc', 'PackageTypeSvc', function($scope, $routeParams, $location, RemoteSvc, StoreUtilSvc, ControlSvc, StoreDisableSvc, PackageTypeSvc) {
  $scope.mode = StoreUtilSvc.resourceMode();
  $scope.editMode = ($scope.mode == 'edit');
  $scope.packageTypes = PackageTypeSvc.resource.query();
  $scope.storeUtils = StoreUtilSvc;

  $scope.raw = {
    name: '',
    timeout_seconds: '',
    cache_timeout_seconds: '',
  };
  
  $scope.controls = function( store ){
    $scope.store = store;
    
    ControlSvc.addControlHrefs($scope, 'remote', $scope.raw.packageType, $scope.raw.name, $scope.mode, $location);
    ControlSvc.addStoreControls($scope, $location, 'remote', RemoteSvc, StoreUtilSvc, {
      save: function(scope){
        if ( scope.is_passthrough ){
          delete scope.store.cache_timeout_seconds;
          delete scope.store.metadata_timeout_seconds;
        }
        else{
          scope.store.cache_timeout_seconds = StoreUtilSvc.durationToSeconds(scope.raw.cache_timeout_seconds);
          scope.store.metadata_timeout_seconds = StoreUtilSvc.durationToSeconds(scope.raw.metadata_timeout_seconds, true);
        }

        scope.store.timeout_seconds = StoreUtilSvc.durationToSeconds(scope.raw.timeout_seconds);
      },
    });
  };

  if ($scope.mode == 'new'){
    $scope.raw.new = true;
    $scope.store = {
      url: '',
      timeout_seconds: 60,
      cache_timeout_seconds: 86400,
      metadata_timeout_seconds: 86400,
      is_passthrough: false
    };
    
    $scope.controls( $scope.store );
  }
  else{
    RemoteSvc.resource.get({packageType: $routeParams.packageType, name: $routeParams.name}, function(store){
      $scope.raw.name = StoreUtilSvc.nameFromKey(store.key);
      $scope.raw.packageType = StoreUtilSvc.packageTypeFromKey(store.key);
      $scope.raw.storeHref = StoreUtilSvc.storeHref(store.key);
      $scope.raw.description = StoreUtilSvc.defaultDescription(store.description);

      $scope.raw.cache_timeout_seconds = StoreUtilSvc.secondsToDuration(store.cache_timeout_seconds);
      $scope.raw.timeout_seconds = StoreUtilSvc.secondsToDuration(store.timeout_seconds);
      $scope.raw.metadata_timeout_seconds = StoreUtilSvc.secondsToDuration(store.metadata_timeout_seconds, true);

      var useX509 = store.server_certificate_pem !== undefined;
      useX509 = store.key_certificate_pem !== undefined || useX509;

      $scope.raw.use_x509 = useX509;

      StoreDisableSvc.setEnableAttributes($scope.raw, store, StoreUtilSvc);
//      console.log("VIEW: After calling setEnableAttributes, raw.enabled == " + $scope.raw.enabled);

      var useProxy = store.proxy_host !== undefined;
      $scope.raw.use_proxy = useProxy;

      var useAuth = (useProxy && store.proxy_user !== undefined);
      useAuth = store.user !== undefined || useAuth;

      $scope.raw.use_auth = useAuth;
      
      $scope.controls( store );
    });
  }

}]);

indyControllers.controller('HostedListCtl', ['$scope', '$location', 'HostedSvc', 'StoreUtilSvc', 'ControlSvc', 'StoreDisableSvc', function($scope, $location, HostedSvc, StoreUtilSvc, ControlSvc, StoreDisableSvc) {
    ControlSvc.addListingControlHrefs($scope, $location);
    
    StoreDisableSvc.setDisabledMap($scope);

    $scope.hostedOptionLegend = StoreUtilSvc.hostedOptionLegend();
  
    $scope.listing = HostedSvc.resource.query({}, function(listing){
      if ( listing.items ){
          for(var i=0; i<listing.items.length; i++){
            var item = listing.items[i];
            item.detailHref = StoreUtilSvc.detailHref(item.key);
            item.storeHref = StoreUtilSvc.storeHref(item.key);
            item.packageType = StoreUtilSvc.packageTypeFromKey(item.key);
            item.name = StoreUtilSvc.nameFromKey(item.key);
            item.hostedOptions = StoreUtilSvc.hostedOptions(item);
            item.description = StoreUtilSvc.defaultDescription(item.description);
          }
      }
    });

    $scope.storeUtils = StoreUtilSvc;
    $scope.orderProp = 'key';
  }]);

indyControllers.controller('HostedCtl', ['$scope', '$routeParams', '$location', 'HostedSvc', 'StoreUtilSvc', 'ControlSvc', 'StoreDisableSvc', 'PackageTypeSvc', function($scope, $routeParams, $location, HostedSvc, StoreUtilSvc, ControlSvc, StoreDisableSvc, PackageTypeSvc) {
  $scope.mode = StoreUtilSvc.resourceMode();
  $scope.editMode = ($scope.mode == 'edit');
  $scope.packageTypes = PackageTypeSvc.resource.query();
  $scope.storeUtils = StoreUtilSvc;

  $scope.raw = {
    name: '',
    snapshot_timeout_seconds: '',
  };
  
  $scope.showSnapshotTimeout = function(){
    return $scope.store.allow_snapshots;
  };
  
  $scope.allowUploads = function(){
    return $scope.store.allow_snapshots || $scope.store.allow_releases;
  };
  
  $scope.controls = function( store ){
    $scope.store = store;
    
    ControlSvc.addControlHrefs($scope, 'hosted', $scope.raw.packageType, $scope.raw.name, $scope.mode, $location);
    ControlSvc.addStoreControls($scope, $location, 'hosted', HostedSvc, StoreUtilSvc, {
      save: function(scope){
        if (!$scope.store.allow_snapshots){
          delete $scope.store.snapshotTimeoutSeconds;
        }
        else{
          $scope.store.snapshotTimeoutSeconds = StoreUtilSvc.durationToSeconds($scope.raw.snapshotTimeoutSeconds);
        }
      },
    });
  };

  if($scope.mode == 'new'){
    $scope.raw.new = true;
    $scope.store = {
      allow_releases: true,
      allow_snapshots: true,
      snapshotTimeoutSeconds: 86400,
    };
    
    $scope.controls( $scope.store );
  }
  else{
    HostedSvc.resource.get({packageType: $routeParams.packageType, name: $routeParams.name}, function(store){
      $scope.raw.name = StoreUtilSvc.nameFromKey(store.key);
      $scope.raw.packageType = StoreUtilSvc.packageTypeFromKey(store.key);
      $scope.raw.storeHref = StoreUtilSvc.storeHref(store.key);
      $scope.raw.description = StoreUtilSvc.defaultDescription(store.description);
      $scope.raw.snapshotTimeoutSeconds = StoreUtilSvc.secondsToDuration(store.snapshotTimeoutSeconds);

      StoreDisableSvc.setEnableAttributes($scope.raw, store, StoreUtilSvc);

      $scope.controls(store);
    });
  }
}]);

indyControllers.controller('GroupListCtl', ['$q', '$scope', '$location', 'GroupSvc', 'StoreUtilSvc', 'ControlSvc', 'StoreDisableSvc', 'AllStoreDisableSvc', function($q, $scope, $location, GroupSvc, StoreUtilSvc, ControlSvc, StoreDisableSvc, AllStoreDisableSvc) {
    ControlSvc.addListingControlHrefs($scope, $location);

    StoreDisableSvc.setDisabledMap($scope);

    $scope.listing = GroupSvc.resource.query({}, function(listing){
      if ( listing.items ){
          for(var i=0; i<listing.items.length; i++){
            var item = listing.items[i];
            item.detailHref = StoreUtilSvc.detailHref(item.key);
            item.storeHref = StoreUtilSvc.storeHref(item.key);
            item.type = StoreUtilSvc.typeFromKey( item.key );
            item.packageType = StoreUtilSvc.packageTypeFromKey(item.key);
            item.name = StoreUtilSvc.nameFromKey(item.key);
            item.description = StoreUtilSvc.defaultDescription(item.description);

            item.display = false;

            StoreDisableSvc.setEnableAttributes(item, item, StoreUtilSvc);

            var oldConstituents = item.constituents;
            item.constituents = [oldConstituents.length];
            for( var j=0; j<oldConstituents.length; j++ ){
              var key = oldConstituents[j];
              var c = {
                  key: oldConstituents[j],
                  detailHref: StoreUtilSvc.detailHref(key),
                  storeHref: StoreUtilSvc.storeHref(key),
                  type: StoreUtilSvc.typeFromKey( key ),
                  packageType: StoreUtilSvc.packageTypeFromKey(key),
                  name: StoreUtilSvc.nameFromKey(key),
              }
              item.constituents[j] = c;
            }
          }
      }
    });

    $scope.displayConstituents = function(item){
      item.display = true;
    };
    
    $scope.hideConstituents = function(item){
      item.display = false;
    };

    $scope.hideAll = function(){
      for(var i=0; i<$scope.listing.items.length; i++){
        var item = $scope.listing.items[i];
        $scope.hideConstituents(item);
      }
    }

    $scope.storeUtils = StoreUtilSvc;
    $scope.orderProp = 'key';
  }]);

indyControllers.controller('GroupCtl', ['$scope', '$routeParams', '$location', 'GroupSvc', 'StoreUtilSvc', 'ControlSvc', 'AllEndpointsSvc', 'StoreDisableSvc', 'PackageTypeSvc', function($scope, $routeParams, $location, GroupSvc, StoreUtilSvc, ControlSvc, AllEndpointsSvc, StoreDisableSvc, PackageTypeSvc) {
  $scope.mode = StoreUtilSvc.resourceMode();
  $scope.editMode = ($scope.mode == 'edit');
  $scope.packageTypes = PackageTypeSvc.resource.query();
  $scope.storeUtils = StoreUtilSvc;

  StoreDisableSvc.setDisabledMap($scope);

  $scope.raw = {
    name: '',
    available: [],
    constituentHrefs: {},
  };
  
  $scope.controls = function(store){
    $scope.store = store;
    
    AllEndpointsSvc.resource.query(function(listing){
      $scope.raw.available = StoreUtilSvc.sortEndpoints( listing.items );
    });

    ControlSvc.addControlHrefs($scope, 'group', $scope.raw.packageType, $scope.raw.name, $scope.mode, $location);
    ControlSvc.addStoreControls($scope, $location, 'group', GroupSvc, StoreUtilSvc);
  };

  if ($scope.mode == 'new'){
    $scope.raw.new = true;
    $scope.store = {
      constituents: [],
    };

    $scope.controls($scope.store);
  }
  else{
    GroupSvc.resource.get({packageType: $routeParams.packageType, name: $routeParams.name}, function(store){
      $scope.raw.name = StoreUtilSvc.nameFromKey(store.key);
      $scope.raw.packageType = StoreUtilSvc.packageTypeFromKey(store.key);
      $scope.raw.storeHref = StoreUtilSvc.storeHref(store.key);
      $scope.raw.description = StoreUtilSvc.defaultDescription(store.description);

      StoreDisableSvc.setEnableAttributes($scope.raw, store, StoreUtilSvc);

      if ( !store.constituents ){
        store.constituents = [];
      }

      for(var i=0; i<store.constituents.length; i++){
        var item = store.constituents[i];
        $scope.raw.constituentHrefs[item] = {
          detailHref: StoreUtilSvc.detailHref(item),
        };
      }
      
      $scope.controls(store);
    });
  }
}]);

indyControllers.controller('NfcController', ['$scope', '$routeParams', '$location', 'NfcSvc', 'StoreUtilSvc', 'AllEndpointsSvc',
                                              function($scope, $routeParams, $location, NfcSvc, StoreUtilSvc, AllEndpointsSvc){
  $scope.raw = {
    available: [],
  };
  
  $scope.clearAllNFC = function(){
    NfcSvc.resource.deleteAll();
    $location.path('/nfc');
  };
  
  $scope.clearSection = function(section){
    var key = section.key;
    
// alert( "Clear all NFC entries for: " + key );
    
    var name=StoreUtilSvc.nameFromKey(key);
    var packageType = StoreUtilSvc.packageTypeFromKey(key);
    var type = StoreUtilSvc.typeFromKey(key);
    
    NfcSvc.resource.delete({name: name, type: type, packageType: packageType},
      function(){
        $scope.message = {type: 'OK', message: 'Cleared NFC for ' + key + "'"};
// alert( "NFC for " + key + " has been cleared!");
      }, 
      function(error){
        $scope.message = {type: 'ERROR', message: 'Failed to clear NFC for ' + key + "'", detail: error};
// alert('[ERROR] Failed to clear NFC for ' + key + "'\n" + error );
      }
    );
    
    section.paths = [];
  };
  
  $scope.clearSectionPath = function(section, path){
    path = path.substring(1);
    var key = section.key;
    
// alert( "Clear all NFC entries for: " + key + ", path: " + path );
    
    var name=StoreUtilSvc.nameFromKey(key);
    var packageType = StoreUtilSvc.packageTypeFromKey(key);
    var type = StoreUtilSvc.typeFromKey(key);
    
    NfcSvc.resource.delete({name: name, type: type, path: path, packageType: packageType},
      function(){
        $scope.message = {type: 'OK', message: 'Cleared NFC for ' + key + "', path: " + path};
// alert( "NFC for: " + key + ", path: " + path + " has been cleared!" );
      }, 
      function(error){
        $scope.message = {type: 'ERROR', message: 'Failed to clear NFC for ' + key + "'", detail: error};
// alert('[ERROR] Failed to clear NFC for ' + key + "', path: " + path + "\n" +
// error );
      }
    );
    
    var idx = section.paths.indexOf(path);
    section.paths.splice(idx,1);
  };
  
  $scope.showAll = function(){
    if ( !window.location.hash.startsWith( "#/nfc/view/all" ) ){
      $location.path('/nfc/view/all');
    }
  };
  
  $scope.show = function(){
    if ( !$scope.currentKey ){return;}
    
    var viewPath = '/nfc/view/' + $scope.currentKey.replace(/:/g, '/');
    
    if ( !window.location.hash.startsWith( "#" + viewPath ) ){
      $location.path(viewPath);
    }
  };

  $scope.pageSizes = [10,25,50,100,200];
  $scope.currentPageNumber = 1;
  var scopePageIndex = 0;
  var scopePageSize = 10;

  $scope.changePageSize = function(currentPageSize){
    scopePageSize = currentPageSize;
    queryByPageIndexAndSize(0, scopePageSize);
    $scope.currentPageNumber = 1;
  }
  $scope.changePageNumber = function($event, currentPageNumber){
    if($event.keyCode == 13) {
      scopePageIndex = currentPageNumber - 1;
      queryByPageIndexAndSize(scopePageIndex, scopePageSize);
    }
  }

  $scope.prevPage = function(){
    scopePageIndex--;
    queryByPageIndexAndSize(scopePageIndex, scopePageSize);
    $scope.currentPageNumber = scopePageIndex + 1;
    $scope.prevDisabled = false;
    if(scopePageIndex <= 0) {
      $scope.prevDisabled = true;
    }
  }

  $scope.nextPage = function(){
    scopePageIndex++;
    queryByPageIndexAndSize(scopePageIndex, scopePageSize);
    $scope.currentPageNumber = scopePageIndex + 1;
  }

  AllEndpointsSvc.resource.query(function(listing){
    var available = [];
    listing.items.each(function(item){
      if (item.type == ( "group" ))
      {
        return;
      }
      item.key = StoreUtilSvc.formatKey(item.packageType, item.type, item.name);
      item.label = StoreUtilSvc.keyLabel(item.key);
      available.push(item);
    });
    
    $scope.raw.available = StoreUtilSvc.sortEndpoints( available );
  });

  function queryByPageIndexAndSize(index, size){
      $scope.prevDisabled = false;
      if(index <= 0) {
        $scope.prevDisabled = true;
      }
      $scope.paginationHidden = true;
      if ( window.location.hash == ( "#/nfc/view/all" ) ){
        // alert( "showing all NFC entries");
        $scope.paginationHidden = false;
        delete $scope.currentKey;
        NfcSvc.resource.query({pageIndex: index , pageSize: size}, function(nfc){
          if ( nfc.sections !== undefined ){
            nfc.sections.each(function(section){
              section.label = StoreUtilSvc.keyLabel(section.key);
              section.paths.sort();
            });
          }
          $scope.sections = StoreUtilSvc.sortByEmbeddedKey(nfc.sections);
        });
      }
      else{
        var routePackageType = $routeParams.packageType;
        var routeType = $routeParams.type;
        var routeName = $routeParams.name;
        if ( routeType !== undefined && routeName !== undefined ){
          $scope.paginationHidden = false;
          $scope.currentKey = StoreUtilSvc.formatKey(routePackageType, routeType, routeName);

          // alert( "showing NFC entries for: " + $scope.currentKey);
          NfcSvc.resource.get({packageType: routePackageType, type:routeType, name:routeName, pageIndex: index, pageSize: size}, function(nfc){
            if ( nfc.sections !== undefined ){
              nfc.sections.each(function(section){
                section.label = StoreUtilSvc.keyLabel(section.key);
                section.paths.sort();
              });
            }

            $scope.sections = StoreUtilSvc.sortByEmbeddedKey(nfc.sections);
          });
        }
      }
  }

  queryByPageIndexAndSize(0,10);
}]);

indyControllers.controller('CacheCtl', ['$scope', '$routeParams', 'CacheSvc', 'ControlSvc', function($scope, $routeParams, CacheSvc, ControlSvc) {
  $scope.path = '';
  $scope.data = '';
  $scope.raw = {
  };
  $scope.deleteCache = function(){
    if ( !$scope.path || $scope.path.length < 1 ){
      alert( "You must provide a path!" );
      return;
    }
    CacheSvc.remove($scope, $scope.path, ControlSvc);
  };
}]);

indyControllers.controller('FooterCtl', ['$scope', 'FooterSvc', function($scope, FooterSvc){
  $scope.stats = FooterSvc.resource.query();
}]);

indyControllers.controller('LogoutCtl', ['$scope', '$location', function($scope, $location){
  if ( auth.loggedIn ){
    console.log("Logging out.");
    auth.logout();
  }
  else{
    console.log("Not logged in.");
    window.location = '/index.html';
  }
}]);

