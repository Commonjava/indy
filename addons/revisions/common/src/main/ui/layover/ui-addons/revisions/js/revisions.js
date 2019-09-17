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
var indy = angular.module('indy');

indy.provide.factory('StoreChangelogSvc', ['$resource', function($resource){
  return {
    resource: $resource(appPath('/api/revisions/changelog/store/:type/:name'), {}, {
      list: {
        method:'GET', 
        params:{type: '', name:''}, 
        isArray:false
      },
    }),
    viewStorePath: function(type, name){
      return '/revisions/changelog/store/' + type + '/' + name;
    },
  };
}]);


indy.controllerProvider.register('StoreChangelogCtl', ['$scope', '$location', '$q', 'StoreChangelogSvc', 'AllEndpointsSvc', 'StoreUtilSvc',
                                                            function($scope, $location, $q, StoreChangelogSvc, AllEndpointsSvc, StoreUtilSvc) {
  var tz = -1 * (new Date().getTimezoneOffset() / 60);
  var tzLabel = 'GMT';
  if ( tz < 0 ){
    tzLabel += tz;
  }
  else if ( tz > 0 ){
    tzLabel += '+' + tz;
  }

  $scope.form = {
      key: '',
      timezone: tzLabel,
      start: 0,
      count: 10,
  };
  
  $scope.loadEndpoints = function(){
    var deferred = $q.defer();
    
    $scope.form.available = [];
    console.log( "Loading endpoint listing");
    AllEndpointsSvc.resource.query().$promise.then(function(listing){
      for(var i=0; i<listing.items.length; i++){
        var item = listing.items[i];
        var key = StoreUtilSvc.formatKey(item.type, item.name);
        $scope.form.available.push({key: key, label: StoreUtilSvc.keyLabel(key)});
      }
      
      $scope.listing = listing;
      console.log( "resolving endpoints promise.");
      deferred.resolve(listing);
    });
    
    return deferred.promise;
  };
  
  $scope.loadChangelog = function(){
    var deferred = $q.defer();
    
    console.log( "Retrieving changelog for: " + $scope.form.type + ":" + $scope.form.name );
    StoreChangelogSvc.resource.list({type: $scope.form.type, name: $scope.form.name, start: $scope.form.start, count: $scope.form.count}).$promise.then(function(listing){
      $scope.form.changes = [];
      
      console.log( "changelog JSON: " + JSON.stringify(listing));
      if ( listing && listing.items && listing.items.length > 0 ){
        console.log( "Formatting " + listing.items.length + " entries.");
        
        for(var i=0; i<listing.items.length; i++){
          var item = listing.items[i];
          item.datestamp = new Date(item.timestamp).toUTCString();
          $scope.form.changes.push(item);
        }
      }
      else{
        console.log( "No entries to format.");
      }
      
      $scope.changes = listing;
      console.log( "resolving changelog promise.");
      deferred.resolve(listing);
    });
    
    return deferred.promise;
  };
  
  $scope.change = function(){
    if ( $scope.form.key !== undefined && $scope.form.key != '' ){
      $scope.form.type = StoreUtilSvc.typeFromKey($scope.form.key);
      $scope.form.packageType = StoreUtilSvc.packageTypeFromKey($scope.form.key);
      $scope.form.name = StoreUtilSvc.nameFromKey($scope.form.key);
      $scope.form.label = StoreUtilSvc.keyLabel( $scope.form.key );
      
      $q.all([ $scope.loadEndpoints(), $scope.loadChangelog() ])
        .then(
          function(data){
            console.log( "everything loaded; should be ready");
          }, function(reason){
            alert( "Failed to load: " + reason);
          });
    }
    else{
      console.log( "Cannot get changelog; $scope.form.key is undefined!" ); 
      $q.all([
              $scope.loadEndpoints()
            ]).then(
                function(data){
                  console.log( "everything loaded; should be ready");
                }, function(reason){
                  alert( "Failed to load: " + reason);
                });
    }
  };
  
  $scope.change();
}]);

indy.controllerProvider.register( 'StoreChangelogEntryCtl', ['$scope', function($scope){
  $scope.display=false;
  
  $scope.toggleSummary = function(){
    $scope.display = !$scope.display;
  }
}]);

