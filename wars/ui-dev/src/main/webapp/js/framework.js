var api_base = 'http://localhost:9080/aprox/api/1.0';

var tabsVisible=false;
var $tabs;
$(function(){
    $tabs = $('#tabs').tabs({
      // tabTemplate: '<li><a href="#{href}"><span>#{label}</span></a></li>',
      collapsible: true,
    });
});
    
var panels_loaded = [];
function addMenuItem(name, id, page) {
  if (!panels_loaded[id]) {
    addTab(name, page);
    panels_loaded[id] = true;
  }
}

function addTab(name, page) {
  $tabs.tabs('add', page, name);
}

function notice( id, message ){
  $('#notices ul').append( '<li id="notice-' + id + '">' + message + '</li>' );
}

function clear_notice( id, message ){
  $('#notice-' + id).delay(1000).hide( 'slow', function(){
    $(this).detach();
  });
}

function renderApiLink(parts) {
  var url = renderApiUrl( parts );
  return '<a target="_new" href="' + url + '">' + url + '</a>';
}

function renderAccessLink(type, name) {
  return renderApiLink([type, name]);
}

function renderAdminLink(type, sub) {
  return renderApiLink(['admin', type, sub]);
}

function renderRemoteLink(url) {
  return '<a target="_new" href="' + url + '">' + url + '</a>';
}

function renderApiUrl(parts) {
  var path = '';
  for (var i = 0; i < parts.length; i++) {
      path += '/' + parts[i];
  }

  return api_base + path;
}

function renderAccessUrl(type, name) {
  return renderApiUrl([type, name]);
}

function renderAdminUrl(type, sub) {
  return renderApiUrl(['admin', type, sub]);
}

function loadOptions( select_expr, type, name_prefix, name_suffix ){
  var listUrl = renderAdminUrl( type, 'list' );
  notice( 'loading-options-' + type, "Loading options from " + listUrl );
  
  $.getJSON( listUrl, function(data)
  {
    $.each(data.items, function(index, value){
      $(select_expr).append( '<option name="' + value.key + '">' + name_prefix + ' ' + value.name + ' ' + name_suffix + '</option>' );
    });
  })
  
  .error( function()
  {
    notice( 'loading-options-' + type + '-failed', "Failed to load options of type: " + type );
    clear_notice( 'loading-options-' + type + '-failed' );
  });
  
  clear_notice( 'loading-options-' + type );
}

