Spine = require('spine')
RemoteRepository = require('models/remote-repository')
HostedRepository = require('models/hosted-repository')
Group = require('models/group')
$ = Spine.$

class GroupsHelper extends Spine.Controller
  constructor: ->
    super
    
  getStores: (keys) ->
    stores = []
    
    console.log("Getting stores to match keys: #{keys.join(', ')}")

    remoteRepos = RemoteRepository.all()
    console.log( "Checking #{remoteRepos.length} remote repositories")
    for store in remoteRepos
      store.key = "remote:#{store.name}" unless store.key
      console.log( "Checking remote repo: #{store.key}")
      if keys and store and keys.indexOf( store.key ) > -1
        console.log("Adding remote repo: #{store.name}")
        stores.push({'key': store.key, 'name': store.name, 'type': 'R'})

    groups = Group.all()
    console.log( "Checking #{groups.length} groups")
    for store in groups
      store.key = "group:#{store.name}" unless store.key
      console.log( "Checking group: #{store.key}")
      if keys and store and keys.indexOf( store.key ) > -1
        console.log("Adding group: #{store.name}")
        stores.push({'key': store.key, 'name': store.name, 'type': 'G'})

    hostedRepos = HostedRepository.all()
    console.log( "Checking #{hostedRepos.length} hostedRepos")
    for store in hostedRepos
      store.key = "deploy_point:#{store.name}" unless store.key
      console.log( "Checking hosted repository: #{store.key}")
      if keys and store and keys.indexOf( store.key ) > -1
        console.log("Adding hosted repository: #{store.name}")
        stores.push({'key': store.key, 'name': store.name, 'type': 'H'})

    stores
    
  allAvailable: (group) ->
    constituents = group.constituents
    available = []

    remoteRepos = RemoteRepository.all()
    available.push({'key': "remote:#{store.name}", 'name': store.name, 'type': 'R'}) for store in remoteRepos when constituents.indexOf(store.key) < 0

    groups = Group.all()
    available.push({'key': "group:#{store.name}", 'name': store.name, 'type': 'G'}) for store in groups when group.name != store.name and constituents.indexOf(store.key) < 0

    hostedRepos = HostedRepository.all()
    available.push({'key': "hostd:#{store.name}", 'name': store.name, 'type': 'H'}) for store in hostedRepos when constituents.indexOf(store.key) < 0
    
    available
    
  availableWithSortedConstituents: (group) ->
    constituents = group.constituents
    available = []
  
    remoteRepos = RemoteRepository.all()
    available.push({'key': "remote:#{store.name}", 'name': store.name, 'type': 'R'}) for store in remoteRepos

    groups = Group.all()
    available.push({'key': "group:#{store.name}", 'name': store.name, 'type': 'G'}) for store in groups

    hostedRepos = HostedRepository.all()
    available.push({'key': "hosted:#{store.name}", 'name': store.name, 'type': 'H'}) for store in hostedRepos
    
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