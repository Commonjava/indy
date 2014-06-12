alert( "Defining autoprox controllers, services, etc.");

var aproxServices = angular.module('aprox.services', ['ngResource']);

aproxServices.factory('AutoProxCalculatorSvc', ['$resource',
  function($resource){
    return $resource('/api/1.0/autoprox/eval/:type/:name', {}, {
      evalRemote: {method:'GET', params:{type: 'remote', name:''}, isArray:false},
      evalHosted: {method:'GET', params:{type: 'hosted', name:''}, isArray:false},
      evalGroup: {method:'GET', params:{type: 'group', name:''}, isArray:false},
    });
  }]);

var aproxControllers = angular.module('aprox.controllers', []);

aproxControllers.controller('AutoProxCalculatorCtl', ['$scope', 'AutoProxCalculatorSvc', function($scope, AutoProxCalculatorSvc) {
	$scope.store = {};
	$scope.calculateRemote = function(name){
		$scope.store = AutoProxCalculatorSvc.evalRemote({name: name});
	};
	
	$scope.calculateHosted = function(name){
		$scope.store = AutoProxCalculatorSvc.evalHosted({name: name});
	};
	
	$scope.calculateGroup = function(name){
		$scope.store = AutoProxCalculatorSvc.evalGroup({name: name});
	};
  }]);

registerDynamic( 'aprox.services', 'AutoProxCalculatorSvc');
registerDynamic( 'aprox.controllers', 'AutoProxCalculatorCtl');