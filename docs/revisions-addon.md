---
title: "Revisions Data-Versioning Add-On"
---

### Version Control for Your Data

The Revisions add-on knows how to use Git to manage the contents of the `data` directory (the place where repository definitions, etc. are stored). When AProx starts, the Revisions add-on can clone or update your data directory from a remote Git repository, and you can configure it to push any changes to a remote Git repository immediately.

This can be a very useful way of tracking who changed what, for what reason, and when.

**Note:** Currently, AProx doesn't have a proper authentication or user-management mechanism, so the 'who' part is a bit lacking, but the rest is in place. We're planning to add authentication, authorization, and user management in the near future.

### Configuration

The configuration file is `etc/aprox/conf.d/revisions.conf`, and you can enable a remote Git data repository by making it to look something like this:

    [revisions]
    # 'push.enabled' determines whether changes get pushed back to the 
    # origin git repository.
    # Values: true|false
    push.enabled=true
    
    # 'conflict.action' determines what to do with changes that conflict 
    # with local configuration files.
    # Values:
    #   * merge - attempt a git merge
    #   * overwrite - keep the copy from the origin repository
    #   * keep - keep the local copy
    #
    conflict.action=keep
    
    # 'data.upstream.url' determines the origin-repository URL for 
    # cloning/pulling and pushing changes.
    #
    data.upstream.url=ssh://secretsauce.myco.com/git/aprox-data.git

### Git Repositories Using SSH

In the above example, you'll notice that the Git URL uses the SSH protocol. This requires authentication, and since AProx is using Git behind-the-scenes, you must provide a connection that doesn't require interactive authentication. Public-key authentication with a key that has an empty passphrase works pretty well. 

AProx will read your `$HOME/.ssh` directory just like any other SSH client to find the keys it can use when connecting.

### A Note on Upgrading

One thing to note about managing AProx configuration and data directories via Git: **Once you take over for the defaults provided in the AProx distribution, you're in charge of merging in new configurations, content templates, and so forth for any version upgrades.**

These configuration changes should be minimal since we go to great lengths to give AProx sensible default configuration values, and to make things like configuration backward compatible.

### Clients and UI

#### Web UI

The AProx web UI prompts users for changelog details whenever they modify a repository or group, and won't allow you to save modifications without providing a non-empty value. If you're not tracking revisions of your data files, this might seem onerous. On the other hand, it's a good idea to version these files, and the Web UI's approach means there is no additional penalty to do so.

### Java Client

The Java client API requires a changelog String parameter for any update to a repository or group definition. You can leave this null or empty if you want AProx to use its default, canned changelog entry. But again, it's a good idea to version these files, and it doesn't take much extra effort to add a reason to the update call. 

### REST API

If you're using REST directly, you can set the `metadata -> changelog` property. For cases where you want to delete a repository or group, you should use the HTTP header `changelog` to send the reason (since many HTTP clients don't support a HTTP body for the DELETE method).

AProx will of course default to a canned reason if you don't provide one with your change (in case you don't really care to track changes), but it's pretty easy to take advantage of this feature.

