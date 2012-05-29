var assert = require("assert"),
    util = require("util"),
    vows = require("vows"),
    UserError = require("./usererror");

function MyError(message, cause) {
    message = message || "Boom!";
    UserError.call(this, message, cause);
}

util.inherits(MyError, UserError);

vows.describe("UserError").addBatch({
    "A UserError": {
        topic: function () {
            this.message = "Bang!";
            var err = new UserError(this.message);
            return err;
        },
        "without a cause": {
            "should know its name": function (e, err) {
                assert.equal("UserError", err.name);
            },
            "should know its message": function (e, err) {
                assert.equal(err.message, this.message);
            },
            "should not have a cause": function (e, err) {
                assert.isUndefined(err.cause);
            },
            "should have a fullStack property": function (e, err) {
                assert.ok(typeof err.fullStack == "string");
            }
        },
        "with a cause": {
            topic: function (cause) {
                this.message = "Another bang!";
                var err = new UserError(this.message, cause);
                return err;
            },
            "should know its name": function (e, err) {
                assert.equal("UserError", err.name);
            },
            "should know its message": function (e, err) {
                assert.equal(err.message, this.message);
            },
            "should have a cause": function (e, err) {
                assert.ok(err.cause);
            },
            "should have a fullStack property": function (e, err) {
                assert.ok(typeof err.fullStack == "string");
            }
        }
    },
    "A UserError subclass instance": {
        topic: function () {
            this.message = "Boom!";
            var err = new MyError(this.message);
            return err;
        },
        "without a cause": {
            "should know its name": function (e, err) {
                assert.equal("MyError", err.name);
            },
            "should know its message": function (e, err) {
                assert.equal(err.message, this.message);
            },
            "should not have a cause": function (e, err) {
                assert.isUndefined(err.cause);
            },
            "should have a fullStack property": function (e, err) {
                assert.ok(typeof err.fullStack == "string");
            }
        },
        "with a cause": {
            topic: function (cause) {
                this.message = "Another boom!";
                var err = new MyError(this.message, cause);
                return err;
            },
            "should know its name": function (e, err) {
                assert.equal("MyError", err.name);
            },
            "should know its message": function (e, err) {
                assert.equal(err.message, this.message);
            },
            "should have a cause": function (e, err) {
                assert.ok(err.cause);
            },
            "should have a fullStack property": function (e, err) {
                assert.ok(typeof err.fullStack == "string");
            }
        }
    }
}).export(module);
