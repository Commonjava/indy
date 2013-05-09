require = window.require

describe 'Repository.form.x509', ->
  Repository.form.x509 = require('controllers/repository.form.x509')
  
  it 'can noop', ->
    