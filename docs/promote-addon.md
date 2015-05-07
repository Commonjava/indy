---
title: "Promote Content-Relocation Add-On"
---

### Content Promotion Fundamentals

In general terms, promotion really just consists of two things:

* moving content from one repository to another
* applying validation tests beforehand to ensure the new content is compatible with the target repository

The Promote add-on is a fairly new addition to AProx. Its eventual aim is to provide a robust mechanism for promoting content from one location to another, complete with a validation rule framework. Currently, it acts more as a simple content relocation add-on, allowing users to move content from the storage of one hosted / remote repository to some hosted-repository target.

More specifically, the Promote add-on can:

* Promote all content in a repository, or cherry pick specific GAVs
* Purge (or not) content from the source repository after promotion to a target repository
* Rollback promotion, if the promotion result is passed back in
* Resume a partial, failed promotion attempt
* Perform a "dry run" that calculates the promotion details but doesn't execute it

### CAUTION: You May Be About to Delete Content. Continue? (Y/n)

When you move content from one repository to another, it's possible you might be removing content from a repository that people are depending on to support their build systems. Removing an artifact that is already referenced from a project build can result in unpredictable results, with builds succeeding on some machines and failing on others. Modern build tools tend to cache resolved artifacts locally so they don't incur extra bandwidth and performance penalties by continually re-downloading the same files again and again. 

Imagine that a member of some development team resolves one of your artifacts as a dependency during a project build. Then, at some point after this initial build that referenced your artifact, the artifact is removed from the repository as part of a promotion (or rollback!) request you execute. Now, any subsequent attempts to build this project on a machine that hasn't previously built it will fail, since the machine doesn't have your artifact cached locally and the artifact no longer exists in the expected repository. The only way to reset to a consistent state is to remove the local artifact cache on all systems, after which the build will fail everywhere consistently.

This cautionary tale illustrates why you can't really pull artifacts back once they're deployed to a public-facing repository. In practice, this rule has a little bit of wiggle room; you can shift content from one repository to another, **if**:

* you don't expose the individual repositories (they're only exposed via a repository group)
* both the source and target repositories are in **all** of the same repository groups

But generally, you have to be **very** careful anytime you start thinking about removing an artifact from any repository.

### Promoting Content

The Promote add-on currently has no web UI, or plans to create one. If you have an idea and would like to take a crack at it, feel free to submit a pull request!

This leaves the Java client API and the REST API. In both cases, the user interacts with the add-on by submitting a serialized configuration (DTO for Java, JSON for REST), and receiving a result containing a list of successfully promoted files, a list of pending files, and potentially an error message (along with the original request configuration). In the case of the `resume` and `rollback` operations, the configuration submitted is actually a result object from a previous promotion call (or a configuration constructed to look like a previous result).

#### Promote a single file

In Java:

    Aprox client = new Aprox("http://localhost:8080/api", 
                             new AproxPromoteClientModule() );
    
    String path = "org/commonjava/commonjava/4/commonjava-4.pom";
    StoreKey sourceRepo = new StoreKey( StoreType.hosted, "my-build" );
    StoreKey targetRepo = new StoreKey( StoreType.hosted, "releases" );
    
    PromoteResult result = client.module(AproxPromoteClientModule.class)
          .promote( new PromoteRequest( sourceRepo, targetRepo, path ) );

Pure REST:

