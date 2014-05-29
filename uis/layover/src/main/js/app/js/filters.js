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
  .filter('duration', function() {
    return function(secs) {
      if ( secs < 1 ){
        return 'none';
      }

      var hours = Math.floor(secs / (60 * 60));

      var mdiv = secs % (60 * 60);
      var minutes = Math.floor(mdiv / 60);

      var sdiv = mdiv % 60;
      var seconds = Math.ceil(sdiv);

      var out = '';
      if ( hours > 0 ){
        out += hours + 'h';
      }

      if ( minutes > 0 ){
        if ( out.length > 0 ){ out += ' '; }

        out += minutes + 'm';
      }

      if ( seconds > 0 ){
        if ( out.length > 0 ){ out += ' '; }

        out += seconds + 's';
      }

      return out;
    };
  });
