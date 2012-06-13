require('lib/jquery-ui')
Spine = require('spine')
DialogController = require('controllers/dialog')
$ = Spine.$

class RepositoryX509Form extends DialogController
  className: 'repository-x509-form'
  
  constructor: ->
    super
    @active @change

  render: =>
    $(@el).html( require('views/repository.x509.form')(@item) )
    @form = $(@el).find('form')
    @renderDialog( 'X.509 Options', 700, 600 )

  saving: (e) =>
    @item.fromForm(@form)
    
module.exports = RepositoryX509Form