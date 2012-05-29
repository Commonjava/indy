require = window.require

describe 'Repository.form', ->
  Repository.form = require('controllers/repository.form')
  
  it 'can noop', ->
    