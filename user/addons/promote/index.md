---
title: "Promote Content-Relocation Add-On"
---

### Content Promotion Fundamentals

In general terms, promotion really just consists of two things:

* validating new content to ensure its compatibility with the target repository
* adjusting access to content

Content promotion works in one of two modes: promotion via group membership (i.e. adding the repository with the content into the group's membership), or promotion based on paths (copying/moving content physically). In both scenarios, the proposed promotion can be validated using a flexible framework of rules and rule-sets, which are maintained as Groovy and JSON files, respectively. If content validation fails, the failing rule(s) can provide feedback to the user indicating why it failed.

In the documentation below, you'll see sections annotated with `[PATH-BASED]` and `[GROUP-BASED]`. This denotes the promotion mode covered in that section.

### Features

Specifically, the Promote add-on can:

* Promote a source repository into a target group
* Promote all content in a repository, or cherry pick specific paths
* Purge (or not) path-based content from the source repository after promotion to a target repository (not for group-based promotion)
* Rollback promotion, using the promotion result as the rollback request
* Resume a partial, failed promotion attempt (again, using the failed promotion result as the resume request)
* Perform a "dry run" that calculates the promotion details (including validation) but doesn't execute it
* Validate promotable content prior to actually promoting it

### CAUTION: You May Be About to Delete Content. Continue? (Y/n) [PATH-BASED]

When you move content from one repository to another (path-based promotion), it's possible you might be removing content from a repository that people are depending on to support their build systems. Removing an artifact that is already referenced from a project build can result in unpredictable results, with builds succeeding on some machines and failing on others. Modern build tools tend to cache resolved artifacts locally so they don't incur extra bandwidth and performance penalties by continually re-downloading the same files again and again. 

Imagine that a member of some development team resolves one of your artifacts as a dependency during a project build. Then, at some point after this initial build that referenced your artifact, the artifact is removed from the repository as part of a promotion (or rollback!) request you execute. Now, any subsequent attempts to build this project on a machine that hasn't previously built it will fail, since the machine doesn't have your artifact cached locally and the artifact no longer exists in the expected repository. The only way to reset to a consistent state is to remove the local artifact cache on all systems, after which the build will fail everywhere consistently.

This cautionary tale illustrates why you can't really pull artifacts back once they're deployed to a public-facing repository. In practice, this rule has a little bit of wiggle room; you can shift content from one repository to another, **if**:

* you don't expose the individual repositories (they're only exposed via a repository group)
* both the source and target repositories are in **all** of the same repository groups

But generally, you have to be **very** careful anytime you start thinking about removing an artifact from any repository.

### Promoting Content

The Promote add-on currently has no web UI, or plans to create one. If you have an idea and would like to take a crack at it, feel free to submit a pull request!

This leaves the Java client API and the REST API. In both cases, the user interacts with the add-on by submitting a serialized configuration (DTO for Java, JSON for REST), and receiving a result containing a list of successfully promoted files, a list of pending files, and potentially an error message (along with the original request configuration). In the case of the `resume` and `rollback` operations, the configuration submitted is actually a result object from a previous promotion call (or a configuration constructed to look like a previous result).

#### Getting Started: Apache Maven

If you use Apache Maven, you'll need the following dependencies in order to use this add-on:

    <!-- The core of the Indy client API -->
    <dependency>
      <groupId>org.commonjava.indy</groupId>
      <artifactId>indy-client-core-java</artifactId>
      <version>${indyVersion}</version>
    </dependency>
    <!-- Indy client API module for promote -->
    <dependency>
      <groupId>org.commonjava.indy</groupId>
      <artifactId>indy-promote-client-java</artifactId>
      <version>${indyVersion}</version>
    </dependency>

#### Promote a single file [PATH-BASED]

