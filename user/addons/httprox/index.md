---
title: "HTTProx Add-On"
---

### What About Those Other Files?

It's one thing to have a repository manager that knows how to proxy artifacts from here and everywhere. Maybe it can do backflips effortlessly and provide a "just works" experience for its users. Maybe it generates `settings.xml` files to make it even simpler to use with Apache Maven, and can even automatically create repositories based on some rules you define.

But what about all the other files you might need to download during a build?

At the root, artifact repository managers are file proxies with some special rules and handling added in. These special extras are what make repository managers better than something generic like `squid`. Likewise, the HTTProx add-on for Indy attempts to bring the concepts that have been successful in artifact repository management to bear on the problems of generic content proxying. It provides:

* Proxying of content and error states (or proxy-ish interpretation of errors, where that's more appropriate)
* Auto-creation of proxy caches based on target host
* Content access tracking

#### Proxy and Cache

Sometimes you just need a license notice file hosted by some third party, or a Checkstyle configuration. If you're interested in insulating your builds from the vagaries of internet connectivity and the inevitable drift of online services as time passes, you'll want a cache of all external content that your build references...not just the artifacts.

HTTProx provides this sort of caching for generic content (not just artifacts) by repurposing the repository storage logic in Indy without the artifact- and repository-specific logic on top. It listens on a separate port (8081 by default) so it can have access to raw HTTP messages in order to retrieve the original target URL accurately. When a new request is received, HTTProx will search the available Indy remote repository definitions for one referencing the base-URL (the requested URL without any path or query information). Then, it looks for an existing remote repository with a httprox-mangled form of the requested host and port. If it finds one, it will reuse that definition for the request. Otherwise, HTTProx will create a new remote repository pointing at the base URL of the requested target, and use that.

Once it has retrieved (or defined) the appropriate remote repository definition, it uses Indy infrastructure to request the content or metadata for target path. This allows Indy to follow its normal process for caching the content (and metadata) and firing the appropriate events to the rest of the system. Caching for these repositories is defaulted to never expire, though this can be managed after-the-fact via normal Indy client APIs or the UI.

When all of this setup is done, the original request is passed through Indy almost as if it were a request for an artifact, and the response is passed back to the user. If the upstream server returns an error, that error will be interpreted appropriately by HTTProx as it's conveyed back to the user. This provides the user with the ability to distinguish errors taking place in HTTProx itself (500 status) from those happening upstream (usually a 502 error).

##### NOTE:

In the future, HTTProx should follow the same approach used by AutoProx (see [Resources](#resources), below), where a set of Groovy rule files are used to create repository definitions. This will allow more flexibility in how the repositories are configured.

#### ...And Content Tracking

Going beyond the basics, sometimes it's nice to have a record of exactly what content your build downloads. Similar to the Folo add-on (see [Resources](#resources), below), HTTProx can track content accesses by using a user-specified tracking key to group accesses within a single report. By reusing Folo's infrastructure, these content-access records are integrated with the normal artifact content-access records that Folo tracks natively.

Unlike Folo, HTTProx doesn't have the luxury of separating tracked requests from untracked ones by the use of a different URL. Instead, it uses one of three options for the `tracking` configuration variable:

* **always** - Always use the *unaltered* authenticated proxy user as the tracking id to track content accesses. Challenge for authentication if it's not provided.
* **never** - Never track accesses.
* **suffix** - (*default*) If the proxy user is provided, check that it ends with `+tracking`. If not, don't track content accesses. Only challenge for authentication if the `secured` configuration option is `true` (it's `false` by default)

When content access is tracked, you can use the normal Folo Admin client APIs and URLs to view the tracking report after the fact.

### Client APIs

Currently there are no client APIs specifically created for accessing HTTProx features.

<a name="resources"></a>

### Resources

* [Folo documentation](folo-addon.html)
* [AutoProx documentation](autoprox-addon.html)
