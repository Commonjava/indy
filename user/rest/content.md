---
title: "REST API: Content Management"
---

### Contents

* [Existence/Metadata](#exist)
* [Creation](#create)
* [Retrieval](#retrieve)
* [Deletion](#delete)
* [Group Metadata-File Handling](#group)

<a name="exist"></a>

### Check existence and get metadata about a file

Just like most webservers, if you issue a **HEAD** request for a given path within an artifact store, it will return 200 for a file that exists, or 404 for one that does not. And, like most webservers, if the file exists, the response will also include headers indicating content length, type, and last modification date.

Here's a sample exchange for a missing file:

    $ curl --head \
        http://localhost:8080/api/remote/central/org/foo/bar/2/bar-2.txt
    HTTP/1.1 404 Not Found
    Connection: keep-alive
    Set-Cookie: JSESSIONID=17nPeinZK-f7a8TgtCJqt5eG; path=/
    Content-Type: text/plain
    Content-Length: 0
    Date: Mon, 11 May 2015 16:23:56 GMT

And here's a sample exchange for a file that's available:

    $ curl --head \
        http://localhost:8080/api/remote/central/org/commonjava/commonjava/4/commonjava-4.pom
    HTTP/1.1 200 OK
    Connection: keep-alive
    Last-Modified: Mon, 11 May 2015 11:25:29 CDT
    Set-Cookie: JSESSIONID=ECRMqALGnBaQXNTW3gUQk0JT; path=/
    Content-Length: 11780
    Content-Type: application/octet-stream
    Date: Mon, 11 May 2015 16:25:29 GMT

**NOTE:** The `Last-Modified` header in the above response is incorrect. This is actually a problem with metadata being discarded for proxied files, and is tracked [here](https://github.com/Commonjava/galley/issues/15). As a result, the system actually reports the date the content was proxied, rather than the true last-modified date from the upstream server.

<a name="create"></a>

### Creating content

Storing content (deploying artifacts) is a critical function of any repository manager. Since it's tuned to be compatible with Apache Maven, AProx makes a departure from normal REST design in this function.

Maven expects to use HTTP **PUT** requests to send content to the server, where most REST services use a **POST** request to the resource base URL in order to create a new resource. For the content function **only**, AProx embraces the idiosyncracies of Maven over REST. So, to upload a new file to AProx, you'd use something like this:

    $ curl -i -X PUT --data @foo-1.pom \
       http://localhost:8080/api/hosted/local-deployments/org/foo/1/foo-1.pom
    HTTP/1.1 201 Created
    Connection: keep-alive
    Set-Cookie: JSESSIONID=W1AlmCF6hTaA4nUM6eMALEbm; path=/
    Location: http://localhost:8080/api/hosted/local-deployments/org/foo/foo-1.pom
    Content-Length: 0
    Date: Mon, 11 May 2015 16:52:02 GMT

<a name="retrieve"></a>

### Retrieving content

For files (and artifacts), retrieval is just what you'd expect. A simple **GET** request streams the content back to you:

    $ curl -i \
        http://localhost:8080/api/remote/central/org/commonjava/commonjava/4/commonjava-4.pom
    HTTP/1.1 200 OK
    Connection: keep-alive
    Last-Modified: Mon, 11 May 2015 11:31:38 CDT
    Set-Cookie: JSESSIONID=ENu6anqG0XaBrF7yzE-RQTS1; path=/
    Content-Type: application/octet-stream
    Content-Length: 11780
    Date: Mon, 11 May 2015 19:45:34 GMT

    <project>
      <modelVersion>4.0.0</modelVersion>

      <groupId>org.commonjava</groupId>
      <artifactId>commonjava</artifactId>
      <version>4</version>
      <packaging>pom</packaging>

      <name>CommonJava Top-Level Parent POM</name>
      [...]

For directories, it's a little more complicated. When you **GET** a directory path, AProx will *try* to generate a directory listing and return it. *Try* is a critical word here; see below for details.

If you set the `Accept` header to `application/json` you'll get JSON back:

    $ curl -i -H 'Accept: application/json' \
        http://localhost:8080/api/remote/central/org/commonjava/commonjava/
    HTTP/1.1 200 OK
    Connection: keep-alive
    Set-Cookie: JSESSIONID=IA2DCL_o9OGo3CfbYwdd5402; path=/
    Content-Type: application/json
    Content-Length: 885
    Date: Mon, 11 May 2015 19:48:46 GMT

    {
      "items" : [ {
        "key" : "remote:central",
        "path" : "org/commonjava/commonjava/1/"
      }, {
        "key" : "remote:central",
        "path" : "org/commonjava/commonjava/2/"
      }, {
        "key" : "remote:central",
        "path" : "org/commonjava/commonjava/3/"
      }, {
        "key" : "remote:central",
        "path" : "org/commonjava/commonjava/4/"
      }
      [...]
    }

On the other hand, if you leave off the `Accept` header, AProx will generate a HTML directory listing and send it back:

    $ curl -i \
        http://localhost:8080/api/remote/central/org/commonjava/commonjava/
    HTTP/1.1 200 OK
    Connection: keep-alive
    Set-Cookie: JSESSIONID=Nns0O3jK_kRWXqYMD2dP97o8; path=/
    Content-Type: text/html
    Content-Length: 3096
    Date: Mon, 11 May 2015 19:52:24 GMT

    <html>
      <head>
        <title>AProx: Directory listing for org/commonjava on central</title>
        <style media="screen" type="text/css">
          h2{
            color: #333;
          }
          .item-listing{
            list-style: none outside;
          }
          footer{
            border-top: 1px solid #777;
            font-size: small;
          }
        </style>
      </head>
      <body>
        <h2>Directory listing for org/commonjava on central</h2>
        <ul class="item-listing">
        <li><a href="http://localhost:8080/api/remote/central/org/commonjava/">..</a></li>

          <li><a class="item-link" title='sources:
    http://repo1.maven.apache.org/maven2/org/commonjava/commonjava/1/' href='http://localhost:8080/api/remote/central/org/commonjava/commonjava/1/'>1/</a></li>
    [...]

#### A note about directory listing contents

Note that the directory listing contains files we haven't yet proxied. AProx basically screen-scrapes the generated directory listings from upstream remote repositories, and merges the items it comes up with into those it already knows about from its local cache. If you're requesting a directory listing from a repository group, it will perform all these steps for each member repository, then merge the individual repository listings before generating the output JSON (or HTML).

As I'm sure you've noticed, the weak point in all of this is the screen-scraping AProx does to come up with a listing of what content is available on the upstream repository. This depends heavily on that upstream server rendering directory listings in a fairly basic way, without additional links in the HTML. Any extra links usually wind up in the AProx directory listing, and can be a bit confusing to deal with. For this reason, it's not really recommended for you to depend on AProx directory listing information in an automated way. While you can retrieve the listing in JSON format, that JSON could very well include junk links that originated as page decoration on the remote server. Likewise, if the upstream repository doesn't allow directory browsing, you may not see any results at all.

Unfortunately, there just isn't a standard way to render this type of information in the Maven repository format.

<a name="delete"></a>

### Deleting content

Content deletion works about like you'd expect. You send a **DELETE** request, and AProx returns `204 No Content` signifying the content no longer exists on the server. In the case of a remote repository, sending a delete request **only removes the file from the proxy cache**, not from the upstream remote repository.

For example:

    $ curl -i -X DELETE \
        http://localhost:8080/api/remote/central/org/commonjava/commonjava/4/commonjava-4.pom
    HTTP/1.1 204 No Content
    Connection: keep-alive
    Set-Cookie: JSESSIONID=yXfxjd8vBaQtxIbd7TNpxylY; path=/
    Content-Length: 0
    Date: Mon, 11 May 2015 20:15:41 GMT

Also, be aware that if you send a **DELETE** request for a directory path, that whole directory will be removed.
