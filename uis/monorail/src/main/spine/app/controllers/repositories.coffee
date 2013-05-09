Spine = require('spine')
Repository = require('models/repository')
RepositoryForm = require('controllers/repository.form')
ListingController = require('controllers/listing')
$ = Spine.$

class Repositories extends ListingController
  className: 'repositories'
    
  constructor: ->
    super
    @form = new RepositoryForm
    
    @active @change

    Repository.bind('refresh change', @render)
    
  render: =>
    @items = Repository.all()
    @log("Rendering #{@items.length} repositories")
    @html require('views/repositories')(@)
    @postRender
  
  deselect: (item) =>
    @log("De-selected: #{item.name}")
  
  createEmptyItem: ->
    new Repository('timeout_seconds': 10, 'cache_timeout_seconds': 86400)

module.exports = Repositories