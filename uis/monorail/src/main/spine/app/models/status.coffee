Spine = require('spine')

class Status extends Spine.Model
  @configure 'Status', 'version', 'built_by', 'commit_id', 'built_on'
             
  @extend Spine.Model.Ajax
  
  @url: window.location.pathname + 'api/1.0/stats/version-info'
  
  # Spine.Ajax.disable ->
  #   record.destroy()
  #   record.save()
  #   record.update()
  
module.exports = Status