Spine = require('spine')
RemoteRepositories = require('controllers/remotes')
HostedRepositories = require('controllers/hosted')
Groups = require('controllers/groups')
$ = Spine.$

class Stack extends Spine.Stack
  className: 'main stack'
    
  controllers:
    remotes: RemoteRepositories
    hosted: HostedRepositories
    groups: Groups
    
module.exports = Stack