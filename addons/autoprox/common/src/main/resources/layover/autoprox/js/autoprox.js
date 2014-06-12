//alert( "Defining autoprox controllers, services, etc.");

var aproxServices = angular.module('aprox.services', ['ngResource']);

aproxServices.factory('AutoProxCalculatorSvc', ['$resource',
  function($resource){
    return $resource('/api/1.0/autoprox/eval/:type/:name', {}, {
      eval: {
        method:'GET', 
        params:{type: 'remote', name:'foo'}, 
        isArray:false
      },
    });
  }]);

var aproxControllers = angular.module('aprox.controllers', []);

aproxControllers.controller('AutoProxCalculatorCtl', ['$scope', 'AutoProxCalculatorSvc', 'StoreUtilSvc', function($scope, AutoProxCalculatorSvc, StoreUtilSvc) {
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

aproxControllers.controller('AutoProxCalcConstituentCtl', ['$scope', 'StoreUtilSvc', function( $scope, StoreUtilSvc){
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

registerDynamic( 'aprox.services', 'AutoProxCalculatorSvc');
registerDynamic( 'aprox.controllers', 'AutoProxCalculatorCtl');
registerDynamic( 'aprox.controllers', 'AutoProxCalcConstituentCtl');
