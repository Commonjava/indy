require('lib/jquery-ui')
Spine = require('spine')
DialogController = require('controllers/dialog')
$ = Spine.$

class RepositoryProxyForm extends DialogController
  className: 'repository-proxy-form'
  
  constructor: ->
    super
    @active @change

  render: =>
    $(@el).html( require('views/repository.proxy.form')(@item) )
    @form = $(@el).find('form')
    @renderDialog( 'HTTP Proxy Options', 350, 600 )
    
  saving: (e) =>
    @item.fromForm(@form)
      
module.exports = RepositoryProxyForm