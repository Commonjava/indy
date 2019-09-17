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
// See http://stackoverflow.com/questions/5306680/move-an-array-element-from-one-array-position-to-another
Array.prototype.move = function (old_index, new_index) {
    if (new_index >= this.length) {
        var k = new_index - this.length;
        while ((k--) + 1) {
            this.push(undefined);
        }
    }
    this.splice(new_index, 0, this.splice(old_index, 1)[0]);
    return this; // for testing purposes
};

Array.prototype.each = function (callback) {
  for(var i=0; i<this.length; i++){
    if ( callback(this[i]) == false ){
      break;
    }
  }
  return this;
};

var buildUrl = function(baseUrl, path){
  if ( baseUrl.endsWith('/') ){
    if ( path.startsWith('/') ){
      return baseUrl + path.substring(1);
    }
    else{
      return baseUrl + path;
    }
  }
  else if ( path.startsWith( '/' ) ){
    return baseUrl + path;
  }
  else{
    return baseUrl + '/' + path;
  }
};

var appUrl = function(path){
  var img = document.createElement('img');
  img.src = path; // set string url
  var url = img.src; // get qualified url
  img.src = null; // no server request
  return url;
};

var appPath = function( path ){
  var result = window.location.pathname;
//  alert( "raw result: '" + result + "'");
  
  var lastSlash = result.lastIndexOf('/');
  if( lastSlash && lastSlash > -1 ){
    result = result.substring(0, lastSlash + 1);
//    alert( "trimmed result to: '" + result + "'");
  }
  
  if ( !result.endsWith( '/' ) ){
    result = '/';
  }
  
  if ( path.length > 0 ){
    result += path.startsWith( '/' ) ? path.substring(1) : path;
  }
  
//  alert( "constructed path: " + result );
  
  return result;
};
