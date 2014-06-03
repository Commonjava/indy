'use strict';

/* Filters */

var filterModule = angular.module('aprox.filters', []);

filterModule
  .filter('checkmark', function() {
    return function(input) {
      return input ? '\u2713' : '\u2718';
    };
  });

filterModule
  .filter('duration', ['StoreUtilSvc', function(StoreUtilSvc) {
    return function(secs) {
      return StoreUtilSvc.secondsToDuration( secs );
    };
  }]);
