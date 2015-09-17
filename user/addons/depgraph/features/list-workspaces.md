---
title: "Listing workspaces"
---

### BUG: AProx versions <= 0.24.0

Versions of AProx up to and including 0.24.0 have a bug in the Depgraph add-on. If you have not saved your first workspace (by discovering a dependency graph) and you try to list the workspaces on the system, it will throw a `NullPointerException`. The issue is tracked in [AProx #223](https://github.com/Commonjava/aprox/issues/223).

### Examples

Listing the workspaces stored in the system is straightforward:

#### Using the Java Client API

    WorkspaceList listing = module.listWorkspaces();
    for( String wsid: listing.getWorkspaces() ){
    	System.out.printf( "Got workspace: %s\n", wsid );
    }

#### Using the REST API

    $ curl -i \
           -H 'Accept: application/json' \
           http://localhost:8080/api/depgraph/ws
    HTTP/1.1 200 OK
    Connection: keep-alive
	Transfer-Encoding: chunked
	Content-Type: application/json
	Date: Thu, 17 Sep 2015 21:37:38 GMT

    {
    	"workspaces": [
    	    "my-workspace"
    	]
    }