In Java:

    Indy client = new Indy("http://localhost:8080/api", 
                             new IndyPromoteClientModule() );
    
    String path = "org/commonjava/commonjava/4/commonjava-4.pom";
    StoreKey sourceRepo = new StoreKey( StoreType.hosted, "my-build" );
    StoreKey targetRepo = new StoreKey( StoreType.hosted, "releases" );
    
    PathsPromoteResult result = client.module(IndyPromoteClientModule.class)
          .promoteByPath( new PathsPromoteRequest( sourceRepo, targetRepo, path ) );

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
               http://localhost:8080/api/promotion/paths/promote
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
          "validations": {
            "valid": true,
            "ruleSet": "the-gauntlet.json"
          },
          "completedPaths" : [
              "org/commonjava/commonjava/4/commonjava-4.pom"
          ]
        }

#### ...Or, a whole repository [PATH-BASED]

To promote all the content in a given repository, simply leave out the paths when creating your promotion request configuration.

In Java:

    Indy client = new Indy("http://localhost:8080/api", 
                             new IndyPromoteClientModule() );
    
    //String path = "org/commonjava/commonjava/4/commonjava-4.pom";
    StoreKey sourceRepo = new StoreKey( StoreType.hosted, "my-build" );
    StoreKey targetRepo = new StoreKey( StoreType.hosted, "releases" );
    
    PathsPromoteResult result = client.module(IndyPromoteClientModule.class)
          .promoteByPath( new PathsPromoteRequest( sourceRepo, 
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
               http://localhost:8080/api/promotion/paths/promote
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
          "validations": {
            "valid": true,
            "ruleSet": "the-gauntlet.json"
          },
          "completedPaths" : [
              "org/commonjava/commonjava/4/commonjava-4.pom"
          ]
        }

#### Promoting a repository to a group [GROUP-BASED]

To promote a given repository to a target group, you use a slightly different request.

In Java:

    Indy client = new Indy("http://localhost:8080/api", 
                             new IndyPromoteClientModule() );
    
    //String path = "org/commonjava/commonjava/4/commonjava-4.pom";
    StoreKey sourceRepo = new StoreKey( StoreType.hosted, "my-build" );
    String targetGroup = "releases";
    
    GroupPromoteResult result = client.module(IndyPromoteClientModule.class)
          .promoteToGroup( new GroupPromoteRequest( sourceRepo, 
                                        targetGroup ) );

Pure REST:

1. Create a JSON file containing your promotion configuration (let's call it `promote.json`):

        {
          "source" : "hosted:my-build",
          "targetGroupName" : "releases",
        }

2. Then, POST your configuration:

        $ curl -i \
               --data @promote.json \
               -X POST \
               -H 'Content-Type: application/json' \
               http://localhost:8080/api/promotion/groups/promote
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
          "validations": {
            "valid": true,
            "ruleSet": "the-gauntlet.json"
          }
        }



#### Purging the source repository [PATH-BASED]

Purging the source repository (removing paths that haven been promoted) is a simple step, too.

In Java, use the fluent PromoteRequest API and call the `setPurgeSource()` method:

    PromoteResult result = client.module(IndyPromoteClientModule.class)
          .promote( new PromoteRequest( sourceRepo, targetRepo )
                            .setPurgeSource( true ) );

In REST, alter your promotion configuration:

    {
      "source" : "hosted:my-build",
      "target" : "hosted:releases",
      "purgeSource" : true
    }

### Test the waters first with a dry run

It's possible to calculate the validity and effects (for path-based promotion) of a promotion request without actually changing anything. This is called performing a dry run. If any errors are likely to occur with your promotion request, performing a dry run first should expose them.

#### Path-Based Dry Run

Changing your request to perform a dry run and calculate the changes your promotion will generate is a simple option (see below). The result will contain any errors (exceptions or validation errors), an empty or non-existent `completedPaths` listing, and a `pendingPaths` listing containing all the paths that would have been promoted. It will look exactly the same as if you requested promotion to a repository where the promotion failed for all paths, except there is no `error` field.

In Java, use the fluent PathsPromoteRequest API and call the `setDryRun()` method:

    PathsPromoteResult result = client.module(IndyPromoteClientModule.class)
          .promote( new GroupPromoteRequest( sourceRepo, "releases" )
                            .setDryRun( true ) );

In REST, alter your promotion configuration:

    {
      "source" : "hosted:my-build",
      "targetGroupName" : "releases",
      "dryRun" : true
    }

#### Group-Based Dry Run

The dry run behavior is much the same for group-based requests as it is for path-based requests, except (obviously) no completed or pending paths are returned. If the promotion request would result in one or more validation errors, those are returned in the result. If it would result in an exception being thrown, that will turn up in the result.

In Java, use the fluent GroupPromoteRequest API and call the `setDryRun()` method:

    GroupPromoteResult result = client.module(IndyPromoteClientModule.class)
          .promoteToGroup( new PathsPromoteRequest( sourceRepo, targetRepo )
                            .setDryRun( true ) );

In REST, alter your promotion configuration:

    {
      "source" : "hosted:my-build",
      "target" : "hosted:releases",
      "dryRun" : true
    }

### What if Something Goes Wrong? (Resuming) [PATH-BASED]

It's possible that a content set you try to promote won't be appropriate for your target repository. For instance, if your source repository allows a mixture of release- and snapshot-versioned artifacts, but your target repository is configured to only store releases, promotion of your entire source repository will result in errors if the promotion process encounters a snapshot artifact. If your intention is to allow these snapshots to be promoted, then it's a fairly straightforward task to modify your target repository to allow snapshots as well as releases. But where does that leave the promotion process you started?

Promotion requests are not transactional, so the result you receive will give two listings: one of successfully promoted paths, and another of pending paths. If you encounter a situation like that described above, you can simply resume your promotion request after fixing the target repository. Assuming you saved the promotion result from your first attempt, you can re-post the result to resume the promotion:

Java:

    Indy client = new Indy("http://localhost:8080/api", 
                             new IndyPromoteClientModule() );
    
    PathsPromoteResult result = // from previous promotion request...
    
    PathsPromoteResult secondResult = client.module(IndyPromoteClientModule.class)
          .resumePathPromote( result );
    
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
      "validations": {
        "valid": false,
        "ruleSet": "the-gauntlet.json"
        "validatorErrors": {
          "you-shall-not-pass.groovy": "I am a servant of the Secret Fire, wielder of the Flame of Anor. The dark fire will not avail you, Flame of Udun!"
        }
      },
      "completedPaths" : [
          "org/commonjava/commonjava/4/commonjava-4.pom"
      ]
    }

