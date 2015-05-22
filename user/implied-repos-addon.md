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

### Java Client API

The Implied Repositories add-on provides a very small client API module, `ImpliedRepoClientModule`. This module is simply responsible for managing the repository implication metadata attached to store definitions. As such, it uses the AproxStoresClientModule to load and update store definitions, and merely reads metadata objects from existing store definitions. You'll probably need to use it in conjunction with the AproxStoresClientModule, available via the `Aprox.stores()` convenience method.

Using the Implied Repositories client module, you can retrieve and even modify the list of implied repositories for a given "source" repository.

#### Getting Started: Apache Maven

If you use Apache Maven, you'll need the following dependencies in order to use this add-on:

    <!-- The core of the AProx client API -->
    <dependency>
      <groupId>org.commonjava.aprox</groupId>
      <artifactId>aprox-client-core-java</artifactId>
      <version>${aproxVersion}</version>
    </dependency>
    <!-- AProx client API module for implied-repos -->
    <dependency>
      <groupId>org.commonjava.aprox</groupId>
      <artifactId>aprox-implied-repos-client-java</artifactId>
      <version>${aproxVersion}</version>
    </dependency>

#### Get Repositories Implied by This One

To retrieve the list of stores (repositories and groups) that are known to be implied by the given repository, you would use something like the following:

    Aprox client = new Aprox( "http://localhost:8080/api",
                              new ImpliedRepoClientModule() );
    
    // StoreKey is an aggregation of type and name.
    List<StoreKey> impliedKeys = 
        client.module( ImpliedRepoClientModule.class )
              .getStoresImpliedBy( StoreType.remote, "central" );

**NOTE:** These results will be repositories that were declared by POMs in the `central` repository.

#### Get Repositories that Imply This One

To retrieve the list of stores (repositories and groups) that are known to imply the given repository, you would use something like the following:

    Aprox client = new Aprox( "http://localhost:8080/api",
                              new ImpliedRepoClientModule() );
    
    // StoreKey is an aggregation of type and name.
    List<StoreKey> impliedKeys = 
        client.module( ImpliedRepoClientModule.class )
              .getStoresImplying( StoreType.remote, "my-repo" );

**NOTE:** These results will be repositories in which POMs were detected that declared the `my-repo` repository.

#### Set Repositories Implied by This One

If you want to forcibly setup an implication relationship between stores, you can use something like the following:

    Aprox client = new Aprox( "http://localhost:8080/api",
                              new ImpliedRepoClientModule() );
    
    RemoteRepository central = client.stores().load(
        StoreType.remote, "central", RemoteRepository.class
    );
    
    List<StoreKey> implications = new ArrayList<>();
    implications.add( new StoreKey( StoreType.remote, "my-repo" ) );
    
    // StoreKey is an aggregation of type and name.
    client.module( ImpliedRepoClientModule.class )
          .setStoresImpliedBy( central,
                               implications,
                               "This is the changelog for git" );

**NOTE:** This will bypass the POM parsing and repository detection that normally happens in the Implied Repositories add-on. Be careful!

### REST API

The Implied Repositories add-on currently doesn't have a proper REST API, since its functionality is implemented through event listeners on the server side, and its state is maintained as metadata on the store definitions themselves.



