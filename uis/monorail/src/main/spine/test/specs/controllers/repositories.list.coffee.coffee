require = window.require

describe 'Repositories.list.coffee', ->
  Repositories.list.coffee = require('controllers/repositories.list.coffee')
  
  it 'can noop', ->
    