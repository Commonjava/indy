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
    @log("Showing X.509 form for repository: #{@item.name}" ) if @item and @item.name
    $(@el).html( require('views/repository.x509.form')(@item) )
    @form = $(@el).find('form')
    @renderDialog( 'X.509 Options', 700, 600 )
    
module.exports = RepositoryX509Form