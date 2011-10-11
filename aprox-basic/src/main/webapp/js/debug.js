function pp(obj, maxdepth) {
  var od = new Object;
  var result = "";
  var len = 0;

  for ( var property in obj) {
    var value = obj[property];
    if (typeof value == 'string')
      value = "'" + value + "'";
    else if (typeof value == 'object') {
      if (value instanceof Array) {
        value = "[ " + value + " ]";
      } else if (maxdepth > 1) {
        var ood = pp(value, maxdepth - 1);
        value = "{ " + ood.dump + " }";
      }
    }
    result += "\n'" + property + "' : " + value + ", ";
    len++;
  }
  od.dump = result.replace(/, $/, "");
  od.len = len;

  return od;
}