require = window.require

describe 'Deploy.form', ->
  Deploy.form = require('controllers/deploy.form')
  
  it 'can noop', ->
    