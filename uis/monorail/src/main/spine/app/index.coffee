require('lib/setup')
Spine = require('spine')
Aprox = require('controllers/aprox')
$ = Spine.$

class App extends Spine.Controller
  constructor: ->
    super
    
    @aprox = new Aprox
    @append @aprox.active()
    
    Spine.Route.setup()

module.exports = App
    