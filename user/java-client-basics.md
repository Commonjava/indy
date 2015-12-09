---
title: "Basics: The Java Client API"
---

### Overview

Indy provides a Java client API that wraps Indy REST calls with a fluent API and a set of model objects (complete with serialization to and from JSON). This client API supports nearly all of the functions exposed by the underlying REST API, and grows more complete with each release.

### Getting Started

If you're using Apache Maven to build, you can add the client API to your project with a single dependency:

    <dependency>
      <groupId>org.commonjava.indy</groupId>
      <artifactId>indy-client-core-java</artifactId>
      <version>0.20.0</version>
    </dependency>

Once you have the dependency added to your project, you'll need to setup a new instance of `org.commonjava.indy.client.core.Indy` in order to talk to your Indy server. Something like this should get you started:

    Indy indy = new Indy( "http://localhost:8080/api/" ).connect();

Once you have that, read on for more information about using the core Indy client modules (and Indy client modules in general).

### Client Modules

Like Indy itself, the Java client API consists of a core of functionality, seen as fundamental to the operation of a repository manager, and a set of add-ons. Related sets of functions are implemented in modules within the client API, and the Indy core functionality is currently covered by three of these modules:

* [Stores](#stores) - Module containing CRUD + listing and existence methods for working with artifact store (repository and group) definitions
* [Content](#content) - Module containing CRUD + listing and existence methods for working with content within a given artifact store
* [Stats](#stats) - Module containing methods to access information about the Indy server version and build info, along with information about the add-ons that are available

Each of the above modules has its own convenience accessor method in the `org.commonjava.indy.client.core.Indy` class. 

### Stores Module
<a name="stores"></a>

To manage store definitions (repositories and groups), you can use the `IndyStoresClientModule` module via the `stores()` convenience method:

    // initialize Indy client
    Indy indy = new Indy("http://localhost:8080/api/")
                  .connect();
    
    StoreListingDTO<Group> groups = 
        indy.stores().listGroups();
    
    StoreListingDTO<HostedRepository> hosteds = 
        indy.stores().listHostedRepositories();
    
    StoreListingDTO<RemoteRepository> remotes = 
        indy.stores().listRemoteRepositories();
    
    Group pub = indy.stores().load(StoreType.group, 
                                    "public", 
                                    Group.class);
    
    Group group = new Group("test-group", 
                            new StoreKey(StoreType.remote, "central"));
    
    group = indy.stores().create(group,
                                  "Changelog for new group definition", 
                                  Group.class);
    
    boolean exists = 
        indy.stores().exists(StoreType.group, "test-group");
    
    group.addConstituent(new StoreKey(StoreType.hosted", 
                                      "local-deployments"));
    
    boolean success = indy.stores().update(group, 
                                            "Adding local hosted repo");
    
    indy.stores().delete(StoreType.group, group.getName())

### Content Module
<a name="content"></a>

To access or manage content available within an artifact store, you can use the `IndyContentClientModule` module via the `content()` convenience method:

    // initialize Indy client
    Indy indy = new Indy("http://localhost:8080/api/")
                  .connect();
    
    // for brevity later...
    String repo = "local-deployments";
    StoreType type = StoreType.hosted;
    String path = "org/foo/foo-bar/1/foo-bar-1.pom";
    
    // This is FALSE; we haven't deployed it
    boolean exists = indy.content().exists(type, repo, path);
    
    // This is NULL; the file doesn't exist yet.
    InputStream in = indy.content().get(type, repo, path);
    
    // Not a valid POM, obviously...
    in = new ByteArrayInputStream("<project/>".getBytes());
    
    indy.content().store(type, repo, path, in);
    IOUtils.closeQuietly(in);
    
    // This is now TRUE; we just deployed it
    exists = indy.content().exists(type, repo, path);
    
    PathInfo info = indy.content().getInfo(type, repo, path);
    System.out.printf("Path %s:\n  Exists? %s\n  Content-Type: %s\n" + 
                      "  Content-Length: %s\n  Last-Modified: %s\n", 
                      path, 
                      info.exists(), 
                      info.getContentType(), 
                      info.getContentLength(), 
                      info.getLastModified());
    
    in = indy.content().get(type, repo, path);
    String content = IOUtils.toString(in);
    IOUtils.closeQuietly(in);
    
    // This is "org/foo/foo-bar/1";
    String dir = new File(path).getParentPath();
    DirectoryListingDTO listing = 
        indy.content().listContents(type, repo, dir);
    
    // This is "foo-bar-1.pom"
    String versionDir = listing.getItems().get(0);
    
    // Delete just the file itself.
    indy.content().delete(type, repo, path);
    
    // Or, delete the whole directory.
    indy.content().delete(type, repo, dir);


### Stats Module
<a name="stats"></a>

Or, if you want very general information about Indy itself, you can use the `IndyStatsClientModule` via the `stats()` convenience method:

    // initialize Indy client
    Indy indy = new Indy("http://localhost:8080/api/")
                  .connect();
    
    // Get information about the running version of Indy.
    IndyVersioning info = indy.stats().getVersionInfo();
    System.out.printf("Indy, version %s\n  built by: %s at %s\n" + 
                      "  from commit: %s\n", 
                      info.getVersion(), 
                      info.getBuilder(), 
                      info.getTimestamp(), 
                      info.getCommitId() );
    
    AddOnListing addons = indy.stats().getActiveAddons();
    for(IndyAddOnID addon: addons.getItems()){
        System.out.println(addon.getName());
    }
    
    EndpointViewListing stores = indy.stats().getAllEndpoints();
    for(EndpointView store: stores){
        System.out.printf("Name: %s, Type: %s\n  URL: %s\n", 
                          store.getName(), 
                          store.getType(), 
                          store.getResourceUri());
    }

### Add-On Modules

Additionally, client modules for accessing the functions of various add-ons can be initialized when the Indy client is initialized, then accessed via the `module(..)` method:

    // initialize Indy client with the Folo content add-on module
    Indy indy = new Indy("http://localhost:8080/api/",
                             new IndyFoloContentClientModule())
                  .connect();
    
    // retrieve content using the Folo content module.
    InputStream stream = indy.module(IndyFoloContentClientModule.class)
         .get( "b01234", 
               StoreType.remote, 
               "central",
               "org/commonjava/commonjava/4/commonjava-4.pom" );

Yes, you could have used a separate field for the Folo content module, and used that (after having passed it into the Indy client to initialize it). However, using the `module(..)` method allows you to pass around the Indy client instance without having to manage the module instances separately.

### Further Reading

Each Indy add-on can expose as many of its own client API modules as makes sense, in addition to those of the Indy client's core API (the Folo content module above is one example). Documentation for these add-ons will detail the corresponding Java client API modules (along with pertinent REST API operations).

