Spine = require('spine')
Navbar = require('controllers/navbar')
Stack = require('controllers/stack')
Repository = require('models/repository')
Deploy = require('models/deploy')
Group = require('models/group')
$ = Spine.$

class Aprox extends Spine.Controller
  className: 'aprox'
    
  constructor: ->
    super
  
    @stack = new Stack
    @navbar = new Navbar
  
    @routes
      '/repos': (params) ->
        @navbar.active(params)
        @navbar.light('repos')
        @stack.repos.active(params)
      '/deploys': (params) ->
        @navbar.active(params)
        @navbar.light('deploys')
        @stack.deploys.active(params)
      '/groups': (params) ->
        @navbar.active(params)
        @navbar.light('groups')
        @stack.groups.active(params)
  
    divider = $('<div />').addClass('divider')
    
    @append @navbar.active(), divider, @stack.active()
    
    Repository.fetch()
    Deploy.fetch()
    Group.fetch()
    
    # @navigate '/repos'
  
module.exports = Aprox