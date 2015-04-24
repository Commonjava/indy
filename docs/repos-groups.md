---
title: "The Basics: Repositories and Groups"
---

### Overview

All Maven repository managers share a few core functions:

* [Proxying remote repositories](#proxying-remote-repositories) to insulate users from network outages and slow transfer times
* [Hosting deployed artifacts](#hosting-deployed-artifacts) that are deployed in a local environment (often snapshot builds, and often for use with a continuous integration system)
* [Grouping repositories together](#grouping-repositories-together) behind a single repository URL with merged metadata to allow them to be used as a single virtual repository

However, in each of these areas AProx adds some extra conveniences that make it simpler to manage your build environment. Additionally, AProx offers a [few nice features](#a-little-something-extra) that apply to all repositories and groups.

Let's go through each type in detail and explore some of the ways AProx makes life a little easier, even with these common tasks.

### Proxying Remote Repositories

Like any good repository manager, AProx supports proxying content from external (remote) repositories. In order to insulate users from network problems, this implies caching the proxied artifacts. It also implies streaming proxied data back to the user as soon as possible, without waiting for the caching step to complete. If the user has to wait for AProx to cache an artifact before receiving any content, this actually penalizes them for using a repository manager.

#### Standard Features

##### Caching with flexible timeouts

Each remote repository (proxy) has a configurable timeout that determines how long AProx will hold cached content before getting rid of it. It also has a minimum cache timeout which is designed to hold onto an artifact at least as long as a modest build would take to execute...just in case its first and last modules need the same artifact. 

Accessing an artifact will reset the timeout for that file.

##### Marking failed proxy attempts (Not-Found cache)

Blindly pounding on remote repositories for content that has never existed there is not a very nice thing to do. To avoid this, AProx marks failed proxy attempts for each repository, and avoids re-checking for those artifacts for awhile. Each record in this not-found cache expires after awhile, allowing AProx to re-check just in case. You can also manually expire a not-found record from the UI.
 
##### Remote content listings

Face it, sometimes it's just easier to fire up the ol' browser and go look through a POM, take a look at the versions available for a project, or jog your memory for a particularly gnarly Maven groupId. In some repository managers, you only get to browse through the content that it has previously cached (and that hasn't expired yet). This can be pretty confusing, and it can mean that you still need to know the remote repository URLs so you can go browse them directly.

The better repository managers solve this dilemma for you by proxying directory listing content as well as the files themselves. AProx attempts to do this, and even caches the listing data (just like any artifact) to improve performance. Since it has to parse HTML to get the listing data from the remote server, it doesn't always produce a clean result, but it's usually a lot better than nothing.

#### Extra Features

##### Passthrough-style caching (minimal cache timeout)

Sometimes using a repository manager is more about the aggregation of content from multiple sources (grouping, which we'll cover in detail below) than it is about insulating against network problems. When working in this mode, content found via remote repositories in your groups will often change fairly quickly, and you expect up-to-date access to that content. In this situation, cache timeouts are actually a bad thing, something to be minimized. 

AProx solves this problem by setting a global minimum cache timeout that makes sense to support a build of modest size without the need to re-cache content. With that set you can simply check a box flagging your remote repository as a pass-through and AProx won't cache content from that repository any longer than the absolute minimum. If you need to, you can then tune the minimum cache timeout in your AProx configuration and affect all pass-through repositories at once.

##### Per-repository SSL settings

Have you ever tried using a Java application to access content on a server that's protected by a self-signed certificate? What about servers that require client SSL certificates? These are too often nightmarish scenarios requiring you to maintain a custom Java keystore and manage the keys manually. And they're unusual enough that searching for documentation lands you on some backwater wiki page that hasn't been updated since 2009.

Not so with AProx. Each repository configuration can store a client SSL certificate (and passphrase), and the SSL certificate of the server (for accommodating self-signed certificates). In theory, you could even have two separate remote repository definitions connecting to the same remote site, but using two different client SSL certificates. Though, it's hard to imagine why you would. But you could...

### Hosting Deployed Artifacts

If you produce software that isn't always available to the public, or you have a development process that includes continuous integration and you use snapshot builds, then you need someplace to park your artifacts. This place needs to be accessible to other Maven-ish builds (otherwise what's the point?) and ideally, it should be easy to push artifacts up to it with minimal configuration of your build tooling.

This is what hosted repositories were designed to handle. Hosted repositories normally allow build tools to use HTTP PUT to push new artifacts up into storage, and will serve them via a normal HTTP GET, just like any other type of repository. While there isn't a lot to distinguish one repository manager from another when it comes to hosting artifacts, it's still useful to talk about the "standard" form of hosting and the little extras that AProx provides.

#### Standard Features

##### Hosting release and snapshot artifacts

As mentioned above, repository managers are expected to host build artifacts, both releases with concrete versions and snapshots using the virtual \*-SNAPSHOT version. This means supporting HTTP PUT access to add new artifacts and HTTP GET access to retrieve artifacts (for example, during a build).

It also means maintaining accurate artifact metadata, such as the list of versions deployed to that hosted repository and the list of uniquely-versioned snapshot artifacts stored within a \*-SNAPSHOT directory. Maven (and other tools) require this metadata whenever they have to resolve version ranges or other ambiguous scenarios during a build.

##### Storage timeout for snapshots

When storing snapshot artifacts, storage often becomes a problem. It's tempting to run CI systems almost, well, continously when doing active development. If projects depend on one another, you need to use a repository manager to host the CI output so other CI jobs can depend on updated artifacts. Over time, snapshot directories bloat severely with obsolete artifacts.

To address this, most repository managers provide at least one way to cull obsolete snapshot artifacts. The most basic uses a snapshot timeout, which removes the snapshot after some period expires. Accessing the artifact resets this timeout.

#### Extra Features

##### Combined release / snapshot storage in a single repository

When doing large-scale deployment of a typical repository manager - especially in a CI environment - you will often find that you're setting up the same three repositories over and over again:

* One for hosting releases in the CI group
* One for hosting snapshots in the CI group
* One repository group to tie the hosted repositories together with public repository proxies and provide a single URL for resolving artifacts

AProx simplifies this a bit by allowing you to configure hosted repositories to store release artifacts, snapshot artifacts, or **both**. It's not enough to completely eliminate this tedium (check out the [Autoprox add-on](autoprox.html) to completely eliminate this tedium), but in these environments every little bit can help.

##### Flexible storage locations (directories)

Perhaps you have filled your disk with proxied remote artifacts. Perhaps you need a RAID array to host your new hotness and make sure nothing happens to it. Perhaps you've got a networked filesystem and a ton of AProx instances around the globe that all share the same hosted artifacts. Perhaps for *some* of your hosted repositories, you want to make sure their content is as safe as safe can be, for the next 10 years or more...while for others, their content could go up in a puff of smoke tomorrow without prompting much more than a slight shrug.

There are any number of reasons why you might need to specify where you store hosted artifacts. With AProx, you can configure each hosted repository to store its artifacts on a different disk if you want to.

### Grouping Repositories Together

Most Java developers have encountered that situation when you can't find some artifact you've been reading about and decided to use. You've never had a reason to deviate from the Maven central repository, but for some reason this one artifact just isn't hosted there. What to do?

Using plain vanilla Maven, you could add a repository entry to your settings.xml...but then you have to share your settings.xml with your whole development team. You could also add it to your project (and every other project you develop...or a parent project descriptor).

Or, if you have a repository manager, you can define a new remote repository proxy and add it to the repository group you use for development. A big attraction of this approach is that you de-clutter your settings.xml and pom.xml files. In the case of pom.xml files, you allow them to be source-agnostic, which makes them far less sensitive to changes in remote repository URLs over the long haul (remember that these remote servers are normally outside of your control). Another attraction is that it allows a team to manage the composition of the repository group and another team to focus on developing code.

Another nice thing about repository groups is their ability to partition repository proxies and allow users to reference and manage them abstractly.

All good Maven-ish repository managers offer repository groups, and for the most part they offer approximately the same repository-group features.

#### Standard Features

##### Ordered membership

When you request content from a repository group, what happens if a matching artifact exists on two of the member repositories?

If it's an actual artifact file (not a metadata file), the grouping logic should proceed through the membership repositories in order until it finds a match...which it then provides to the user. This means you can affect how artifacts are resolved (and sometimes, how quickly they are resolved) by tuning the membership of your repository group.

##### Metadata and index merging

When you request a metadata file from a repository group (such as the list of available versions for a project), what happens if multiple member repositories contain matching files?

In the case of metadata (not actual artifact content), the content is retrieved from all member repositories with matching files...and merged together. The merged file is then cached (with a timeout) in a storage location for the group, in order to avoid the cost of retrieving and merging the remote content for each request. This means the most relevant result (selected by the user's tooling) can be retrieved from the appropriate member repository seamlessly.

##### Recursive grouping

As mentioned above, repository groups are also handy ways to partition sets of repositories and reference them as aggregate units within the repository manager itself. For example, a build environment might allow two kinds of builds: cleanroom and open. The open builds can resolve artifacts from the Maven central repository and a number of other remote repositories, along with anything that's hosted locally. However, the cleanroom builds should only reference hosted artifacts.

One easy way to handle this situation is to define a repository group that holds all the hosted repositories as members (call it `cleanroom`), and another group which holds all the remote repositories (traditionally called `public`). You can then define another repository group called `open` and include both `cleanroom` and `public` as members. Now you have an easy place to add new remote repositories (`public`) and another easy place to add new hosted repositories (`cleanroom`). Artifacts available from the `open` group will be adjusted automatically.

You can then point cleanroom builds to use the `cleanroom` repository group URL, and open builds to use the `open` group URL.

#### Extra Features

##### Merged (remote) directory listings

Just as AProx provides a means of browsing the content of remote repositories, it also provides merged directory listings for repository groups. These merged listings include remote content that hasn't been cached yet along with content that it hosts or has cached. Additionally, hovering over any entry in a directory listing will provide information about what specific repositories in the group contains that listing entry. In the case of directories, this information could list multiple member repositories.

### A Little Something Extra

In addition to the features described above, AProx offers some convenient features that are common across all artifact store types (hosted and remote repositories, and groups). 

One of these is that any store can contain a text description. This seems trivial, but sometimes in large deployments repository names can become a bit cryptic. A text description allows you to escape the terseness of a repository name and describe what each repository is actually used for. The UI even allows you to search this description field, along with the URLs (the local URL, and in the case of a remote proxy, the remote URL) and repository name.

Another nice feature common to all store types is revision storage. All data files that contain AProx state[^1] are stored in a Git repository, and each change is committed as a new revision of the affected file. This is why the UI prompts you to enter a changelog entry whenever you make a change, and the Java client API's methods for manipulating artifact stores all take a parameter for the changelog entry. While this is still a nascent feature of AProx, it lays the groundwork for much more sophisticated revision review and management (such as rolling back to previous states). More information about Git data versioning can be found in 

### Notes

[^1]: AProx state is distinct from configuration files that determine what ports AProx uses and things like that, and from artifact caches which store the artifacts themselves.