[![build status](https://secure.travis-ci.org/mjijackson/usererror.png)](http://travis-ci.org/mjijackson/usererror)

UserError: Because JavaScript errors could be a lot more useful.

This package provides a base constructor (i.e. "class") that makes JavaScript
errors on V8 a lot more useful. Errors built using this class have the following
benefits:

  - They are easily subclassed
  - They are nestable (see below)

## Installation

Install this package using [npm](http://npmjs.org):

    $ npm install usererror

You are also free to [browse or download the source](https://github.com/mjijackson/error).

## Usage

The simplest usage for this class is:

```javascript
var UserError = require('usererror');

try {
  throw new UserError('Kaboom!');
} catch (e) {
  console.log(e.message);
}
```

Errors are nestable, so an error can have a reference to another error that
caused it. This is useful when you'd like to throw a high level error that was
actually caused by some lower level error. The error that was the cause is used
as the second argument to the constructor.

In the example below, we define our own error class `LoginFailedError` that
inherits from `UserError`. An instance of this class is passed to the
`loginUser` callback when it fails for some reason. Inside `loginUser` we try
and connect to the database. In reality, we could be doing any number of things
that may ultimately cause an error (e.g. reading from a flat file of user data,
validating the user id, etc.). However, we want callers to know that `loginUser`
will always return a `LoginFailedError` if it fails, and not some other error.

The solution is to wrap any other error in a `LoginFailedError` before passing
it back up the callback chain. This allows us to preserve the full stack trace
of the error (in the `fullStack` property) while giving callers a reasonable
expectation for what class(es) of errors they can expect.

Note: The `stack` property still works as you would expect, and only contains
the stack trace for the error one level deep.

```javascript
var util = require('util');
var UserError = require('usererror');

function LoginFailedError(cause) {
  UserError.call(this, 'Login failed', cause);
}

util.inherits(LoginFailedError, UserError);

function loginUser(userId, callback) {
  connectToDatabase(function (err, db) {
    if (err) {
      callback(new LoginFailedError(err));
    }

    // Login the user.
  });
}

loginUser(myUserId, function (err) {
  console.log(err.fullStack); // Recursive stack trace.
  console.log(err.stack); // Single-level stack trace.
});
```

## License

Copyright 2011 Michael Jackson

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

The software is provided "as is", without warranty of any kind, express or
implied, including but not limited to the warranties of merchantability,
fitness for a particular purpose and non-infringement. In no event shall the
authors or copyright holders be liable for any claim, damages or other
liability, whether in an action of contract, tort or otherwise, arising from,
out of or in connection with the software or the use or other dealings in
the software.
