var assert = require('assert');
var util = require('util');
var UserError = require('../user-error');

describe('A UserError', function () {
  describe('without a cause', function () {
    var message, err;
    beforeEach(function () {
      message = 'Bang!';
      err = new UserError(message);
    });

    it('knows its name', function () {
      assert.equal('UserError', err.name);
    });

    it('knows its message', function () {
      assert.equal(err.message, message);
    });

    it('does not have a cause', function () {
      assert.ok(typeof err.cause === 'undefined');
    });

    it('has a fullStack property', function () {
      assert.ok(typeof err.fullStack === 'string');
    });
  });

  describe('with a cause', function () {
    var cause, message, err;
    beforeEach(function () {
      cause = new UserError;
      message = 'Another bang!';
      err = new UserError(message, cause);
    });

    it('knows its name', function () {
      assert.equal('UserError', err.name);
    });

    it('knows its message', function () {
      assert.equal(err.message, message);
    });

    it('has a cause', function () {
      assert.ok(err.cause);
    });

    it('has a fullStack property', function () {
      assert.ok(typeof err.fullStack === 'string');
    })
  });
});

describe('A UserError subclass instance', function () {
  function MyError(message, cause) {
    message = message || 'Boom!';
    UserError.call(this, message, cause);
  }

  util.inherits(MyError, UserError);

  describe('without a cause', function () {
    var message, err;
    beforeEach(function () {
      message = 'Boom!';
      err = new MyError(message);
    });

    it('knows its name', function () {
      assert.equal('MyError', err.name);
    });

    it('knows its message', function () {
      assert.equal(err.message, message);
    });

    it('does not have a cause', function () {
      assert.ok(typeof err.cause === 'undefined');
    });

    it('has a fullStack property', function () {
      assert.ok(typeof err.fullStack === 'string');
    });
  });

  describe('with a cause', function () {
    var cause, message, err;
    beforeEach(function () {
      cause = new UserError;
      message = 'Another boom!';
      err = new MyError(message, cause);
    });

    it('knows its name', function () {
      assert.equal('MyError', err.name);
    });

    it('knows its message', function () {
      assert.equal(err.message, message);
    });

    it('has a cause', function () {
      assert.ok(err.cause);
    });

    it('has a fullStack property', function () {
      assert.ok(typeof err.fullStack === 'string');
    });
  });
});
