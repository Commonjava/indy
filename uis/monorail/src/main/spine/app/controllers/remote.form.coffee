require('lib/jquery-ui')
Spine = require('spine')
DialogController = require('controllers/dialog')
RemoteX509Form = require('controllers/remote.form.x509')
RemoteProxyForm = require('controllers/remote.form.proxy')
$ = Spine.$

class RemoteForm extends DialogController
  className: 'remote-form'
    
  constructor: ->
    super
    @x509Form = new RemoteX509Form
    @proxyForm = new RemoteProxyForm
    @doctype = 'remote'
    
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
    @log("Showing form for remote: #{@item.name}" ) if @item and @item.name
    $(@el).html( require('views/remote.form')(@item) )
    @form = $(@el).find('form')
    @renderDialog( 'Remote Repository', 350, 600 )
  
module.exports = RemoteForm