The only differences between this resume request and the original promote request are the use of the previous output (result) as input to resume, and the use of the `resume` endpoint in place of the original `promote` endpoint. The response will be another promotion result, hopefully this time containing only `completedPaths` and no remaining `pendingPaths`.

#### You can also "resume" a path-based dry run...

Since executing a promotion request with the `dryRun` flag set results in a promotion response with all the would-be promoted paths still in the `pendingPaths` listing, you can save this response and reuse it with the `resume` endpoint to actually run the promotion. This would allow you to preview the work to be done, then simply resubmit it for actual execution.

### Um, I Changed My Mind... (Rollback)

It's always possible that a promotion request just can't be completed. Maybe it's not appropriate to reconfigure the target repository to accept the failed content; or maybe the disk where the target repository is full. No matter what the reason, you can rollback the promoted content to its original location (and maybe try something else). In practice, the request for `rollback` works almost the same way as the `resume` request.

#### Path-Based Rollback

When performing a path-based rollback, it's often helpful to have the original promotion result on hand. This allows you to only process the files that were actually copied over:

Java:

    Indy client = new Indy("http://localhost:8080/api", 
                             new IndyPromoteClientModule() );
    
    PathsPromoteResult result = // from previous promotion request...
    
    PathsPromoteResult secondResult = client.module(IndyPromoteClientModule.class)
          .rollbackPathPromote( result );
    
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

#### Group-Based Rollback

With a group-based rollback, you can either use the result from the previous call, or you can construct one on the spot. It makes little difference, since the promotion process only uses the source and target store keys:

