'use strict';

/* Directives */


var directives = angular.module('aprox.directives', []);

directives.directive('apDurationHint', function() {
  return {
    restrict: 'E',
    /*template: '<span class="hint">(eg. 24h 36m 00s)</span>',*/
    link: function(scope, element, attributes){
      var suggestion = element.text();
      if ( suggestion == '' ){
        suggestion = "24h 36m 00s";
      }

      element.html('<span class="hint">(eg. ' + suggestion + ')</span>');
    }
  };
});
