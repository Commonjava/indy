---
title: "Listing Included GAVs"
---

### A Basic Example

Listing the GAVs in a graph, without a pre-existing graph:

#### Using the Java Client API

    ProjectGraphRequest req = module.newProjectGraphRequest()
        .withWorkspaceId("my-workspace")
        .withGraph( module.newGraphDescription()
          .withRoots( 
                new SimpleProjectVersionRef( 
                    "commons-codec", 
                    "commons-codec", 
                    "1.7"
                ),
                new SimpleProjectVersionRef(
                    "commons-io",
                    "commons-io",
                    "2.4"
                )
          )
          .withPreset("runtime")
          .build()
        )
        .withResolve(true)
        .withSource("group:public")
        .build();

    ProjectListResult result = module.list( req );
    for( ProjectVersionRef project: result.getProjects() ){
        System.out.println( "Got GAV: " + project );
    }

#### Using the REST API

First, create a JSON file to contain the request configuration. We'll call this `request.json`:

    $ cat request.json
    {
        "workspaceId": "my-workspace",
        "resolve": true,
        "graph": {
            "roots": [
                "commons-codec:commons-codec:1.7", 
                "commons-io:commons-io:2.4"
            ],
            "preset": "runtime"
        },
        "source": "group:public"
    }

    $ curl -i \
           -X POST \
           --data @request.json \
           -H 'Content-Type: application/json' \
           -H 'Accept: application/json' \
           http://localhost:8080/api/depgraph/project/list
    HTTP/1.1 200 OK
    [...]

    {
    	"projects": [
          "commons-codec:commons-codec:1.7",
          "commons-io:commons-io:2.4"
    	]
    }

### Matching a Project Pattern

It's also possible to specify a GAV regex that can be used to narrow the results returned. In the example below, we'll constrain our results to those matching `commons-io:.*`

#### Using the Java Client API

    ProjectGraphRequest req = module.newProjectGraphRequest()
        .withWorkspaceId("my-workspace")
        .withGraph( module.newGraphDescription()
          .withRoots( 
                new SimpleProjectVersionRef( 
                    "commons-codec", 
                    "commons-codec", 
                    "1.7"
                ),
                new SimpleProjectVersionRef(
                    "commons-io",
                    "commons-io",
                    "2.4"
                )
          )
          .withPreset("runtime")
          .build()
        )
        .withResolve(true)
        .withSource("group:public")
        .withProjectGavPattern("commons-io:.*")
        .build();

    ProjectListResult result = module.list( req );
    for( ProjectVersionRef project: result.getProjects() ){
        System.out.println( "Got GAV: " + project );
    }

#### Using the REST API

First, create a JSON file to contain the request configuration. We'll call this `request.json`:

    $ cat request.json
    {
        "workspaceId": "my-workspace",
        "resolve": true,
        "graph": {
            "roots": [
                "commons-codec:commons-codec:1.7", 
                "commons-io:commons-io:2.4"
            ],
            "preset": "runtime"
        },
        "source": "group:public",
        "projectGavPattern": "commons-io:.*"
    }

    $ curl -i \
           -X POST \
           --data @request.json \
           -H 'Content-Type: application/json' \
           -H 'Accept: application/json' \
           http://localhost:8080/api/depgraph/project/list
    HTTP/1.1 200 OK
    [...]

    {
        "projects": [
          "commons-io:commons-io:2.4"
        ]
    }
