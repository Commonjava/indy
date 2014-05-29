'use strict';

/* Services */


var aproxServices = angular.module('aprox.services', ['ngResource']);

aproxServices.factory('RemoteSvc', ['$resource',
  function($resource){
    return $resource('/api/1.0/admin/remote/:name', {}, {
      query: {method:'GET', params:{name:''}, isArray:false}
    });
  }]);

aproxServices.factory('HostedSvc', ['$resource',
  function($resource){
    return $resource('/api/1.0/admin/hosted/:name', {}, {
      query: {method:'GET', params:{name:''}, isArray:false}
    });
  }]);

aproxServices.factory('GroupSvc', ['$resource',
  function($resource){
    return $resource('/api/1.0/admin/group/:name', {}, {
      query: {method:'GET', params:{name:''}, isArray:false}
    });
  }]);

aproxServices.factory('FooterSvc', ['$resource',
  function($resource){
    return $resource('/api/1.0/stats/version-info', {}, {
      query: {method:'GET', params:{}, isArray:false}
    });
  }]);


aproxServices.factory('StoreUtilSvc', function(){
    return {
      nameFromKey: function(key){
        return key.substring(key.indexOf(':')+1);
      },

      storeHref: function(key){
        var parts = key.split(':');

        var hostAndPort = window.location.hostname;
        if ( window.location.port != 80 && window.location.port != 443 ){
          hostAndPort += ':' + window.location.port;
        }

        return window.location.protocol + "//" + hostAndPort + window.location.pathname + 'api/1.0/' + parts[0] + '/' + parts[1] + '/';
      },

      detailHref: function(key){
        var parts = key.split(':');
        return "#/" + parts[0] + "/" + parts[1];
      },

      editHref: function(key){
        var parts = key.split(':');
        return "#/" + parts[0] + "/" + parts[1] + "/edit";
      },

      hostedOptions: function(store){
        var options = [];

        if ( store.allow_snapshots ){
          options.push({icon: 'S', title: 'Snapshots allowed'});
        }

        if ( store.allow_releases ){
          options.push({icon: 'R', title: 'Releases allowed'});
        }

        if ( store.allow_snapshots || store.allow_releases ){
          options.push({icon: 'D', title: 'Deployment allowed'});
        }

        return options;
      },
    };
  });
