---
title: "Delete a workspace"
---

### Examples

Deleting a workspace stored in the system is straightforward:

#### Using the Java Client API

    module.deleteWorkspace("my-workspace");

#### Using the REST API

    $ curl -i \
           -X DELETE \
           http://localhost:8080/api/depgraph/ws/my-workspace
    HTTP/1.1 204 No Content

