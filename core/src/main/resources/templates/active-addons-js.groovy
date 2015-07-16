'use strict'
var addons = ${addonsJson};

var aproxAddons = angular.module('aprox.addons', ['ngResource']);

<% if( addonsLogic ){ addonsLogic.each{ key,js -> %>
/** START: ${key} **/
${js}
/** END: ${key} **/
<% }} %>
