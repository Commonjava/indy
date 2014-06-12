var aprox = angular.module('aprox');

aprox.provide.factory('AutoProxCalculatorSvc', ['$resource',
  function($resource){
    return $resource('/api/1.0/autoprox/eval/:type/:name', {}, {
      eval: {
        method:'GET', 
        params:{type: 'remote', name:'foo'}, 
        isArray:false
      },
    });
  }]);

aprox.controllerProvider.register('AutoProxCalculatorCtl', ['$scope', 'AutoProxCalculatorSvc', 'StoreUtilSvc', function($scope, AutoProxCalculatorSvc, StoreUtilSvc) {
  $scope.types = ['remote', 'hosted', 'group'];
  $scope.form = {
    type: 'remote',
    name: 'RH-eap6.1.0',
  };
  
	$scope.calculate = function(){
//	  alert( "Calculating: " + JSON.stringify( $scope.form, undefined, 2 ));
	  
		AutoProxCalculatorSvc.eval($scope.form, function( result ){
			if ( result.error ){
				$scope.error = result.error;
        delete $scope.raw;
        delete $scope.store;
				delete $scope.supplemental;
			}
			else{
        delete $scope.raw;
			  delete $scope.error;
			  delete $scope.supplemental;
			  
				$scope.store = result.store;
				
				var store = result.store;
				var key = result.store.key;
				
				$scope.raw = {
			    demo: true,
				  type: StoreUtilSvc.typeFromKey( key ),
				  name: StoreUtilSvc.nameFromKey( key ),
				  description: StoreUtilSvc.defaultDescription( result.store.description ),
				  storeHref: StoreUtilSvc.storeHref( key ),
				};
				
				if ( $scope.raw.type == 'remote'){
	        var useX509 = store.server_certificate_pem !== undefined;
	        useX509 = store.key_certificate_pem !== undefined || useX509;

	        $scope.raw.use_x509 = useX509;

	        var useProxy = store.proxy_host !== undefined;
	        $scope.raw.use_proxy = useProxy;

	        var useAuth = (useProxy && store.proxy_user !== undefined);
	        useAuth = store.user !== undefined || useAuth;

	        $scope.raw.use_auth = useAuth;
				}
				else if ( $scope.raw.type == 'group' ){
	        if ( result.supplementalStores ){
	          $scope.supplemental = result.supplementalStores;
	        }
				}
			}
		});
	};
	
//	$scope.calculate();
}]);

aprox.controllerProvider.register('AutoProxCalcConstituentCtl', ['$scope', 'StoreUtilSvc', function( $scope, StoreUtilSvc){
  $scope.display = false;
  
  $scope.displayConstituent = function(){
    $scope.display = true;
    
    var key = $scope.store.key;
    var store = $scope.store;
    
    $scope.raw = {
      demo: true,
      type: StoreUtilSvc.typeFromKey( key ),
      name: StoreUtilSvc.nameFromKey( key ),
      description: StoreUtilSvc.defaultDescription( store.description ),
      storeHref: StoreUtilSvc.storeHref( key ),
    };
    
    if ( $scope.raw.type == 'remote'){
      var useX509 = store.server_certificate_pem !== undefined;
      useX509 = store.key_certificate_pem !== undefined || useX509;

      $scope.raw.use_x509 = useX509;

      var useProxy = store.proxy_host !== undefined;
      $scope.raw.use_proxy = useProxy;

      var useAuth = (useProxy && store.proxy_user !== undefined);
      useAuth = store.user !== undefined || useAuth;

      $scope.raw.use_auth = useAuth;
    }
    else if ( $scope.raw.type == 'group' ){
      if ( result.supplementalStores ){
        $scope.supplemental = result.supplementalStores;
      }
    }
  };
  
  $scope.hideConstituent = function(){
    $scope.display = false;
  };
}]);

aprox.provide.factory('AutoProxCatalogSvc', ['$resource', function($resource){
  return $resource('/api/1.0/autoprox/catalog', {}, {
    query: {method:'GET', params:{}, isArray:false},
  });
}]);

aprox.controllerProvider.register( 'AutoProxRulesCtl', ['$scope', 'AutoProxCatalogSvc', function($scope, AutoProxCatalogSvc){
  AutoProxCatalogSvc.query(function(listing){
    if ( listing.error !== undefined ){
      delete $scope.rules;
      $scope.error = listing.error;
    }
    else{
      delete $scope.error;
      $scope.rules = listing.rules;
    }
  });
  
  $scope.showRule = function(){
    if ( $scope.currentName ){
      $scope.rules.each(function(rule){
        if( rule.name == $scope.currentName ){
          $scope.currentRule = rule;
          return false;
        }
      });
    }
  }
}]);

