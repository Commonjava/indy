#!/usr/bin/env node
var express  = require('express');
var argv     = require('optimist').argv;
var resolve  = require('path').resolve;

var path = resolve(argv._[0] || process.cwd());
var port = argv.p || argv.port || process.env.PORT || 9294;

server = express.createServer();
server.use(express.static(path));
server.listen(port);

console.log('Started server on: ' + port);