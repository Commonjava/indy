require('lib/jquery-ui')
Spine = require('spine')
DialogController = require('controllers/dialog')
RepositoryX509Form = require('controllers/repository.form.x509')
RepositoryProxyForm = require('controllers/repository.form.proxy')
$ = Spine.$

class RepositoryForm extends DialogController
  className: 'repository-form'
    
  constructor: ->
    super
    @x509Form = new RepositoryX509Form
    @proxyForm = new RepositoryProxyForm
    @doctype = 'repository'
    
    @active @change
  
  processFormButton: (e) =>
    dest = $(e.target).attr('dest')
    switch dest
      when 'x509'
        @log( 'open X.509 dialog' )
        @x509Form.open(@item, @)
      when 'proxies'
        @log( 'open proxy dialog' )
        @proxyForm.open(@item, @)

  render: =>
    @log("Showing form for repository: #{@item.name}" ) if @item and @item.name
    $(@el).html( require('views/repository.form')(@item) )
    @form = $(@el).find('form')
    @renderDialog( 'Repository', 350, 600 )
  
module.exports = RepositoryForm