Spine = require('spine')

class Navbar extends Spine.Controller
  className: 'navbar'

  events:
    'click .remotes': 'showRemotes'
    'click .hosted': 'showHosted'
    'click .groups': 'showGroups'

  constructor: ->
    super
    @html require('views/navbar')()
    @active @change
  
  change: (params) =>
    @render()
  
  render: (params) =>
    $(@el).find('.button').addClass('ui-corner-top')

  light: (type) ->
    switch type
      when 'remotes'
        $(@el).find('.remotes').addClass('ui-state-active')
        $(@el).find('.hosted').removeClass('ui-state-active')
        $(@el).find('.groups').removeClass('ui-state-active')
      when 'hosted'
        $(@el).find('.remotes').removeClass('ui-state-active')
        $(@el).find('.hosted').addClass('ui-state-active')
        $(@el).find('.groups').removeClass('ui-state-active')
      when 'groups'
        $(@el).find('.remotes').removeClass('ui-state-active')
        $(@el).find('.hosted').removeClass('ui-state-active')
        $(@el).find('.groups').addClass('ui-state-active')
        
  showRemotes: ->
    @navigate '/remotes'

  showHosted: ->
    @navigate '/hosted'

  showGroups: ->
    @navigate '/groups'

module.exports = Navbar