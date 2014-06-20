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
