if ( addons !== undefined ){
  addons.items.each( function(addon){
    if ( addon.initJavascriptHref !== undefined ){
      var fileref=document.createElement('script');
      fileref.setAttribute("type","text/javascript");
      fileref.setAttribute("src", appPath( "/cp/layover/" + addon.initJavascriptHref ));
      
      document.getElementsByTagName("body")[0].appendChild(fileref);
    }
  });
}

