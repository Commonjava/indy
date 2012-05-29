Spine = require('spine')

class Navbar extends Spine.Controller
  className: 'navbar'

  events:
    'click .repos': 'showRepos'
    'click .deploys': 'showDeploys'
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
      when 'repos'
        $(@el).find('.repos').addClass('ui-state-active')
        $(@el).find('.deploys').removeClass('ui-state-active')
        $(@el).find('.groups').removeClass('ui-state-active')
      when 'deploys'
        $(@el).find('.repos').removeClass('ui-state-active')
        $(@el).find('.deploys').addClass('ui-state-active')
        $(@el).find('.groups').removeClass('ui-state-active')
      when 'groups'
        $(@el).find('.repos').removeClass('ui-state-active')
        $(@el).find('.deploys').removeClass('ui-state-active')
        $(@el).find('.groups').addClass('ui-state-active')
        
  showRepos: ->
    @navigate '/repos'

  showDeploys: ->
    @navigate '/deploys'

  showGroups: ->
    @navigate '/groups'

module.exports = Navbar