Java:

    Indy client = new Indy("http://localhost:8080/api", 
                             new IndyPromoteClientModule() );
    
    GroupPromoteResult result;

    result = // from previous promotion request...

    // Or, construct from whole cloth...
    StoreKey sourceRepo = new StoreKey( StoreType.hosted, "my-build" );
    String targetGroup = "releases";
    result = new GroupPromoteResult( new GroupPromoteRequest( sourceRepo, targetGroup ) );
    
    // anyhow, just call the rollback!
    GroupPromoteResult secondResult = client.module(IndyPromoteClientModule.class)
          .rollbackGroupPromote( result );
    
REST:

(Assuming you saved your previous promotion result to a file called `result.json`):

    $ curl -i \
            --data @result.json \
            -X POST \
            -H 'Content-Type: application/json' \
            http://localhost:8080/api/promotion/groups/rollback
    HTTP/1.1 200 OK
    Connection: keep-alive
    Set-Cookie: JSESSIONID=v5HSyMPf_fphqf1CMH2CGRrv; path=/
    Content-Type: application/json
    Content-Length: 189
    Date: Tue, 05 May 2015 19:15:24 GMT
    
    {
      "request" : {
        "source" : "hosted:my-build",
        "targetGroupName" : "releases",
      },
      "validations": {
        "valid": false,
        "ruleSet": "the-gauntlet.json"
        "validatorErrors": {
          "you-shall-not-pass.groovy": "I am a servant of the Secret Fire, wielder of the Flame of Anor. The dark fire will not avail you, Flame of Udun!"
        }
      }
    }

### Validating Content Promotions

In the examples above, you may have noticed some validation errors in the promotion results. Each time a promotion request is processed, the promotion manager looks for a validation rule-set that matches the target repository (or group) key. If it finds one, it goes through the rule list contained within that set, and executes each rule to validate the request. Each validation rule has the opportunity to append an error to the validation result (keyed by the rule name).

But how do you write rules and rule-sets?

#### Rule Sets are Defined in JSON Files

A validation rule set is just a collection of references to validation rule scripts (by filename only; any path fragments will be flattened to filenames), associated with a rule-set name and store-key pattern. When a promotion request is received, the target store's key is matched against each rule-set's `storeKeyPattern` (using `StoreKey.toString()` to render it to the `type:name` format first). The first rule-set that matches is used to validate the request.

The matching rule-set's list of rule references is then iterated. Each rule is looked up (from the validations manager) and executed against the request.

A simple example for a rule set might be:

    {
      "storeKeyPattern": ".*",
      "ruleNames": [
        "you-shall-not-pass.groovy"
      ],
      "validationParameters": {
        "scope": "runtime"
      }
    }

One extra piece you may notice above is the `validationParameters` map. This is an optional mapping of generic key-value string pairs that can be used by rules in the set to validate the request. This allows validation rules to be parameterized, making them a bit more generic and (hopefully) reusable. In this specific example, we might expect the `you-shall-not-pass.groovy` rule to verify all `runtime` scoped dependencies declared in POMs to be promoted are available in the promotion target. 

#### Rules are Groovy Scripts

A validation rule is just a Groovy script dropped in your `${indy.home}/var/lib/indy/data/promote/rules` directory. Your script should define a class implementing the `ValidationRule` interfacewhich contists of a single method:

    String validate( ValidationRequest request ) throws Exception;

If the `validate()` method returns `null`, it is considered to have passed validation. Anything non-null return value will be mapped to the rule's name (the name of the groovy script file) in the `validatorErrors` field embedded in `validations` within the promotion result.

#### I'll Just Need Some Tools...

Any non-trivial validation rule will probably need some access to Indy infrastructure in order to do its job. Maybe it needs to access existing content in the target repository (perhaps in order to validate that runtime dependencies are available?), or maybe it needs to verify that some *other* repository group that contains the target as its constituent doesn't already contain the promotable content.

For these purposes, the validation framework exposes selected operations from ContentManager (retrieval of existing content) and StoreDataManager (retrieval of existing store definitions) to validation rules for their use.

