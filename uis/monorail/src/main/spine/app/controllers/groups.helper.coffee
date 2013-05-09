Spine = require('spine')
Repository = require('models/repository')
Deploy = require('models/deploy')
Group = require('models/group')
$ = Spine.$

class GroupsHelper extends Spine.Controller
  constructor: ->
    super
    
  getStores: (keys) ->
    stores = []
    
    console.log("Getting stores to match keys: #{keys.join(', ')}")

    repos = Repository.all()
    console.log( "Checking #{repos.length} repositories")
    for store in repos
      store.key = "repository:#{store.name}" unless store.key
      console.log( "Checking repo: #{store.key}")
      if keys and store and keys.indexOf( store.key ) > -1
        console.log("Adding repo: #{store.name}")
        stores.push({'key': store.key, 'name': store.name, 'type': 'D'})

    groups = Group.all()
    console.log( "Checking #{groups.length} groups")
    for store in groups
      store.key = "group:#{store.name}" unless store.key
      console.log( "Checking group: #{store.key}")
      if keys and store and keys.indexOf( store.key ) > -1
        console.log("Adding group: #{store.name}")
        stores.push({'key': store.key, 'name': store.name, 'type': 'D'})

    deploys = Deploy.all()
    console.log( "Checking #{deploys.length} deploys")
    for store in deploys
      store.key = "deploy_point:#{store.name}" unless store.key
      console.log( "Checking deploy point: #{store.key}")
      if keys and store and keys.indexOf( store.key ) > -1
        console.log("Adding deploy point: #{store.name}")
        stores.push({'key': store.key, 'name': store.name, 'type': 'D'})

    stores
    
  allAvailable:  ->
    available = []

    repos = Repository.all()
    available.push({'key': "repository:#{store.name}", 'name': store.name, 'type': 'R'}) for store in Repository.all()

    groups = Group.all()
    available.push({'key': "group:#{store.name}", 'name': store.name, 'type': 'G'}) for store in Group.all()

    deploys = Deploy.all()
    available.push({'key': "deploy_point:#{store.name}", 'name': store.name, 'type': 'D'}) for store in Deploy.all()
    
    available
    
  availableWithSortedConstituents: (constituents) ->
    available = []
  
    repos = Repository.all()
    available.push({'key': "repository:#{store.name}", 'name': store.name, 'type': 'R'}) for store in Repository.all()

    groups = Group.all()
    available.push({'key': "group:#{store.name}", 'name': store.name, 'type': 'G'}) for store in Group.all()

    deploys = Deploy.all()
    available.push({'key': "deploy_point:#{store.name}", 'name': store.name, 'type': 'D'}) for store in Deploy.all()
    
    available.sort( (a,b) ->
      ai = if constituents then constituents.indexOf(a.key) else -1
      bi = if constituents then constituents.indexOf(b.key) else -1

      result = 0
      if ai < 0 and bi >= 0
        result = 1;
      else if ai >= 0
        if bi < 0
          result = -1
        else
          result = if ai - bi < 0 then -1 else 1

      result
    )
    
    available
    
module.exports = GroupsHelper