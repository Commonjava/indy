---
---

### AProx (Approximately A Proxy)

AProx has its roots in the relatively simple task of managing proxies to remote Maven repositories, and the grouping logic necessary to aggregate them. However, its feature set has grown far beyond this initial purpose to make it simpler to work with Maven artifacts and repositories in a number of ways. This is no mere repository manager; it is a platform for managing access to Maven artifacts and the repositories that contain them.

Why a platform? In addition to its core functionality, AProx provides a framework and API for working with the artifacts it has access to, and that flow through it. It also has a Java client API for accessing these functions, both the core features and the add-ons. And in its development, we've gone to great lengths to make AProx accessible in a number of ways, ranging from embedding AProx in your application to continuous deployment in a Docker infrastructure (with the help of its sister project [aprox-docker](/aprox-docker/)).

### Our Feature Selections

Some of the more interesting features provided by AProx are:

* Hosted repositories with configurable / combined release and snapshot storage
* Remote repositories with individually configurable mutual SSL and server SSL certificate
* Logical repository grouping with content merging for known file types that require it
* `settings.xml` file generation tuned to work with each repository and group, with local repository separation
* WebDAV access to cached / stored repository content **and** generated settings.xml files
* Repository / Group definition change tracking with changelogs, stored in Git
* Ability to synchronize all repository / group definition changes to a remote Git repository
