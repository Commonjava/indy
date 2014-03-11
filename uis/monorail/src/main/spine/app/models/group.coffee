Spine = require('spine')

class Group extends Spine.Model
  @configure 'Group', 'name', 'constituents'
  @extend Spine.Model.Ajax
  
  @url: window.location.pathname + 'api/1.0/admin/groups'
  
  @fromJSON: (objects) ->
    return unless objects

    if typeof objects is 'string'
      objects = JSON.parse(objects)

    if objects.items
      objects = objects.items
      if objects
        for object in objects
          if object
            if object.key
              key = object.key
              parts = key.split(':')
              object.name = parts[1] if parts and parts.length > 1
              object.type = parts[0] if parts and parts.length > 0
              object.id = object.name
            
            object.key = "#{object.key.type}:#{object.key.name}" if object.key and object.key.type and object.key.name
    else
      object = objects
      if object.key
        key = object.key
        parts = key.split(':')
        object.name = parts[1] if parts and parts.length > 1
        object.type = parts[0] if parts and parts.length > 0
        object.id = object.name
        
      object.key = "#{object.key.type}:#{object.key.name}" if object.key and object.key.type and object.key.name

    if Spine.isArray(objects)
      (new @(value) for value in objects)
    else
      new @(objects)
  
  toJSON: (objects) ->
    data = @attributes()

    result = null
    if Spine.isArray(data)
      for obj in data
        if obj
          obj.id = obj.name
          obj.key = "group:#{obj.name}"

      data =
        'items': data
    else
      if data
        data.id = data.name
        data.key = "group:#{data.name}"

    data
  
module.exports = Group