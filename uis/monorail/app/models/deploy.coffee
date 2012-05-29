Spine = require('spine')

class Deploy extends Spine.Model
  @configure 'Deploy', 'name', 'allow_snapshots', 'allow_releases', 'snapshot_timeout_seconds'
  @extend Spine.Model.Ajax
  
  @url: '/aprox/api/1.0/admin/deploys'
  
  @fromJSON: (objects) ->
    return unless objects

    if typeof objects is 'string'
      objects = JSON.parse(objects)

    if objects.items
      objects = objects.items
      if objects
        for object in objects
          if object
            object.id = object.name if object.name

    if Spine.isArray(objects)
      (new @(value) for value in objects)
    else
      new @(objects)
  
  toJSON: (objects) ->
    data = @attributes()

    result = null
    if Spine.isArray(data)
      objs = []
      for obj in data
        if obj
          o =
            'id': obj.name
            'name': obj.name
            'allow_releases': if obj.allow_releases is 'on' then true else false
            'allow_snapshots': if obj.allow_snapshots is 'on' then true else false
            'snapshot_timeout_seconds': if obj.allow_snapshots is true then parseInt obj.snapshot_timeout_seconds else -1
            
          objs.push(o)
      
      data =
        'items': objs
    else
      if data
        data = 
          'id': data.name
          'name': data.name
          'allow_releases': if data.allow_releases is 'on' then true else false
          'allow_snapshots': if data.allow_snapshots is 'on' then true else false
          'snapshot_timeout_seconds': if data.allow_snapshots is true then parseInt data.snapshot_timeout_seconds else -1

    console.log("Serialized JSON: '#{JSON.stringify(data)}'")
    data
  
module.exports = Deploy