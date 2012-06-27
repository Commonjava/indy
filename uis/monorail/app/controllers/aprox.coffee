Spine = require('spine')
Navbar = require('controllers/navbar')
Stack = require('controllers/stack')
Footer = require('controllers/footer')
Repository = require('models/repository')
Deploy = require('models/deploy')
Group = require('models/group')
Status = require('models/status')
$ = Spine.$

class Aprox extends Spine.Controller
  className: 'aprox'
    
  constructor: ->
    super
  
    @stack = new Stack
    @navbar = new Navbar
    @footer = new Footer
  
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
    
    @append @navbar.active(), divider, @stack.active(), @footer.active()
    
    Status.fetch()
    Repository.fetch()
    Deploy.fetch()
    Group.fetch()
    
    # @navigate '/repos'
  
module.exports = Aprox