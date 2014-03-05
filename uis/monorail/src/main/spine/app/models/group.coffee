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
            object.id = object.key.name if object.key and object.key.name
            object.name = object.key.name if object.key and object.key.name
            object.type = object.key.type if object.key and object.key.type
            object.key = "#{object.key.type}:#{object.key.name}" if object.key and object.key.type and object.key.name
    else
      object = objects
      object.id = object.key.name if object.key and object.key.name
      object.name = object.key.name if object.key and object.key.name
      object.type = object.key.type if object.key and object.key.type
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