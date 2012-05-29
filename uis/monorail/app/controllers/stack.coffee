Spine = require('spine')
Repositories = require('controllers/repositories')
Deploys = require('controllers/deploys')
Groups = require('controllers/groups')
$ = Spine.$

class Stack extends Spine.Stack
  className: 'main stack'
    
  controllers:
    repos: Repositories
    deploys: Deploys
    groups: Groups
    
module.exports = Stack