1. Create a JSON file containing your promotion configuration (let's call it `promote.json`):

        {
          "source" : "hosted:my-build",
          "target" : "hosted:releases",
          "paths" : [ "org/commonjava/commonjava/4/commonjava-4.pom" ]
        }

2. Then, POST your configuration:

        $ curl -i \
               --data @promote.json \
               -X POST \
               -H 'Content-Type: application/json' \
               http://localhost:8080/api/promotion/promote
        HTTP/1.1 200 OK
        Connection: keep-alive
        Set-Cookie: JSESSIONID=v5HSyMPf_fphqf1CMH2CGRrv; path=/
        Content-Type: application/json
        Content-Length: 189
        Date: Tue, 05 May 2015 19:15:24 GMT
        
        {
          "request" : {
            "source" : "hosted:my-build",
            "target" : "hosted:releases",
            "paths"  : [ "org/commonjava/commonjava/4/commonjava-4.pom" ]
          },
          "completedPaths" : [
              "org/commonjava/commonjava/4/commonjava-4.pom"
          ]
        }

#### ...Or, a whole repository

To promote all the content in a given repository, simply leave out the paths when creating your promotion request configuration.

In Java:

    Aprox client = new Aprox("http://localhost:8080/api", 
                             new AproxPromoteClientModule() );
    
    //String path = "org/commonjava/commonjava/4/commonjava-4.pom";
    StoreKey sourceRepo = new StoreKey( StoreType.hosted, "my-build" );
    StoreKey targetRepo = new StoreKey( StoreType.hosted, "releases" );
    
    PromoteResult result = client.module(AproxPromoteClientModule.class)
          .promote( new PromoteRequest( sourceRepo, 
                                        targetRepo /*, path*/ ) );

Pure REST:

1. Create a JSON file containing your promotion configuration (let's call it `promote.json`):

        {
          "source" : "hosted:my-build",
          "target" : "hosted:releases",
        }

2. Then, POST your configuration:

        $ curl -i \
               --data @promote.json \
               -X POST \
               -H 'Content-Type: application/json' \
               http://localhost:8080/api/promotion/promote
        HTTP/1.1 200 OK
        Connection: keep-alive
        Set-Cookie: JSESSIONID=v5HSyMPf_fphqf1CMH2CGRrv; path=/
        Content-Type: application/json
        Content-Length: 189
        Date: Tue, 05 May 2015 19:15:24 GMT
        
        {
          "request" : {
            "source" : "hosted:my-build",
            "target" : "hosted:releases",
          },
          "completedPaths" : [
              "org/commonjava/commonjava/4/commonjava-4.pom"
          ]
        }

#### Purging the source repository

Purging the source repository (removing paths that haven been promoted) is a simple step, too.

In Java, use the fluent PromoteRequest API and call the `setPurgeSource()` method:

    PromoteResult result = client.module(AproxPromoteClientModule.class)
          .promote( new PromoteRequest( sourceRepo, targetRepo )
                            .setPurgeSource( true ) );

In REST, alter your promotion configuration:

    {
      "source" : "hosted:my-build",
      "target" : "hosted:releases",
      "purgeSource" : true
    }

#### Test the waters first with a dry run

Likewise, changing your request to perform a dry run and calculate the changes your promotion will generate is a simple option (see below). The result will contain an empty or non-existent `completedPaths` listing, and a `pendingPaths` listing containing all the paths that would have been promoted. It will look exactly the same as if you requested promotion to a repository where the promotion failed for all paths, except there is no `error` field.

In Java, use the fluent PromoteRequest API and call the `setPurgeSource()` method:

    PromoteResult result = client.module(AproxPromoteClientModule.class)
          .promote( new PromoteRequest( sourceRepo, targetRepo )
                            .setDryRun( true ) );

In REST, alter your promotion configuration:

    {
      "source" : "hosted:my-build",
      "target" : "hosted:releases",
      "dryRun" : true
    }

### What if Something Goes Wrong? (Resuming)

It's possible that a content set you try to promote won't be appropriate for your target repository. For instance, if your source repository allows a mixture of release- and snapshot-versioned artifacts, but your target repository is configured to only store releases, promotion of your entire source repository will result in errors if the promotion process encounters a snapshot artifact. If your intention is to allow these snapshots to be promoted, then it's a fairly straightforward task to modify your target repository to allow snapshots as well as releases. But where does that leave the promotion process you started?

Promotion requests are not transactional, so the result you receive will give two listings: one of successfully promoted paths, and another of pending paths. If you encounter a situation like that described above, you can simply resume your promotion request after fixing the target repository. Assuming you saved the promotion result from your first attempt, you can re-post the result to resume the promotion:

Java:

    Aprox client = new Aprox("http://localhost:8080/api", 
                             new AproxPromoteClientModule() );
    
    PromotionResult result = // from previous promotion request...
    
    PromoteResult secondResult = client.module(AproxPromoteClientModule.class)
          .resume( result );
    
REST:

(Assuming you saved your previous promotion result to a file called `result.json`):

    $ curl -i \
            --data @result.json \
            -X POST \
            -H 'Content-Type: application/json' \
            http://localhost:8080/api/promotion/resume
    HTTP/1.1 200 OK
    Connection: keep-alive
    Set-Cookie: JSESSIONID=v5HSyMPf_fphqf1CMH2CGRrv; path=/
    Content-Type: application/json
    Content-Length: 189
    Date: Tue, 05 May 2015 19:15:24 GMT
    
    {
      "request" : {
        "source" : "hosted:my-build",
        "target" : "hosted:releases",
      },
      "completedPaths" : [
          "org/commonjava/commonjava/4/commonjava-4.pom"
      ]
    }

The only differences between this resume request and the original promote request are the use of the previous output (result) as input to resume, and the use of the `resume` endpoint in place of the original `promote` endpoint. The response will be another promotion result, hopefully this time containing only `completedPaths` and no remaining `pendingPaths`.

#### You can also "resume" a dry run...

Since executing a promotion request with the `dryRun` flag set results in a promotion response with all the would-be promoted paths still in the `pendingPaths` listing, you can save this response and reuse it with the `resume` endpoint to actually run the promotion. This would allow you to preview the work to be done, then simply resubmit it for actual execution.

### Um, I Changed My Mind... (Rollback)

It's always possible that a promotion request just can't be completed. Maybe it's not appropriate to reconfigure the target repository to accept the failed content; or maybe the disk where the target repository is full. No matter what the reason, as long as you've kept the promotion result, you can rollback the promoted content to its original location (and maybe try something else). In practice, the request for `rollback` works almost the same way as the `resume` request.

Java:

    Aprox client = new Aprox("http://localhost:8080/api", 
                             new AproxPromoteClientModule() );
    
    PromotionResult result = // from previous promotion request...
    
    PromoteResult secondResult = client.module(AproxPromoteClientModule.class)
          .rollback( result );
    
REST:

(Assuming you saved your previous promotion result to a file called `result.json`):

    $ curl -i \
            --data @result.json \
            -X POST \
            -H 'Content-Type: application/json' \
            http://localhost:8080/api/promotion/rollback
    HTTP/1.1 200 OK
    Connection: keep-alive
    Set-Cookie: JSESSIONID=v5HSyMPf_fphqf1CMH2CGRrv; path=/
    Content-Type: application/json
    Content-Length: 189
    Date: Tue, 05 May 2015 19:15:24 GMT
    
    {
      "request" : {
        "source" : "hosted:my-build",
        "target" : "hosted:releases",
      },
      "pendingPaths" : [
          "org/commonjava/commonjava/4/commonjava-4.pom"
      ]
    }

### Planned Features

* [Support for pre-promotion validation tests](https://github.com/Commonjava/aprox/issues/127)
* [Support for group membership-style promotion](https://github.com/Commonjava/aprox/issues/126)
