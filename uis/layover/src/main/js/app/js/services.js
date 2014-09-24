'use strict';

/* Services */


var aproxServices = angular.module('aprox.services', ['ngResource']);

aproxServices.factory('ControlSvc', ['ngDialog', function(ngDialog){
  return {
    promptForChangelog: function(scope, confirmLabel, callback){
      scope.raw.confirmLabel=confirmLabel;
      scope.raw.changelog='';
      
      console.log('Confirm label: ' + scope.raw.confirmLabel);
      
      ngDialog.openConfirm({template: 'partials/dialogs/changelog-dialog.html', scope: scope }).then(function(data){
        if ( !scope.raw.changelog || scope.raw.changelog.length < 1 ){
          alert( "You must provide a changelog!" );
          return;
        }
        
        callback(scope.raw.changelog);
      }, 
      function(data){
        console.log(scope.confirmLabel + ": cancelled");
      });
    },
    
    addControlHrefs: function(scope, storeType, storeName, mode){
      scope.raw.backHref='#/' + storeType;
      if (mode == 'view'){
        scope.raw.newHref='#/' + storeType + '/new';
        scope.raw.editHref='#/' + storeType + '/edit/' + storeName;
      }
    },
    
    addStoreControls: function(scope, location, storeType, storeService, StoreUtilSvc, fixups){
      var self=this;
      
      scope.save = function(){
        scope.store.metadata={};
        self.promptForChangelog(scope, 'Save', function(changelog){
          scope.store.metadata['changelog'] = changelog;
          
          if ( fixups && fixups.save){
            fixups.save(scope);
          }

          if ( scope.mode == 'edit' ){
            storeService.resource.update({name: scope.raw.name}, scope.store, function(){
              location.path( StoreUtilSvc.detailPath(scope.store.key) );
            });
          }
          else if ( scope.mode == 'new' ){
            scope.store.key = StoreUtilSvc.formatKey(storeType, scope.raw.name);
            storeService.resource.create({}, scope.store, function(){
              location.path( StoreUtilSvc.detailPath(scope.store.key) );
            });
          }
        });
      };

      scope.remove = function(){
        console.log("confirm delete");
        self.promptForChangelog(scope, 'Delete', function(changelog){
          console.log("deleting with changelog: " + changelog);
          
          storeService.remove(scope.raw.name, changelog, {
            success: function(data,status){
              location.path( '/' + storeType );
            },
          });
        });
      };

      scope.cancel = function(){
        if ( scope.mode == 'edit' ){
          location.path( StoreUtilSvc.detailPath(scope.store.key) );
        }
        else{
          location.path( '/' + storeType );
        }
      };
    },
  };
}]);

