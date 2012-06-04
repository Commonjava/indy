Spine = require('spine')
Group = require('models/group')
GroupForm = require('controllers/group.form')
GroupsHelper = require('controllers/groups.helper')
ListingController = require('controllers/listing')
$ = Spine.$

class Groups extends ListingController
  className: 'groups'
    
  constructor: ->
    super
    @helper = new GroupsHelper
    @form = new GroupForm
    
    @active @change
      
    Group.bind('refresh change', @render)

  render: =>
    @items = Group.all()
    @log("Rendering #{@items.length} groups")
    @html require('views/groups')(@)
    @postRender
    # Spine.trigger 'current-page', 'groups'
  
  opening: (item) =>
    @backupItem = item.dup();
    @backupItem.constituents = item.constituents.slice(0) if item.constituents
    
  cancelled: (item) =>
    if ( @backupItem )
      item.constituents = @backupItem.constituents if @backupItem.constituents
  
  createEmptyItem: ->
    new Group(constituents: [])
    
module.exports = Groups