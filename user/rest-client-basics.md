---
title: "Basics: The REST Client API"
---

### Overview

AProx conducts all interactions with its clients via its REST API. This even includes its web UI, which is just a Javascript application served by AProx that knows how to use the REST API behind the scenes.

One of the biggest advantages of this is that it's relatively simple to write new client APIs. Whether you're using Java, Python, or even `cURL`, it's not too hard to make use of the AProx REST API and the JSON data that flows through it. Anyone who wants to implement a new client API immediately has access to all the features that any other client can use.

Also, it allows us to concentrate our implementation and testing efforts on a single API, which makes that API more stable and complete.

### Getting Started

To get started using the AProx REST API, you don't really need anything more than `cURL` or another HTTP client library. It helps to have a JSON parser, but it's not strictly required (you could use `grep` and `sed` in a pinch).

AProx's REST API resides under the `/api` sub-path on a given AProx instance. For example, if we wanted to get some basic information about the version of AProx we're using, we could simply do this:

    $ curl -i http://localhost:8080/api/stats/version-info
    HTTP/1.1 200 OK
    Connection: keep-alive
    Set-Cookie: JSESSIONID=d0qWUoWOtEo-wjIU2i4rArSD; path=/
    Content-Type: application/json
    Content-Length: 129
    Date: Fri, 08 May 2015 20:30:08 GMT
    
    {
      "version" : "0.20.1-SNAPSHOT",
      "builder" : "jdcasey",
      "commit-id" : "4ff6a63",
      "timestamp" : "2015-05-05 14:11 -0500"
    }

#### REST Verbs

In general, AProx tries to adhere to the typical REST principles for how the different HTTP method should be used:

* **POST** - Sent to resource base, NOT the named resource itself, this creates a new resource from the body content and returns 201 status on success along with the created entity in JSON and appropriate Location-type header.
* **PUT** - Modify an existing resource using body content, returning 200 status on success
* **GET** - Read a resource, returning 200 status on success
* **HEAD** - Retrieve basic information about the resource, returning 200 status if the resource exists. Also returns headers with meta-information such as last-modified, length, content-type, etc. for a path in an artifact store.
* **DELETE** - Delete the named resource, returning 204 No Content status on success

The main divergence from these is in the artifact-store content resource, where the PUT verb is be used to create new files. This is supported for compatibility reasons, since Apache Maven uses PUT to deploy artifacts to remote repositories.

### AProx Core Endpoints

AProx's core functionality supports CRUD++ operations (augmenting basic CRUD with things like listing and existence) for artifact stores and store content. It also supports some inspection operations, such as returning version and build information for the AProx instance. More specifically, the core supports the following three categories of operations:

* [Stores](rest/stores.html) - Access and manage artifact store (repository and group) definitions.
* [Content](#content) - Access and manage artifact content (files) within stores on the system.
* [Stats](#stats) - Access information about the AProx server version and build info, along with information about the add-ons that are available.

### Further Reading

Each AProx add-on can expose as many of its own REST API operations as makes sense, in addition to those of the AProx core. Documentation for these add-ons will detail the corresponding REST API operations they provide.