aproxServices.factory('StoreUtilSvc', function(){
  return {
    resourceMode: function(){
      if ( window.location.hash.match( ".+/edit/.+" ) ){
        return 'edit';
      }
      else if ( window.location.hash.endsWith( "/new" ) ){
        return 'new';
      }
      else{
        return 'view';
      }
    },
    
    formatKey: function(type, name){
      return type + ':' + name;
    },
    
    keyLabel: function(key){
      var parts = key.split(':');
      return parts[1] + " (" + parts[0] + ")";
    },

    nameFromKey: function(key){
//      alert("Parsing name from:\n" + JSON.stringify(key, undefined, 2));
      return key.substring(key.indexOf(':')+1);
    },

    typeFromKey: function(key){
        return key.substring(0, key.indexOf(':'));
      },

    storeHref: function(key){
      var parts = key.split(':');

      var hostAndPort = window.location.hostname;
      if ( window.location.port != '' && window.location.port != 80 && window.location.port != 443 ){
        hostAndPort += ':';
        hostAndPort += window.location.port;
      }

      var basepath = window.location.pathname;
      basepath = basepath.replace('/app', '');
      basepath = basepath.replace(/index.html.*/, '');


      var proto = window.location.protocol;

      // TODO: In-UI browser that allows simple searching
      return proto + "//" + hostAndPort + basepath + 'api/' + parts[0] + '/' + parts[1] + '/';
    },

    detailPath: function(key){
      var parts = key.split(':');
      return "/" + parts[0] + "/view/" + parts[1];
    },

    detailHref: function(key){
      var parts = key.split(':');
      return "#/" + parts[0] + "/view/" + parts[1];
    },

    editHref: function(key){
      var parts = key.split(':');
      return "#/" + parts[0] + "/edit/" + parts[1];
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

    durationToSeconds: function(duration){
      if ( duration == 'never' ){
        return 0;
      }

      var re=/((\d+)h\s*)?((\d+)m\s*)?((\d+)s?)?/;
      var arry = re.exec( duration );

      var secs = 0;

      if ( arry[2] !== undefined ){
        secs += (parseInt(arry[2]) * 3600 );
      }

      if ( arry[4] !== undefined ){
        secs += (parseInt(arry[4]) * 60 );
      }

      if ( arry[6] !== undefined ){
        secs += (parseInt(arry[6]));
      }

      return secs;
    },

    secondsToDuration: function(secs){
      if ( secs == undefined ){
        return 'never';
      }

      if ( secs < 1 ){
        return 'never';
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
    },

    sortEndpoints: function(endpoints){
      var typeOrder = ['group', 'remote', 'hosted'];
      return endpoints.sort(function(a, b){
        var ta = typeOrder.indexOf(a.type);
        var tb = typeOrder.indexOf(b.type);

        if ( ta != tb ){
          return ta < tb ? -1 : 1;
        }

        if ( a.name < b.name ){
          return -1;
        }
        else if ( b.name < a.name ){
          return 1;
        }

        return 0;
      });
    },
    
    sortByEmbeddedKey: function(items){
      var typeOrder = ['group', 'remote', 'hosted'];
      return items.sort(function(a, b){
        var ap = a.key.split(':');
        var bp = b.key.split(':');
        
        var ati = typeOrder.indexOf(ap[0]);
        var bti = typeOrder.indexOf(bp[0]);

        if ( ati != bti ){
          return ati < bti ? -1 : 1;
        }

        if ( ap[1] < bp[1] ){
          return -1;
        }
        else if ( bp[1] < ap[1] ){
          return 1;
        }

        return 0;
      });
    },

    defaultDescription: function(description){
      var desc = description;
      if ( !desc || desc.length < 1 ){
        desc = 'No description provided.';
      }

      return desc;
    },
  };
});


aproxServices.factory('RemoteSvc', ['$resource', '$http',
  function($resource, $http){
    return {
      resource: $resource(appPath( '/api/admin/remote/:name' ), {}, {
        query: {method:'GET', params:{name:''}, isArray:false},
        update: {method: 'PUT'},
        create: {method: 'POST'},
      }),
      remove: function( name, changelog, functions ){
        $http.delete('/api/admin/remote/' + name, {
          headers:{
          'CHANGELOG': changelog,
          }
        }).success( functions.success );
      },
    };
  }]);

aproxServices.factory('HostedSvc', ['$resource', '$http',
  function($resource, $http){
    return {
      resource: $resource(appPath( '/api/admin/hosted/:name' ), {}, {
        query: {method:'GET', params:{name:''}, isArray:false},
        update: {method: 'PUT'},
        create: {method: 'POST'},
      }),
      remove: function( name, changelog, functions ){
        $http.delete('/api/admin/hosted/' + name, {
          headers:{
          'CHANGELOG': changelog,
          }
        }).success( functions.success );
      },
    }
  }]);

aproxServices.factory('GroupSvc', ['$resource', '$http',
  function($resource, $http){
    return {
      resource: $resource(appPath( '/api/admin/group/:name' ), {}, {
        query: {method:'GET', params:{name:''}, isArray:false},
        update: {method: 'PUT'},
        create: {method: 'POST'},
      }),
      remove: function( name, changelog, functions ){
        $http.delete('/api/admin/group/' + name, {
          headers:{
          'CHANGELOG': changelog,
          }
        }).success( functions.success );
      },
    };
  }]);

aproxServices.factory('NfcSvc', ['$resource',
  function($resource) {
    return {
      resource: $resource(appPath('/api/nfc/:type/:name/:path'), {}, {
        query : { method : 'GET', params : { type: '', name: '', path: '' }, isArray : false },
        get : { method : 'GET', params : { path: '' }, isArray : false },
        deleteAll: {method: 'DELETE', params: {type:'', name:'', path:''}},
        delete: {method: 'DELETE'},
      }),
    };
}]);

aproxServices.factory('AllEndpointsSvc', ['$resource',
  function($resource){
    return {
      resource: $resource(appPath( '/api/stats/all-endpoints' ), {}, {
        query: {method:'GET', params:{}, isArray:false},
      }),
    }
  }]);

aproxServices.factory('FooterSvc', ['$resource',
  function($resource){
    return {
      resource: $resource(appPath( '/api/stats/version-info' ), {}, {
        query: {method:'GET', params:{}, isArray:false},
      }),
    }
  }]);


