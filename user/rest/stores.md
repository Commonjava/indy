---
title: "REST API: Store Management"
---

### Contents

* [Existence](#exist)
* [Retrieval](#retrieve)
* [Creation](#create)
* [Modification](#modify)
* [Listing](#list)
* [Deletion](#delete)

<a name="exist"></a>

### Check existence of a store

To check if a particular artifact store exists, use a **HEAD** request. If the store exists, you'll see a 200 response, like this:

    $ curl -I http://localhost:8080/api/admin/group/public
    HTTP/1.1 200 OK
    Connection: keep-alive
    Set-Cookie: JSESSIONID=v2ti22kOcoEHoxygT_piEvD9; path=/
    Content-Length: 0
    Date: Fri, 08 May 2015 21:02:44 GMT

If the store is missing, you get a 404:

    $ curl -I http://localhost:8080/api/admin/group/foo
    HTTP/1.1 404 Not Found
    Connection: keep-alive
    Set-Cookie: JSESSIONID=lhgiIjNYJUyT_nbaLE33OCAm; path=/
    Content-Length: 0
    Date: Fri, 08 May 2015 21:00:31 GMT

<a name="retrieve"></a>

### Retrieve a store definition

For a group:

    $ curl -i http://localhost:8080/api/admin/group/public
    HTTP/1.1 200 OK
    Connection: keep-alive
    Set-Cookie: JSESSIONID=e2EcgUP3ObYbrVBJ975pSif6; path=/
    Content-Type: application/json
    Content-Length: 133
    Date: Fri, 08 May 2015 21:04:22 GMT
    
    {
      "type" : "group",
      "key" : "group:public",
      "constituents" : [ "remote:central" ],
      "doctype" : "group",
      "name" : "public"
    }

For a hosted repository:

    $ curl -i http://localhost:8080/api/admin/hosted/local-deployments
    HTTP/1.1 200 OK
    Connection: keep-alive
    Set-Cookie: JSESSIONID=x20rfNMjMD9S1mwvVCVxa6Xv; path=/
    Content-Type: application/json
    Content-Length: 208
    Date: Fri, 08 May 2015 21:05:32 GMT
    
    {
      "type" : "hosted",
      "key" : "hosted:local-deployments",
      "snapshotTimeoutSeconds" : 86400,
      "doctype" : "hosted",
      "name" : "local-deployments",
      "allow_snapshots" : true,
      "allow_releases" : true
    }

For a remote repository:

    $ curl -i http://localhost:8080/api/admin/remote/central
    HTTP/1.1 200 OK
    Connection: keep-alive
    Set-Cookie: JSESSIONID=fIVEkDQYu9iKWSg-5esUQzuN; path=/
    Content-Type: application/json
    Content-Length: 333
    Date: Fri, 08 May 2015 21:06:19 GMT
    
    {
      "type" : "remote",
      "key" : "remote:central",
      "host" : "repo1.maven.apache.org",
      "port" : 80,
      "doctype" : "remote",
      "name" : "central",
      "url" : "http://repo1.maven.apache.org/maven2/",
      "timeout_seconds" : 0,
      "nfc_timeout_seconds" : 0,
      "is_passthrough" : false,
      "cache_timeout_seconds" : 0,
      "proxy_port" : 0
    }

<a name="create"></a>

### Create a new store

* Start with the JSON, possibly in a file (ours is `store.json`):

        {
          "type" : "group",
          "key" : "group:foo",
          "constituents" : [ "remote:central" ],
          "doctype" : "group",
          "name" : "foo"
        }

* Then, issue a **POST** request to the store-resource base URL. To make this work, we need to send the JSON file as the request body, **and** we need to set the `Content-Type` header appropriately:

        $ curl -i -X POST --data @store.json \
            -H 'Content-Type: application/json' \
            http://localhost:8080/api/admin/group
        HTTP/1.1 201 Created
        Connection: keep-alive
        Set-Cookie: JSESSIONID=p0df2STULzR-9tTAGdbeBvdz; path=/
        Location: http://localhost:8080/api/admin/group/foo
        Content-Type: application/json
        Content-Length: 127
        Date: Fri, 08 May 2015 21:17:55 GMT
        
        {
          "type" : "group",
          "key" : "group:foo",
          "constituents" : [ "remote:central" ],
          "doctype" : "group",
          "name" : "foo"
        }

As you can see, the response status is 201 to signal the creation of a new resource, and the headers contain `Location`, which tells you what URL to use to retrieve this new group's definition.

<a name="modify"></a>

### Modify an existing store

Now, let's remove the central repository constituent from the group we created above.

* First, modify the `store.json` file. Since I'm lazy, I'll simply remove the `constituents` property altogether:

        {
          "type" : "group",
          "key" : "group:foo",
          "doctype" : "group",
          "name" : "foo"
        }

* Then, we issue a **PUT** request to the store URL for this group. Again, we send the JSON file as the request body, and set the `Content-Type` header appropriately:

        $ curl -i -X PUT --data @store.json \
            -H 'Content-Type: application/json' \
            http://localhost:8080/api/admin/group/foo
        HTTP/1.1 200 OK
        Connection: keep-alive
        Set-Cookie: JSESSIONID=PWX4GzbK86lwSBZ90ct9ARwS; path=/
        Content-Length: 0
        Date: Fri, 08 May 2015 21:25:40 GMT

This time, the response status is 200, and there isn't a lot of extra information beyond that.

<a name="list"></a>

### Get a listing of available stores (for a given store type)

To get a listing of all groups:

    $ curl -i http://localhost:8080/api/admin/group
    HTTP/1.1 200 OK
    Connection: keep-alive
    Set-Cookie: JSESSIONID=fWOnFHvDnEfNXyURH18cCndq; path=/
    Content-Type: application/json
    Content-Length: 263
    Date: Fri, 08 May 2015 21:34:01 GMT
    
    {
      "items" : [ {
        "type" : "group",
        "key" : "group:foo",
        "doctype" : "group",
        "name" : "foo"
      }, {
        "type" : "group",
        "key" : "group:public",
        "constituents" : [ "remote:central" ],
        "doctype" : "group",
        "name" : "public"
      } ]
    }

<a name="delete"></a>

### Delete a store

To delete a group:

    $ curl -i -X DELETE http://localhost:8080/api/admin/group/foo
    HTTP/1.1 204 No Content
    Connection: keep-alive
    Set-Cookie: JSESSIONID=O2HVR6nl3MDJJJ1k2xV17wTE; path=/
    Content-Length: 0
    Date: Fri, 08 May 2015 21:32:11 GMT

The 204 status signals that either the group was deleted, or it didn't exist in the first place. If you wanted to know which is the case, you would start by issuing a **HEAD** request to the group to see if it exists.

