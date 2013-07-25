var util = require('util');

module.exports = UserError;

/**
 * An Error subclass that is better-suited for subclassing and is nestable.
 * Arguments are the error message and an optional cause, which should be
 * another error object that was responsible for causing this error at some
 * lower level.
 */
function UserError(message, cause) {
  Error.call(this);
  Error.captureStackTrace(this, this.constructor);
  this.name = this.constructor.name;
  this.message = message;
  this.cause = cause;
}

util.inherits(UserError, Error);

UserError.prototype.__defineGetter__('fullStack', function () {
  var stack = this.stack;

  if (this.cause) {
    stack += '\nCaused by ' + (this.cause.fullStack || this.cause.stack);
  }

  return stack;
});
