require('lib/jquery-ui')
Spine = require('spine')
DialogController = require('controllers/dialog')
$ = Spine.$

class DeployForm extends DialogController
  className: 'deploy-form'
    
  constructor: ->
    super
    @doctype = 'deploy_point'
    @active @change
  
  render: =>
    @log("Showing form for deploy point: #{@item.name}" ) if @item and @item.name
    $(@el).html( require('views/deploy.form')(@item) )
    @form = $(@el).find('form')
    @renderDialog('Deploy Point', 350, 600)
  
module.exports = DeployForm