'use strict';

/* Directives */


var directives = angular.module('aprox.directives', []);

directives.directive('apDurationHint', function() {
  return {
    restrict: 'E',
    link: function(scope, element, attributes){
      var suggestion = element.text();
      if ( suggestion == '' ){
        suggestion = "24h 36m 00s";
      }

      element.html('<span class="hint">(eg. ' + suggestion + ')</span>');
    }
  };
});

directives.directive('apAvailableGroup', function() {
  return {
    restrict: 'A',
//     scope:{
//       available: '=value',
//     },
    link: function(scope, element, attributes){
      var key = scope.available.type + ':' + scope.available.name;
      var idx = scope.store.constituents.indexOf(key);
      if ( idx < 0 ){
        element.hide();
      }
    },
  };
});
