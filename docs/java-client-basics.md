---
title: "Basics: The Java Client API"
---

### Overview

AProx provides a Java client API that wraps AProx REST calls with a fluent API and a set of model objects (complete with serialization to and from JSON). This client API supports nearly all of the functions exposed by the underlying REST API, and grows more complete with each release.

### Getting Started

If you're using Apache Maven to build, you can add the client API to your project with a single dependency:

    <dependency>
      <groupId>org.commonjava.aprox</groupId>
      <artifactId>aprox-client-core-java</artifactId>
      <version>0.20.0</version>
    </dependency>

Once you have the dependency added to your project, you'll need to setup a new instance of `org.commonjava.aprox.client.core.Aprox` in order to talk to your AProx server. Something like this should get you started:

    Aprox aprox = new Aprox( "http://localhost:8080/api/" ).connect();

Once you have that, read on for more information about using the core AProx client modules (and AProx client modules in general).

### Client Modules

Like AProx itself, the Java client API consists of a core of functionality, seen as fundamental to the operation of a repository manager, and a set of add-ons. Related sets of functions are implemented in modules within the client API, and the AProx core functionality is currently covered by three of these modules:

* [Stores](#stores) - Module containing CRUD + listing and existence methods for working with artifact store (repository and group) definitions
* [Content](#content) - Module containing CRUD + listing and existence methods for working with content within a given artifact store
* [Stats](#stats) - Module containing methods to access information about the AProx server version and build info, along with information about the add-ons that are available

Each of the above modules has its own convenience accessor method in the `org.commonjava.aprox.client.core.Aprox` class. 

#### Stores Module

To manage store definitions (repositories and groups), you can use the `AproxStoresClientModule` module via the `stores()` convenience method:

    // initialize AProx client
    Aprox aprox = new Aprox("http://localhost:8080/api/")
                  .connect();
    
    StoreListingDTO<Group> groups = 
        aprox.stores().listGroups();
    
    StoreListingDTO<HostedRepository> hosteds = 
        aprox.stores().listHostedRepositories();
    
    StoreListingDTO<RemoteRepository> remotes = 
        aprox.stores().listRemoteRepositories();
    
    Group pub = aprox.stores().load(StoreType.group, 
                                    "public", 
                                    Group.class);
    
    Group group = new Group("test-group", 
                            new StoreKey(StoreType.remote, "central"));
    
    group = aprox.stores().create(group,
                                  "Changelog for new group definition", 
                                  Group.class);
    
    boolean exists = 
        aprox.stores().exists(StoreType.group, "test-group");
    
    group.addConstituent(new StoreKey(StoreType.hosted", 
                                      "local-deployments"));
    
    boolean success = aprox.stores().update(group, 
                                            "Adding local hosted repo");
    
    aprox.stores().delete(StoreType.group, group.getName())

#### Content Module

To access or manage content available within an artifact store, you can use the `AproxContentClientModule` module via the `content()` convenience method:

    // initialize AProx client
    Aprox aprox = new Aprox("http://localhost:8080/api/")
                  .connect();
    
    // for brevity later...
    String repo = "local-deployments";
    StoreType type = StoreType.hosted;
    String path = "org/foo/foo-bar/1/foo-bar-1.pom";
    
    // This is FALSE; we haven't deployed it
    boolean exists = aprox.content().exists(type, repo, path);
    
    // This is NULL; the file doesn't exist yet.
    InputStream in = aprox.content().get(type, repo, path);
    
    // Not a valid POM, obviously...
    in = new ByteArrayInputStream("<project/>".getBytes());
    
    aprox.content().store(type, repo, path, in);
    IOUtils.closeQuietly(in);
    
    // This is now TRUE; we just deployed it
    exists = aprox.content().exists(type, repo, path);
    
    PathInfo info = aprox.content().getInfo(type, repo, path);
    System.out.printf("Path %s:\n  Exists? %s\n  Content-Type: %s\n" + 
                      "  Content-Length: %s\n  Last-Modified: %s\n", 
                      path, 
                      info.exists(), 
                      info.getContentType(), 
                      info.getContentLength(), 
                      info.getLastModified());
    
    in = aprox.content().get(type, repo, path);
    String content = IOUtils.toString(in);
    IOUtils.closeQuietly(in);
    
    // This is "org/foo/foo-bar/1";
    String dir = new File(path).getParentPath();
    DirectoryListingDTO listing = 
        aprox.content().listContents(type, repo, dir);
    
    // This is "foo-bar-1.pom"
    String versionDir = listing.getItems().get(0);
    
    // Delete just the file itself.
    aprox.content().delete(type, repo, path);
    
    // Or, delete the whole directory.
    aprox.content().delete(type, repo, dir);


#### Stats Module

Or, if you want very general information about AProx itself, you can use the `AproxStatsClientModule` via the `stats()` convenience method:

    // initialize AProx client
    Aprox aprox = new Aprox("http://localhost:8080/api/")
                  .connect();
    
    // Get information about the running version of AProx.
    AproxVersioning info = aprox.stats().getVersionInfo();
    System.out.printf("AProx, version %s\n  built by: %s at %s\n" + 
                      "  from commit: %s\n", 
                      info.getVersion(), 
                      info.getBuilder(), 
                      info.getTimestamp(), 
                      info.getCommitId() );
    
    AddOnListing addons = aprox.stats().getActiveAddons();
    for(AproxAddOnID addon: addons.getItems()){
        System.out.println(addon.getName());
    }
    
    EndpointViewListing stores = aprox.stats().getAllEndpoints();
    for(EndpointView store: stores){
        System.out.printf("Name: %s, Type: %s\n  URL: %s\n", 
                          store.getName(), 
                          store.getType(), 
                          store.getResourceUri());
    }

#### Add-On Modules

Additionally, client modules for accessing the functions of various add-ons can be initialized when the Aprox client is initialized, then accessed via the `module(..)` method:

    // initialize AProx client with the Folo content add-on module
    Aprox aprox = new Aprox("http://localhost:8080/api/",
                             new AproxFoloContentClientModule())
                  .connect();
    
    // retrieve content using the Folo content module.
    InputStream stream = aprox.module(AproxFoloContentClientModule.class)
         .get( "b01234", 
               StoreType.remote, 
               "central",
               "org/commonjava/commonjava/4/commonjava-4.pom" );

Yes, you could have used a separate field for the Folo content module, and used that (after having passed it into the Aprox client to initialize it). However, using the `module(..)` method allows you to pass around the Aprox client instance without having to manage the module instances separately.



