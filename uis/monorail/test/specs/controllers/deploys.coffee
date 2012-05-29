require = window.require

describe 'Deploys', ->
  Deploys = require('controllers/deploys')
  
  it 'can noop', ->
    