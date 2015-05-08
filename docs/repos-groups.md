---
title: "The Basics: Repositories and Groups"
---

[Documentation Contents](index.html)

### Overview

All Maven repository managers share a few core functions:

* [Proxying remote repositories](remote-repositories.html) to insulate users from network outages and slow transfer times
* [Hosting deployed artifacts](hosted-repositories.html) that are deployed in a local environment (often snapshot builds, and often for use with a continuous integration system)
* [Grouping repositories together](repository-groups.html) behind a single repository URL with merged metadata to allow them to be used as a single virtual repository

However, in each of these areas AProx adds some extra conveniences that make it simpler to manage your build environment. Additionally, AProx offers a [few nice features](#a-little-something-extra) that apply to all repositories and groups.

Let's go through each type in detail and explore some of the ways AProx makes life a little easier, even with these common tasks.


### A Little Something Extra

In addition to the features described above, AProx offers some convenient features that are common across all artifact store types (hosted and remote repositories, and groups). 

One of these is that any store can contain a text description. This seems trivial, but sometimes in large deployments repository names can become a bit cryptic. A text description allows you to escape the terseness of a repository name and describe what each repository is actually used for. The UI even allows you to search this description field, along with the URLs (the local URL, and in the case of a remote proxy, the remote URL) and repository name.

Another nice feature common to all store types is revision storage. All data files that contain AProx state[^1] are stored in a Git repository, and each change is committed as a new revision of the affected file. This is why the UI prompts you to enter a changelog entry whenever you make a change, and the Java client API's methods for manipulating artifact stores all take a parameter for the changelog entry. While this is still a nascent feature of AProx, it lays the groundwork for much more sophisticated revision review and management (such as rolling back to previous states). More information about Git data versioning can be found in 

[^1]: AProx state is distinct from configuration files that determine what ports AProx uses and things like that, and from artifact caches which store the artifacts themselves.