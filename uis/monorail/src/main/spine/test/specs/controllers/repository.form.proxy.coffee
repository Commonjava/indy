require = window.require

describe 'Repository.form.proxy', ->
  Repository.form.proxy = require('controllers/repository.form.proxy')
  
  it 'can noop', ->
    