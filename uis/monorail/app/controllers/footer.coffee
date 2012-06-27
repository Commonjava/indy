Spine = require('spine')
Status = require('models/status')
$ = Spine.$

class Footer extends Spine.Controller
  className: 'footer'
    
  constructor: ->
    super
    @active @change

    Status.bind('refresh change', @render)

  render: =>
    @status = Status.first()
    @html require('views/footer')(@status)
    
module.exports = Footer