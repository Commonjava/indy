Spine = require('spine')

class ListingController extends Spine.Controller
  events:
    'click .item-key': 'showItem'
    'click .create': 'showNewItem'
    'mouseenter .item-field': 'highlightItem'
    'mouseleave .item-field': 'deHighlightItem'
    
  change: (item) =>
    @render()
  
  highlightItem: (e) =>
    e.preventDefault()
    $(e.currentTarget).parents('.item').find('.item-field').addClass("item-highlight")
  
  deHighlightItem: (e) =>
    e.preventDefault()
    $(e.currentTarget).parents('.item').find('.item-field').removeClass("item-highlight")
  
  createEmptyItem: ->
    throw 'Override createEmptyItem()!'
  
  showNewItem: (e) =>
    @log("Creating...")
    e.preventDefault()
    @shownItem = @createEmptyItem()
    @log("New item: #{@shownItem}")
    @show( false )
    
  showItem: (e) =>
    e.preventDefault()
    name = $(e.target).attr('name')
    console.log("Selected from target(#{e.target}): #{name}")
    @shownItem = item for item in @items when item.name == name
    @show( true )
  
  show: (editing) =>
    @log( "Opening item with key: #{@shownItem.key}")
    if ( @shownItem )
      @opening(@shownItem) if editing and @opening
      @log("opening form in #{if editing then 'edit' else 'create'} mode.")
      @form.open(@shownItem, editing, @)

  postRender: =>
    # NOP
    
module.exports = ListingController