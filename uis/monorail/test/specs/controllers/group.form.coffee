require = window.require

describe 'Group.form', ->
  Group.form = require('controllers/group.form')
  
  it 'can noop', ->
    