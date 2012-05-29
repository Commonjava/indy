Spine = require('spine')

class Group extends Spine.Model
  @configure 'Group', 'name', 'constituents'
  @extend Spine.Model.Ajax
  
  @url: '/aprox/api/1.0/admin/groups'
  
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
      for obj in data
        if obj
          obj.id = obj.name

      data =
        'items': data
    else
      if data
        data.id = data.name

    console.log("Serialized JSON: '#{JSON.stringify(data)}'")
    data
  
module.exports = Group