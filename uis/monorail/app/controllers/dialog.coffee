Spine = @Spine or require('spine')

class DialogController extends Spine.Controller
  elements:
    'form': 'form'
  
  events:
    'dialogclose .repository-form': 'close'
    'change .endisable': 'processEndisable'
    'submit form': 'submit'
    'click .button': 'processFormButton'
  
  processFormButton: (e) =>
    # NOP

  delete: (e) =>
    @log("Deleting: #{@item}")
    @deleteDialog = require('views/delete.dialog')(@item)
    $(@deleteDialog).dialog(
      title: "Delete",
      autoOpen: true,
      modal: true,
      buttons:
        Yes: =>
          @deleting() if @deleting
          @owner.deleting(@item) if @owner and @owner.deleting
          @item.destroy()
        
          $(@deleteDialog).dialog('close')
          $(@el).dialog('close')
        No: =>
          $(@deleteDialog).dialog('close')
    )
    
  close: (e) =>
    @dialogClosed( e )
    $(@el).dialog('close')
  
  dialogClosed: (e) =>
    e.preventDefault()
    @canceling( e ) if @canceling
    @owner.cancelled(@item) if @owner and @owner.cancelled
    @cleanup()
  
  cleanup: =>
    # @item = null
    # @owner = null
    # @editing = null
    
  open: (item, editing, owner) =>
    @owner = owner
    @editing = editing
    @opening(item) if @opening
    @change(item)
  
  change: (item) =>
    @item = item
    @render()
  
  renderDialog: (title, width, height) =>
    params =
      title: "#{if @editing then 'Edit' else 'New'} #{title}"
      autoOpen: true
      height: width
      width: height
      modal: true
      # resizable: false
      close: @dialogClosed
      buttons:
        Save: (e) =>
          @submit(e)
        Cancel: (e) =>
          @close(e)
        Delete: (e) =>
          @delete(e)
      
    $(@el).dialog(params)
    @initEndisables()
    
    $(@el).find('input.name-field').attr('size', '15')
    $(@el).find('input[type=number]').attr('size', '4')
    $(@el).find('input[type=url]').attr('size', '50')
    
    # @log($(@el).html())
    # $(@el).html()
  
  initEndisables: =>
    $(@el).find('.endisable').each( (i,target) =>
      @endisable(target)
    )
  
  processEndisable: (e) =>
    @endisable(e.target)
  
  endisable: (target) =>
    name = $(target).attr('name')
    affected = $(target).attr('affects')
    return unless affected
    
    affected = affected.split(' ')
    checked = $(@el).find(".endisable[name=#{name}]:checked")
    
    for a in affected
      reverse = false
      if ( a.substring(0,1) == '!' )
        a = a.substring(1)
        reverse = true
        
      affected = $(@el).find("[name=#{a}]")
      if reverse != ( checked.length == 0 )
        $(affected).hide()
      else
        $(affected).show()
  
  submit: (e) =>
    @log("Item is: #{JSON.stringify(@item)}")
    e.preventDefault()
    $(@el).dialog('close')
    @saving( e ) if @saving
    @owner.saved(@item) if @owner and @owner.saved
    @cleanup()
  
  saving: (e) =>
    @item.fromForm(@form).save()
    @log("From form: #{JSON.stringify(@item)}")

module?.exports = DialogController