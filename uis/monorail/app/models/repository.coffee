Spine = require('spine')

class Repository extends Spine.Model
  @configure 'Repository', 'name', 'remote_url', 'timeout_seconds', 'is_passthrough', 'cache_timeout_seconds'
  @extend Spine.Model.Ajax
  
  @url: '/aprox/api/1.0/admin/repositories'
  
  @fromJSON: (objects) ->
    return unless objects

    if typeof objects is 'string'
      objects = JSON.parse(objects)

    if objects.items
      objects = objects.items
      if objects
        for object in objects
          if object
            object.remote_url = object.url if object.url
            object.id = object.name if object.name

    if Spine.isArray(objects)
      (new @(value) for value in objects)
    else
      new @(objects)
  
  toJSON: (objects) ->
    data = @attributes()
    console.log("Unmodified Serialized JSON: '#{JSON.stringify(data)}'")
    
    result = null
    if Spine.isArray(data)
      objs = []
      for obj in data
        if obj
          o =
            'id': obj.name
            'name': obj.name
            'url': obj.remote_url
            'timeout_seconds': parseInt obj.timeout_seconds
            'is_passthrough': if obj.is_passthrough is 'on' then true else false
            'cache_timeout_seconds': if obj.is_passthrough is true then -1 else parseInt obj.cache_timeout_seconds
            
          objs.push(o)
      
      data =
        'items': objs
    else
      if data
        data = 
          'id': data.name
          'name': data.name
          'url': data.remote_url
          'timeout_seconds': parseInt data.timeout_seconds
          'is_passthrough': if data.is_passthrough is 'on' then true else false
          'cache_timeout_seconds': if data.is_passthrough is true then -1 else parseInt data.cache_timeout_seconds
    
    console.log("Serialized JSON: '#{JSON.stringify(data)}'")
    data
  
module.exports = Repository