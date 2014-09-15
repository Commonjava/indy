'use strict';

/* Directives */


var directives = angular.module('aprox.directives', []);

directives.directive('apPasswordMask', function(){
  return {
    restrict: 'E',
    template: '<span class="password-mask">********</span>',
  };
});

directives.directive('apListingDescription', function(){
  return {
    restrict: 'E',
    link: function(scope, element, attributes){
      var description = scope.store.description;

      if ( !description || description.length < 1 ){
        description = 'No description provided.';
      }

      element.html('<span class="description field">' + description + '</span>');
    },
  };
});

directives.directive('apHint', function(){
  return {
    restrict: 'E',
     scope: {
       key: '@',
     },
    link: function(scope, element, attributes){
      var hint = 'unknown hint: ' + scope.key;
      switch(scope.key){
        case 'passthrough':
          hint = 'subject to a configured minimum cache timeout for performance reasons';
          break;
        case 'request_timeout':
          hint = 'subject to a configured minimum request timeout for performance reasons';
          break;
        case 'client_key':
          hint = 'required if Client Key is supplied';
          break;
      }

      element.html( '<span class="hint">(' + hint + ')</span>' );
    },
  };
});

directives.directive('apDurationHint', ['$timeout',function(timer) {
  return {
    restrict: 'E',
    link: function(scope, element, attributes){
      var run=function(){
        var suggestion = element.text();
        if ( suggestion == '' ){
          suggestion = "24h 36m 00s";
        }
        
        element.html('<span class="hint">(eg. ' + suggestion + ')</span>');
      };
      
      timer(run, 0);
    }
  };
}]);

directives.directive('apGroupConstituent', ['$timeout', function(timer) {
  return {
    restrict: 'A',
    templateUrl: 'partials/directives/ap-group-constituent.html',
    link: function(scope, element, attributes){
      var run = function(){
        scope.rmConstituent = function(){
          var idx = scope.store.constituents.indexOf(scope.constituent);
          
          var parts = scope.constituent.split(':')
          scope.raw.available.push({type: parts[0], name: parts[1]});
          
          scope.store.constituents.splice(idx,1);
          
          scope.storeUtils.sortEndpoints(scope.raw.available);
        };
        
        scope.promote = function(){
          var idx = scope.store.constituents.indexOf(scope.constituent);
          if ( idx > 0){
            scope.store.constituents.move(idx, idx-1);
          }
        };
        
        scope.top = function(){
          var idx = scope.store.constituents.indexOf(scope.constituent);
          scope.store.constituents.move(idx, 0);
        };
        
        scope.demote = function(){
          var idx = scope.store.constituents.indexOf(scope.constituent);
          if ( idx < scope.store.constituents.length-1){
            scope.store.constituents.move(idx, idx+1);
          }
        };
        
        scope.bottom = function(){
          var idx = scope.store.constituents.indexOf(scope.constituent);
          scope.store.constituents.move(idx, scope.store.constituents.length-1);
        };
      };
      
      timer(run, 0);
    },
  };
}]);

directives.directive('apGroupAvailable', ['$timeout', function( timer ) {
  return {
    restrict: 'A',
    templateUrl: 'partials/directives/ap-group-available.html',
    link: function(scope, element, attributes){
      var run = function(){
        scope.addConstituent = function(){
          scope.store.constituents.push(scope.available.type + ':' + scope.available.name);
          element.addClass('hidden');
        };

        var key = scope.available.type + ':' + scope.available.name;
        var idx = scope.store.constituents.indexOf(key);
        var gKey = 'group:' + scope.raw.name;
        
        if ( idx > -1 ){
          element.addClass('hidden');
        }
        else if ( key == gKey ){
          element.addClass('hidden');
        }
      };
      
      timer(run, 100);
    },
  };
}]);
