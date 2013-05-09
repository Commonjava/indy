Spine = require('spine')
Deploy = require('models/deploy')
DeployForm = require('controllers/deploy.form')
ListingController = require('controllers/listing')
$ = Spine.$

class Deploys extends ListingController
  className: 'deploys'
    
  constructor: ->
    super
    @form = new DeployForm
    
    @active @change
    Deploy.bind('refresh change', @render)
  
  render: =>
    @items = Deploy.all()
    @log("Rendering #{@items.length} deploy points")
    @html require('views/deploys')(@)
    @postRender
    Spine.trigger 'current-page', 'deploys'
    
  createEmptyItem: ->
    new Deploy('allow_releases': true, 'allow_snapshots': true, 'snapshot_timeout_seconds': 86400)
    
module.exports = Deploys