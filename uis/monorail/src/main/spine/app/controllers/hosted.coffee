Spine = require('spine')
HostedRepository = require('models/hosted-repository')
HostedForm = require('controllers/hosted.form')
ListingController = require('controllers/listing')
$ = Spine.$

class Hosted extends ListingController
  className: 'hosted'
    
  constructor: ->
    super
    @form = new HostedForm
    
    @active @change
    HostedRepository.bind('refresh change', @render)
  
  render: =>
    @items = HostedRepository.all()
    @log("Rendering #{@items.length} hosted repos")
    @html require('views/hosted')(@)
    @postRender
    Spine.trigger 'current-page', 'hosted'
    
  createEmptyItem: ->
    new HostedRepository('allow_releases': true, 'allow_snapshots': true, 'snapshot_timeout_seconds': 86400)
    
module.exports = Hosted