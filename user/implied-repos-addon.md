---
title: "Implied Repositories Add-On"
---

### Repository Managers' Fatal Flaw

Repository managers have a lot of good points. They can insulate you from slow or unreliable networks with their storage caches. Using repository groups, you can blend content from multiple remote sites seamlessly behind a single URL and predict how the content will be served. And repository managers provide centralized management for your artifact sources, to help standardize your team's environment.

But what happens when you depend on a project that declares some random repository in its own POM? If you're like many repository manager users, you've created (or [generated](dot-maven-addon.html)) a `settings.xml` that uses the `<mirror/>` section to route all requests through your repo manager. This sets up a situation where Maven can't modify the list of repositories it uses to resolve artifacts (they're declared/grouped behind that single, convenient mirror URL on the repository manager)...but it still **thinks** it can. So, Maven tries to add the new (random) repository for your dependency dynamicall during the resolution process. Then, it promptly trips over its own MirrorSelector component, which re-routes the newly added repository to your repository manager instead.

And your build fails.

### A Better Way

The key point to remember in this scenario is that your repository manager is serving up the dependency POM that declares the new repository. This means your repository manager sees the POM before Maven does, and could use its contents to trigger some sort of event...

This is where the Implied Repositories add-on comes into play. Each file that gets cached in AProx triggers an event, which add-ons can listen for. In this case, for each POM stored, Implied Repositories parses it and looks for any repository declarations it might contain. If it finds one, creates a new remote repository in AProx for it. Implied Repositories then tags the "source" repository (where the POM came from) with a piece of metadata noting that it now "implies" the repository declared in the POM. This way, any time the "source" repository is added to the membership of an AProx group, any implied repositories are also added.

### Growing Pains

This add-on is new, so there are still some kinks to be worked out. One of the most important questions is how to automatically remove an implied repository from a group when the repository that implied it is removed.

So, while Implied Repositories will be included in releases of the AProx Savant distribution flavor for any version beyond 0.20.0, it will not be enabled by default.

#### Enabling Implied Repositories

To enable this feature in your Savant deployment, add the following configuration:

`etc/aprox/conf.d/implied-repos.conf`:

    [implied-repos]
    enabled=true

