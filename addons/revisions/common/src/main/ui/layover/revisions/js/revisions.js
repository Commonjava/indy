var aprox = angular.module('aprox');

aprox.provide.factory('StoreChangelogSvc', ['$resource', function($resource){
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


aprox.controllerProvider.register('StoreChangelogCtl', ['$scope', '$routeParams', '$location', '$q', 'StoreChangelogSvc', 'AllEndpointsSvc', 'StoreUtilSvc', 
                                                            function($scope, $routeParams, $location, $q, StoreChangelogSvc, AllEndpointsSvc, StoreUtilSvc) {
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
  };
  
  var routeType = $routeParams.type;
  var routeName = $routeParams.name;
  
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
    StoreChangelogSvc.resource.list({type: $scope.form.type, name: $scope.form.name}).$promise.then(function(listing){
      $scope.form.changes = [];
      
      console.log( "changelog JSON: " + JSON.stringify(listing));
      if ( listing && listing.items && listing.items.length > 0 ){
        console.log( "Formatting " + listing.items.length + " entries.");
        
        for(var i=0; i<listing.items.length; i++){
          var item = listing.items[i];
          item.datestamp = new Date(item.timestamp * 1000).toUTCString();
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
      var type = StoreUtilSvc.typeFromKey($scope.form.key);
      var name = StoreUtilSvc.nameFromKey($scope.form.key);
      
      var storePath = StoreChangelogSvc.viewStorePath(type, name);
      console.log( "Navigating to: " + storePath);
      $location.path(storePath);
      
      routeType = type;
      routeName = name;
    }
    else{
      console.log( "Cannot get changelog; $scope.form.key is undefined!" ); 
    }
  };
  
  if ( routeType !== undefined && routeName !== undefined ){
    $scope.form.type = routeType;
    $scope.form.name = routeName;
    $scope.form.key = StoreUtilSvc.formatKey( $scope.form.type, $scope.form.name );
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
    console.log( routeName + " (name) is undefined, or " + routeType + " (type) is undefined.");
    $q.all([
            $scope.loadEndpoints()
          ]).then(
    function(data){
      console.log( "everything loaded; should be ready");
    }, function(reason){
      alert( "Failed to load: " + reason);
    });
  }
}]);

aprox.controllerProvider.register( 'StoreChangelogEntryCtl', ['$scope', function($scope){
  $scope.display=false;
  
  $scope.toggleSummary = function(){
    $scope.display = !$scope.display;
  }
}]);

