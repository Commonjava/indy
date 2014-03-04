Spine = require('spine')
RemoteRepository = require('models/remote-repository')
RemoteForm = require('controllers/remote.form')
ListingController = require('controllers/listing')
$ = Spine.$

class Remotes extends ListingController
  className: 'remotes'
    
  constructor: ->
    super
    @form = new RemoteForm
    
    @active @change

    RemoteRepository.bind('refresh change', @render)
    
  render: =>
    @items = RemoteRepository.all()
    @log("Rendering #{item.name}") for item in @items
    
    @log("Rendering #{@items.length} remotes")
    @html require('views/remotes')(@)
    @postRender
  
  deselect: (item) =>
    @log("De-selected: #{item.name}")
  
  createEmptyItem: ->
    new RemoteRepository('timeout_seconds': 10, 'cache_timeout_seconds': 86400)

module.exports = Remotes