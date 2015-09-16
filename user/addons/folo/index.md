---
title: "Folo Content-Tracking Add-On"
---

### Can I Have a Receipt for That, Please?

Sometimes it's nice to have a list of all the artifacts you used during a build. The manifest of files you downloaded is useful for anything from creating an offline repository for your project's build, to auditing your project's dependencies in some sort of database for future reference.

Sure, you can capture the build log and parse all the `Downloading...` lines to get at a similar list of paths. But if you're using a repository manager, it can be hard to know where those artifacts originated. Also, if you push content up to the repository manager ("deploy" in Maven parlance), that's a whole different set of console log lines to parse. And if you're deploying to a repository manager group (which is possible in AProx), you might not exactly know where the artifacts were stored. Add to this the complication that certain artifacts could be downloaded during the build without any console logging. I mean, it's not like the console log has a set of requirements for its format.

So, you want to get a full accounting of artifacts transferred to and from your AProx instance during a build, preferably without having to depend on console logging to give you everything. How can you do that?

### Identify and Track Your Build

The Folo (pronounced "follow") add-on allows you to assign an arbitrary identifier to your build, and track the content it uses and produces. After the build completes, you can download a report of the content usage, complete with origin repository information for downloaded content (the remote or hosted repository it came from) and target repository information for uploaded content (the hosted repository in which it was stored).

Content access through Folo URLs acts exactly as it would normally, except each access is logged to a report that's kept for the given identifier. 

#### Choose (Your Identifier) Wisely

How you choose the tracking identifier is up to you, which means its uniqueness is also up to you to maintain. Also, Folo has no way of knowing when your build is done, so if you expect to reuse a tracking identifier, you will have to manage the content reports between builds to avoid appending to an earlier report.

If you start two builds with the same 'foo-123' tracking identifier, Folo will aggregate the content access records of the two builds. 

If you start two successive builds with the same 'foo-123' tracking identifier, Folo will aggregate the content access records for the two builds.

### Almost the Same URL...

To use Folo tracking, you would change something like this:

    /api/remote/central/org/commonjava/commonjava/4/commonjava-4.pom

to this:

    /api/folo/track/foo-123/remote/central/org/commonjava/commonjava/4/commonjava-4.pom

...and proceed normally.

### Retrieving Your Report

After the build completes, you can either retrieve the content report via the REST API or the Java client API.

#### Getting Started: Apache Maven

If you use Apache Maven, you'll need the following dependencies in order to use this add-on via the Java client API:

    <!-- The core of the AProx client API -->
    <dependency>
      <groupId>org.commonjava.aprox</groupId>
      <artifactId>aprox-client-core-java</artifactId>
      <version>${aproxVersion}</version>
    </dependency>
    <!-- AProx client API module for folo -->
    <dependency>
      <groupId>org.commonjava.aprox</groupId>
      <artifactId>aprox-folo-client-java</artifactId>
      <version>${aproxVersion}</version>
    </dependency>

#### REST API

As an example, let's simulate a build by retrieving the `org.commonjava:commonjava:4` POM using curl:

    $ curl http://localhost:8080/api/folo/track/foo-123/remote/central/org/commonjava/commonjava/4/commonjava-4.pom
    [...]

We can also simulate an artifact deployment:

    $ curl -X PUT --data 'This is a test' \
        http://localhost:8080/api/folo/track/foo-123/hosted/local-deployments/org/commonjava/commonjava/101/commonjava-101.pom
    HTTP/1.1 201 Created
    Connection: keep-alive
    Set-Cookie: JSESSIONID=c5UiQJu_Py2SIlEf87eThKcK; path=/
    Location: http://localhost:8080/api/folo/track/foo-123/hosted/local-deployments/org/commonjava/commonjava/101/commonjava-101.pom
    Content-Length: 0
    Date: Tue, 05 May 2015 22:00:51 GMT

Now, we can retrieve a report of our activity using the `/api/folo/admin/foo-123/report` path:

    $ curl http://localhost:8080/api/folo/admin/foo-123/report
    {
      "key" : {
        "id" : "foo-123"
      },
      "uploads" : [ {
        "storeKey" : "hosted:local-deployments",
        "path" : "/org/commonjava/commonjava/101/commonjava-101.pom",
        "localUrl" : "http://localhost:8080/api/hosted/local-deployments/org/commonjava/commonjava/101/commonjava-101.pom",
        "md5" : "ce114e4501d2f4e2dcea3e17b546f339",
        "sha256" : "c7be1ed902fb8dd4d48997c6452f5d7e509fbcdbe2808b16bcf4edce4c07d14e"
      } ],
      "downloads" : [ {
        "storeKey" : "remote:central",
        "path" : "/org/commonjava/commonjava/4/commonjava-4.pom",
        "originUrl" : "http://repo1.maven.apache.org/maven2/org/commonjava/commonjava/4/commonjava-4.pom",
        "localUrl" : "http://localhost:8080/api/remote/central/org/commonjava/commonjava/4/commonjava-4.pom",
        "md5" : "8bc43da33f5817e5e76e5518c5360166",
        "sha256" : "7dfaab31a1f4c0ebe090a637c09e1c33f12ce3368f762a13125c811874ce31c1"
      } ]
    }

Once we're done with the report, we can remove it with a simple DELETE call to the `record` sub-resource:

    $ curl -i -X DELETE \
        http://localhost:8080/api/folo/admin/foo-123/record
    HTTP/1.1 204 No Content
    Connection: keep-alive
    Set-Cookie: JSESSIONID=nD4eZ3fr0Xiv1BuYav_XQsKC; path=/
    Content-Length: 0
    Date: Tue, 05 May 2015 22:00:10 GMT


#### Java Client API

Now, let's try the same example using Java:

    final String trackingId = "foo-123";
    
    final String downloadPath = "/org/commonjava/commonjava/4/" + 
        "commonjava-4.pom";

    final String uploadPath = "/org/commonjava/aprox/aprox-parent/" + 
        "0.20.0/aprox-parent-0.20.0.pom";
    
    AproxFoloContentClientModule content = new AproxFoloContentClientModule();
    AproxFoloAdminClientModule admin = new AproxFoloAdminClientModule();
    Aprox aprox = new Aprox( "http://localhost:8080/api/", content, admin );
    
    InputStream stream = content.get( 
        trackingId, 
        StoreType.remote, 
        "central", 
        downloadPath );

    IOUtils.toString( stream );
    IOUtils.closeQuietly( stream );
    
    stream = new ByteArrayInputStream( ( "This is a test" ).getBytes() );
    content.store( 
        trackingId, 
        StoreType.hosted, 
        "local-deployments", 
        uploadPath, 
        stream );
    
    final TrackedContentDTO report = 
        admin.getTrackingReport( trackingId );
    
    assertThat( report, notNullValue() );
    
    Set<TrackedContentEntryDTO> downloads = report.getDownloads();
    
    assertThat( downloads, notNullValue() );
    assertThat( downloads.size(), equalTo( 1 ) );
    
    TrackedContentEntryDTO entry = downloads.iterator()
                    .next();
    
    System.out.println( entry );
    
    final Set<TrackedContentEntryDTO> uploads = report.getUploads();
    
    assertThat( uploads, notNullValue() );
    assertThat( uploads.size(), equalTo( 1 ) );
    
    entry = uploads.iterator().next();
    
    System.out.println( entry );
    
    admin.clearTrackingRecord( trackingId );
    
    IOUtils.closeQuietly( aprox );

### Web UI

The Folo add-on does not currently have any web UI visibility, but one is [planned](https://github.com/Commonjava/aprox/issues/124).