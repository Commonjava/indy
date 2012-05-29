require('lib/jquery-ui')
Spine = require('spine')
DialogController = require('controllers/dialog')
$ = Spine.$

class RepositoryForm extends DialogController
  className: 'repository-form'
    
  constructor: ->
    super
    @active @change
  
  render: =>
    @log("Showing form for repository: #{@item.name}" ) if @item and @item.name
    $(@el).html( require('views/repository.form')(@item) )
    @form = $(@el).find('form')
    @renderDialog( 'Repository', 350, 600 )

module.exports = RepositoryForm