require = window.require

describe 'Groups.helper', ->
  Groups.helper = require('controllers/groups.helper')
  
  it 'can noop', ->
    