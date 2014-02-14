Spine = require('spine')
Navbar = require('controllers/navbar')
Stack = require('controllers/stack')
Footer = require('controllers/footer')
RemoteRepository = require('models/remote-repository')
HostedRepository = require('models/hosted-repository')
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
      '/remotes': (params) ->
        @navbar.active(params)
        @navbar.light('remotes')
        @stack.remotes.active(params)
      '/hosted': (params) ->
        @navbar.active(params)
        @navbar.light('hosted')
        @stack.hosted.active(params)
      '/groups': (params) ->
        @navbar.active(params)
        @navbar.light('groups')
        @stack.groups.active(params)
  
    divider = $('<div />').addClass('divider')
    
    @append @navbar.active(), divider, @stack.active(), @footer.active()
    
    Status.fetch()
    RemoteRepository.fetch()
    HostedRepository.fetch()
    Group.fetch()
    
    # @navigate '/remotes'
  
module.exports = Aprox