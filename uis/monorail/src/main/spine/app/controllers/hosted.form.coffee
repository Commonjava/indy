require('lib/jquery-ui')
Spine = require('spine')
DialogController = require('controllers/dialog')
$ = Spine.$

class HostedForm extends DialogController
  className: 'hosted-form'
    
  constructor: ->
    super
    @doctype = 'hosted'
    @active @change
  
  render: =>
    @log("Showing form for hosted repo: #{@item.name}" ) if @item and @item.name
    $(@el).html( require('views/hosted.form')(@item) )
    @form = $(@el).find('form')
    @renderDialog('Hosted Repository', 350, 600)
  
module.exports = HostedForm