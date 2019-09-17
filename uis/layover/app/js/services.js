/*
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
'use strict';

/* Services */


var indyServices = angular.module('indy.services', ['ngResource']);

indyServices.factory('PackageTypeSvc', ['$resource', '$http',
  function($resource, $http){
    return {
          resource: $resource(appPath( '/api/stats/package-type/keys' ), {}, {
            query: {method:'GET', params:{}, isArray:true},
          }),
    };
  }]);

indyServices.factory('ControlSvc', ['ngDialog', function(ngDialog){
  return {

    promptForConfirm : function(scope, confirmLabel, confirmText, callback) {
      scope.raw.confirmLabel=confirmLabel;
      scope.raw.confirmText=confirmText;
      ngDialog.openConfirm({template: 'partials/dialogs/confirm-dialog.html', scope: scope }).then(function(data){
        console.log('callback called');
        callback();
      },
      function(data){
        console.log("cancelled");
      });
    },

    promptForChangelog: function(scope, confirmLabel, callback){
      scope.raw.confirmLabel=confirmLabel;
      scope.raw.changelog='';

//      console.log('Confirm label: ' + scope.raw.confirmLabel);

      ngDialog.openConfirm({template: 'partials/dialogs/changelog-dialog.html', scope: scope }).then(function(data){
        if ( !scope.raw.changelog || scope.raw.changelog.length < 1 ){
          alert( "You must provide a changelog!" );
          return;
        }

        callback(scope.raw.changelog);
      },
      function(data){
        console.log("cancelled");
      });
    },

    addListingControlHrefs: function(scope, location){
      scope.createNew = function(){
        location.path(location.path() + '/new');
      }
    },

    addControlHrefs: function(scope, packageType, storeType, storeName, mode, location){
      scope.back = function(){
        location.path('/' + packageType + '/' + storeType);
      }

      scope.edit = function(){
        location.path('/' + packageType + '/' + storeType + '/edit/' + storeName);
      }

      scope.createNew = function(){
        location.path('/' + packageType + '/' + storeType + '/new');
      }
    },

    addStoreControls: function(scope, location, storeType, storeService, StoreUtilSvc, fixups){
      var self=this;

      scope.enable = function(){
        console.log("Enable: " + scope.raw.name + " (enabled already? " + scope.raw.enabled + ")");
        if ( !scope.raw.enabled ){
          if ( !scope.store.metadata ){
            scope.store.metadata = {};
          }

          scope.store.metadata['changelog'] = "Enabling via UI";
          scope.store.type = storeType;

          scope.store.disabled = false;
          scope.raw.enabled = true;
          console.log("Set enabled == " + scope.raw.enabled + " in raw metadata");

          if ( fixups && fixups.save){
            fixups.save(scope);
          }

          if ( !scope.raw.new ){
            storeService.resource.update({packageType: scope.raw.packageType, name: scope.raw.name}, scope.store, function(){
              location.path( StoreUtilSvc.detailPath(scope.store.key) );
            });
          }
        }
      };

      scope.disable = function(){
        console.log("Disable: " + scope.raw.name + " (enabled already? " + scope.raw.enabled + ")");
        if ( scope.raw.enabled ){
          if ( !scope.store.metadata ){
            scope.store.metadata = {};
          }

          scope.store.metadata['changelog'] = "Disabling indefinitely via UI";
          scope.store.type = storeType;

          scope.store.disabled = true;
          scope.raw.enabled = false;
          console.log("Set enabled == " + scope.raw.enabled + " in raw metadata");

          if ( fixups && fixups.save){
            fixups.save(scope);
          }

          if ( !scope.raw.new ){
            storeService.resource.update({packageType: scope.raw.packageType, name: scope.raw.name}, scope.store, function(){
              location.path( StoreUtilSvc.detailPath(scope.store.key) );
            });
          }
        }
      };

      scope.save = function(){
        scope.store.metadata={};
        self.promptForChangelog(scope, 'Save', function(changelog){
          scope.store.metadata['changelog'] = changelog;
          scope.store.type = storeType;

          if ( fixups && fixups.save){
            fixups.save(scope);
          }

          if ( scope.mode == 'edit' ){
            alert("Saving " + scope.raw.packageType + "/" + scope.raw.name );
            storeService.resource.update({packageType: scope.raw.packageType, name: scope.raw.name}, scope.store, function(){
              location.path( StoreUtilSvc.detailPath(scope.store.key) );
            });
          }
          else if ( scope.mode == 'new' ){
            scope.store.key = StoreUtilSvc.formatKey(scope.raw.packageType, storeType, scope.raw.name);
            scope.store.packageType = scope.raw.packageType;
            scope.store.name = scope.raw.name;

            storeService.resource.create({packageType: scope.store.packageType}, scope.store, function(){
              location.path( StoreUtilSvc.detailPath(scope.store.key) );
            });
          }
        });
      };

      scope.remove = function(){
        console.log("confirm delete");
        self.promptForChangelog(scope, 'Delete', function(changelog){
          console.log("deleting with changelog: " + changelog);

          storeService.remove(scope.raw.packageType, scope.raw.name, changelog, {
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

indyServices.factory('StoreUtilSvc', function(){
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

    formatKey: function(packageType, type, name){
      return packageType + ":" + type + ':' + name;
    },

    keyLabel: function(key){
      var parts = key.split(':');
      return parts[2] + " (" + parts[1] + "; " + parts[0] + ")";
    },

    nameFromKey: function(key){
//      alert("Parsing name from:\n" + JSON.stringify(key, undefined, 2));
      var parts = key.split(':');
      return parts[parts.length-1];
    },

    typeFromKey: function(key){
        var parts = key.split(':');
        return parts[1];
      },

    packageTypeFromKey: function(key){
        var parts = key.split(':');
        return parts[0];
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
      return proto + "//" + hostAndPort + basepath + 'api/content/' + parts[0] + '/' + parts[1] + '/' + parts[2];
    },

    detailPath: function(key){
      var parts = key.split(':');
      return "/" + parts[1] + '/' + parts[0] + "/view/" + parts[2];
    },

    detailHref: function(key){
      var parts = key.split(':');
      return "#/" + parts[1] + '/' + parts[0] + "/view/" + parts[2];
    },

    editHref: function(key){
      var parts = key.split(':');
      return "#/" + parts[1] + '/' + parts[0] + "/edit/" + parts[2];
    },

    hostedOptionLegend: function(){
      return [
        {icon: 'S', title: 'Snapshots allowed'},
        {icon: 'R', title: 'Releases allowed'},
        {icon: 'D', title: 'Deployment allowed'}
      ];
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

    remoteOptionLegend: function(){
      return [
        {icon: 'S', title: 'Snapshots allowed'},
        {icon: 'R', title: 'Releases allowed'}
      ];
    },

    remoteOptions: function(store){
      var options = [];

      if ( store.allow_snapshots ){
        options.push({icon: 'S', title: 'Snapshots allowed'});
      }

      if ( store.allow_releases ){
        options.push({icon: 'R', title: 'Releases allowed'});
      }

      return options;
    },

    durationToSeconds: function(duration, useDefault){
      if ( useDefault ) {
        if ( duration === '0' || duration === 0 || duration === 'default' ) {
          return 0;
        }
        if ( duration < 0 || duration === 'never' ) {
          return -1;
        }
      } else if ( duration === 'never' ){
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

    timestampToDateFormat: function(milliseconds){
      if ( milliseconds == undefined ){
        return 'never';
      }

      if ( milliseconds < 1 ){
        return 'never';
      }

      var date = new Date();
      date.setTime(milliseconds)
      return date.toLocaleString();
    },

    secondsToDuration: function(secs, useDefault){
      if ( (secs === undefined || secs === 0) && useDefault ) {
        return "default";
      }

      if ( secs === undefined ){
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

    // Sort by: type (group, remote, hosted), packageType, name
    sortEndpoints: function(endpoints){
      var typeOrder = ['group', 'remote', 'hosted'];
      return endpoints.sort(function(a, b){
        var ta = typeOrder.indexOf(a.type);
        var tb = typeOrder.indexOf(b.type);

        if ( ta != tb ){
          return ta < tb ? -1 : 1;
        }

        if ( a.packageType < b.packageType ){
          return -1;
        }
        else if ( b.packageType < a.packageType ){
          return 1;
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

    // Sort by: type (group, remote, hosted), packageType, name
    sortByEmbeddedKey: function(items){
      if ( items == undefined ){
        return items;
      }

      var typeOrder = ['group', 'remote', 'hosted'];
      return items.sort(function(a, b){
        var ap = a.key.split(':');
        var bp = b.key.split(':');

        var ati = typeOrder.indexOf(ap[1]);
        var bti = typeOrder.indexOf(bp[1]);

        if ( ati != bti ){
          return ati < bti ? -1 : 1;
        }

        if ( ap[0] < bp[0] ){
          return -1;
        }
        else if ( bp[0] < ap[0] ){
          return 1;
        }

        if ( ap[2] < bp[2] ){
          return -1;
        }
        else if ( bp[2] < ap[2] ){
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


indyServices.factory('RemoteSvc', ['$resource', '$http',
  function($resource, $http){
    return {
      resource: $resource(appPath( '/api/admin/stores/:packageType/remote/:name' ), {}, {
        query: {method:'GET', params:{packageType: '_all', name:''}, isArray:false},
        update: {method: 'PUT', params:{packageType: 'unknown'}},
        create: {method: 'POST', params:{packageType:'unknown'}},
      }),
      remove: function( packageType, name, changelog, functions ){
        $http.delete('/api/admin/stores/' + packageType + '/remote/' + name, {
          headers:{
          'CHANGELOG': changelog,
          }
        }).success( functions.success );
      },
    };
  }]);

indyServices.factory('HostedSvc', ['$resource', '$http',
  function($resource, $http){
    return {
      resource: $resource(appPath( '/api/admin/stores/:packageType/hosted/:name' ), {}, {
        query: {method:'GET', params:{packageType: '_all', name:''}, isArray:false},
        update: {method: 'PUT', params:{packageType: 'unknown'}},
        create: {method: 'POST', params:{packageType:'unknown'}},
      }),
      remove: function( packageType, name, changelog, functions ){
        $http.delete('/api/admin/stores/' + packageType + '/hosted/' + name, {
          headers:{
          'CHANGELOG': changelog,
          }
        }).success( functions.success );
      },
    }
  }]);

indyServices.factory('GroupSvc', ['$resource', '$http',
  function($resource, $http){
    return {
      resource: $resource(appPath( '/api/admin/stores/:packageType/group/:name' ), {}, {
        query: {method:'GET', params:{packageType: '_all', name:''}, isArray:false},
        update: {method: 'PUT', params:{packageType: 'unknown'}},
        create: {method: 'POST', params:{packageType:'unknown'}},
      }),
      remove: function( packageType, name, changelog, functions ){
        $http.delete('/api/admin/stores/' + packageType + '/group/' + name, {
          headers:{
          'CHANGELOG': changelog,
          }
        }).success( functions.success );
      },
    };
  }]);

indyServices.factory('NfcSvc', ['$resource',
  function($resource) {
    return {
      resource: $resource(appPath('/api/nfc/:packageType/:type/:name/:path'), {}, {
        query : { method : 'GET', params : { packageType: '', type: '', name: '', path: '' }, isArray : false },
        get : { method : 'GET', params : { path: '' }, isArray : false },
        deleteAll: {method: 'DELETE', params: {packageType: '', type:'', name:'', path:''}},
        delete: {method: 'DELETE'},
      }),
    };
}]);

indyServices.factory('CacheSvc', ['$http',
  function($http){
    return {
      remove: function( scope, path, ControlSvc ) {
        ControlSvc.promptForConfirm(scope, 'Delete', 'delete ' + path, function(){
          $http.delete(path + '?cache-only=true', {}).then(function(response){
            if (response.status == 204) {
              scope.data = 'Deleted ' + path;
            } else {
              scope.data = 'Delete failed';
            }
          }, function(response) { // second function handles error
              scope.data = "Something went wrong - " + response.status + ' ' + response.statusText;
          });
        });
      }
    };
  }
]);

indyServices.factory('StoreDisableSvc', ['$resource',
  function($resource) {
    var self = this;
    var obj = {};
    obj.storeTimeouts = $resource(appPath('/api/admin/schedule/store/:packageType/:type/:name/disable-timeout'), {}, {
        query: { method : 'GET', params: {}, isArray : false }
    });

    obj.allTimeouts = $resource(appPath('/api/admin/schedule/store/all/disable-timeout'), {}, {
        query: {method: 'GET', params: {}, isArray: false}
    });

    obj.setEnableAttributes = function(raw, store, StoreUtilSvc){
//          console.log("Store:\n\n" + JSON.stringify(store));
          var disabled = store.disabled === undefined ? false : store.disabled;
//          console.log( "is " + store.key + " disabled? " + disabled);
          raw.enabled = !disabled;
          if ( disabled ){
            var type = StoreUtilSvc.typeFromKey(store.key);
            obj.storeTimeouts.query({packageType: raw.packageType, type: type, name: raw.name},
              function(exp){
                console.log("Got expiration: " + JSON.stringify(exp) + " for: " + type + " with name: " + raw.name);
                raw.disableExpiration = exp.expiration;
              },
              function(error){
                console.log("Retrieval of disabled expiration failed: " + JSON.stringify(error));
              }
            );
          }
    };

    obj.setDisabledMap = function(scope){
        scope.disabledMap = {};

        obj.allTimeouts.query({},
          function(listing){
            if ( listing.items ) {
              for(var i=0; i<listing.items.length; i++){
                var item = listing.items[i];
                var parts = item.group.split(':');
                var key = parts[0] + ':' + parts[1] + ':' + parts[2];
                console.log("DISABLED: " + key + " (until: " + item.expiration + ")");
                scope.disabledMap[key] = item.expiration;
              }
            }
          },
          function(error){
            console.log("ERROR: " + JSON.stringify(error));
          }
        );

        scope.isDisabled = function(key){
            var result = key in scope.disabledMap;
//            console.log( "Is " + key + " disabled? " + result);
            return result;
        };

    };

    return obj;
}]);

indyServices.factory('AllStoreDisableSvc', ['$resource',
  function($resource) {
    return {
      resource: $resource(appPath('/api/admin/schedule/store/all/disable-timeout'), {}, {
        query: { method: 'GET', params: {}, isArray: false }
      }),
    };
}]);

indyServices.factory('AllEndpointsSvc', ['$resource',
  function($resource){
    return {
      resource: $resource(appPath( '/api/stats/all-endpoints' ), {}, {
        query: {method:'GET', params:{}, isArray:false},
      }),
    }
  }]);

indyServices.factory('FooterSvc', ['$resource',
  function($resource){
    return {
      resource: $resource(appPath( '/api/stats/version-info' ), {}, {
        query: {method:'GET', params:{}, isArray:false},
      }),
    }
  }]);


