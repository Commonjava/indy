if ( addons !== undefined ){
  addons.items.each( function(addon){
//    alert( addon.name );
    if ( addon.initJavascriptHref !== undefined ){
      var fileref=document.createElement('script');
      fileref.setAttribute("type","text/javascript");
      fileref.setAttribute("src", "/cp/layover/" + addon.initJavascriptHref);
      document.getElementsByTagName("head")[0].appendChild(fileref);
    }
  });
}

function registerDynamic(moduleName, controllerName) {
    // Here I cannot get the controller function directly so I
    // need to loop through the module's _invokeQueue to get it
    var queue = angular.module(moduleName)._invokeQueue;
    for(var i=0;i<queue.length;i++) {
      var call = queue[i];
      // call is in the form [providerName, providerFunc, providerArguments]
      var provider = providers[call[0]];
      if(provider && call[2][0] == controllerName) {
          // e.g. $controllerProvider.register("Ctrl", function() { ... })
          provider[call[1]].apply(provider, call[2]);
      }
    }
}
