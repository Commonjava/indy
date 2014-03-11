require('lib/jquery-ui')
require('lib/jquery.ui.multiselect')
Spine = require('spine')
RemoteRepository = require('models/remote-repository')
HostedRepository = require('models/hosted-repository')
Group = require('models/group')
GroupsHelper = require('controllers/groups.helper')
DialogController = require('controllers/dialog')
$ = Spine.$

class GroupForm extends DialogController
  className: 'group-form'
    
  events:
    'submit form': 'submit'
    'drop .available': 'deselect'
    'drop .selected-container *': 'select'
    'dragstop .selected-container *': 'reRender'
    'dragstop .available': 'reRender'
    'dropover .selected li': 'highlightDrag'
    'dropout .selected li': 'deHighlightDrag'
    
  constructor: ->
    super
    @helper = new GroupsHelper
    @doctype = 'group'
    
    @active @change
  
  select: (evt, ui) =>
    # evt.preventDefault()
    @log("ui object: #{ui}")
    store = $(ui.draggable).attr('name')
    
    target = $(evt.target).attr('name')
    
    idx = if @item.constituents then @item.constituents.indexOf(store) else -1
    if @item.constituents?.indexOf(store) > -1
      # if the item already exists, we're moving it, so remove from the old location and prepare to add below...
      @item.constituents[idx...idx+1] = []
      
    if not @item.constituents or @item.constituents.indexOf(target) < 0
      @log("Adding #{store}")
      @item.constituents.push store
    else if @item.constituents.indexOf(store) < 0
      idx = @item.constituents.indexOf(target)

      cons = []
      if idx > 0
        cons.push i for i in @item.constituents[0..idx - 1]
      else
        idx=0
        
      @log("Adding #{store} at index: #{idx}")
      cons.push store
      cons.push i for i in @item.constituents[idx..]
      
      @log("Resulting constituents: #{cons}")
      
      @item.constituents = cons
      
  
  highlightDrag: (e) =>
    # e.preventDefault()
    val = $(e.target).html()
    $(e.target).html($('<div class="drop-insertion-point"></div>').html(val))
    $(e.target).addClass('ui-state-active').addClass('drop-insertion-parent')
  
  deHighlightDrag: (e) =>
    # e.preventDefault()
    $(e.target).removeClass('ui-state-active').removeClass('drop-insertion-parent')
    val = $(e.target).find('.drop-insertion-point').html()
    $(e.target).html( val )

  reRender: (evt) =>
    # evt.preventDefault()
    @log("Re-rendering constituents")
    @item.fromForm(@form)
    @render()
  
  deselect: (evt, ui) =>
    # evt.preventDefault()
    store = $(ui.draggable).attr('name')
    idx = @item.constituents.indexOf(store)
    @item.constituents.splice(idx,1)

  render: =>
    $(@el).html( require('views/group.form')(@) )
    @form = $(@el).find('form')
    
    # $(@el).find('.constituents-selector').multiselect();
    # 
    $(@el).find('.selected > *').draggable( {helper: 'clone', 'scroll': false, 'opacity': 0.35} )
    $(@el).find('.available > *').draggable( {helper: 'clone', 'scroll': false, 'opacity': 0.35} )
    
    $(@el).find('.dropzone').droppable()
    $(@el).find('.selected').droppable(greedy: true)
    $(@el).find('.available').droppable( greedy: true)
    $(@el).find('.selected > *').droppable(greedy: true)
    
    @renderDialog( 'Group', 500, 700 )
  
module.exports = GroupForm