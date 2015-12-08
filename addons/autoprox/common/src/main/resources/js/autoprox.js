/*
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
indyAddons.factory('AutoProxUtilsSvc', function(){
  return {
    viewRulePath: function(rule){
      return '/autoprox/rules/view/' + rule;
    },
    
    viewRuleHref: function(rule){
      return '#/autoprox/rules/view/' + rule;
    },
    
    viewCalcPath: function( form ){
      return '/autoprox/calc/view/' + form.type + '/' + form.name;
    },
    
    viewCalcHref: function( form ){
      return '#/autoprox/calc/view/' + form.type + '/' + form.name;
    },
  };
});

indyAddons.factory('AutoProxCalculatorSvc', ['$resource', function($resource){
  return $resource(appPath('/api/autoprox/eval/:type/:name'), {}, {
    eval: {
      method:'GET', 
      params:{type: 'remote', name:'foo'}, 
      isArray:false
    },
  });
}]);

indyServices.factory('AutoProxCatalogSvc', ['$resource', function($resource){
  return $resource(appPath('/api/autoprox/catalog'), {}, {
    query: {method:'GET', params:{}, isArray:false},
  });
}]);


indyAddons.controller('AutoProxCalculatorCtl', ['$scope', '$routeParams', '$location', 'AutoProxCalculatorSvc', 'AutoProxUtilsSvc', 'StoreUtilSvc',
                                                            function($scope, $routeParams, $location, AutoProxCalculatorSvc, AutoProxUtilsSvc, StoreUtilSvc) {
  $scope.types = ['remote', 'hosted', 'group'];
  $scope.form = {
    type: 'remote',
    name: 'RH-eap6.1.0',
  };
  
  $scope.change = function(){
    $location.path(AutoProxUtilsSvc.viewCalcPath($scope.form));
  };
  
  $scope.create = function(){
    var href = StoreUtilSvc.detailPath($scope.store.key);
    $location.path( href );
  };
  
	$scope.calculate = function(){
//	  alert( "Calculating: " + JSON.stringify( $scope.form, undefined, 2 ));
	  
		AutoProxCalculatorSvc.eval($scope.form, function( result ){
			if ( result.error ){
				$scope.error = result.error;
        delete $scope.raw;
        delete $scope.store;
				delete $scope.supplemental;
        delete $scope.rule_name;
        delete $scope.rule_href;
			}
			else{
        delete $scope.raw;
			  delete $scope.error;
			  delete $scope.supplemental;
			  
				$scope.store = result.store;
				$scope.rule_name = result.ruleName;
				$scope.rule_href = AutoProxUtilsSvc.viewRuleHref(result.ruleName);
				
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
	          
	          var suppKeys = [];
	          $scope.supplemental.each(function(store){
	            store.type = StoreUtilSvc.typeFromKey( store.key );
	            suppKeys.push( store.key );
	          });
	          
	          $scope.raw.constituentHrefs= {};

	          store.constituents.each(function(constituent){
	            if ( suppKeys.indexOf( constituent ) < 0 ){
	              $scope.raw.constituentHrefs[constituent] = {
	                detailHref: StoreUtilSvc.detailHref( constituent )
	              };
	            }
	          });
	        }
				}
			}
		});
	};
	
  var routeType = $routeParams.type;
  var routeName = $routeParams.name;
  
//  alert( "Got route type: " + routeType + "\n and name: " + routeName );
  
  if ( routeType !== undefined && routeName !== undefined ){
    $scope.form.type = routeType;
    $scope.form.name = routeName;
    
  	$scope.calculate();
  }
  
  $scope.storeUtils = StoreUtilSvc;
  
}]);

indyAddons.controller('AutoProxCalcConstituentCtl', ['$scope', 'StoreUtilSvc', function( $scope, StoreUtilSvc){
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
  
  $scope.storeUtils = StoreUtilSvc;
}]);

indyAddons.controller( 'AutoProxRulesCtl', ['$scope', '$routeParams', '$location', 'AutoProxCatalogSvc', 'AutoProxUtilsSvc', 'StoreUtilSvc',
                                                        function($scope, $routeParams, $location, AutoProxCatalogSvc, AutoProxUtilsSvc, StoreUtilSvc){
  
  $scope.currentName = $routeParams.name;
  
  AutoProxCatalogSvc.query(function(listing){
    if ( listing.error !== undefined ){
      delete $scope.rules;
      $scope.error = listing.error;
    }
    else{
      delete $scope.error;
      $scope.rules = listing.rules;
      
      if ( $scope.currentName ){
        $scope.rules.each(function(rule){
          if( rule.name == $scope.currentName ){
            $scope.currentRule = rule;
            return false;
          }
        });
      }
    }
  });
  
  $scope.showRule = function(){
    $location.path(AutoProxUtilsSvc.viewRulePath($scope.currentName));
  };
  
  $scope.storeUtils = StoreUtilSvc;
}]);

