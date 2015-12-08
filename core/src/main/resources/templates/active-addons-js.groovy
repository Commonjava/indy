'use strict'
var addons = ${addonsJson};

var indyAddons = angular.module('indy.addons', ['ngResource']);

<% if( addonsLogic ){ addonsLogic.each{ key,js -> %>
/** START: ${key} **/
${js}
/** END: ${key} **/
<% }} %>
