---
title: "REST API: Indy Statistic Functions"
---

### Contents

**NOTE:** These functions are more about the features of a given Indy instance, not about content statistics. The corresponding URL base-path may change in the future to better reflect this (and avoid confusion in the context of a possible future statistics add-on). If this happens, the url path `/api/stats` will likely be deprecated in favor of two new, clearer paths (one for these functions, one for the statistics add-on).

* [Version Info](#version)
* [Add-Ons](#addon)

<a name="version"></a>

### Version Information

Indy is designed to respond to a **GET** request on `/api/stats/version-info` with its version, timestamp it was built, user-id of the person who built it, and the commit-id from which it was built. In the web UI, this information is used to render links for reporting issues, browsing code, etc.

However, it also makes a handy way to monitor Indy to ensure it is responding to requests. The response you receive will look like this:

    $ curl -i http://localhost:8080/api/stats/version-info
    HTTP/1.1 200 OK
    Connection: keep-alive
    Set-Cookie: JSESSIONID=HjrdWj1mGwRzOFrIl0jUCrkt; path=/
    Content-Type: application/json
    Content-Length: 129
    Date: Mon, 11 May 2015 20:37:15 GMT

    {
      "version" : "0.20.1-SNAPSHOT",
      "builder" : "jdcasey",
      "commit-id" : "321a6c9",
      "timestamp" : "2015-05-11 15:13 -0500"
    }

<a name="addon"></a>

### Add-On Information

Information about which add-ons are available is provided via **GET** request, under the `/api/stats/addons/active` URL path. This endpoint is designed to give enough information to populate a web UI navigational menu, so a lot of its information is likely to be relatively academic for non-web users:

    $ curl -i http://localhost:8080/api/stats/addons/active
    HTTP/1.1 200 OK
    Connection: keep-alive
    Set-Cookie: JSESSIONID=oErZzUB3xqFEW9-ABulwBPRL; path=/
    Content-Type: application/json
    Content-Length: 1284
    Date: Mon, 11 May 2015 20:40:09 GMT

    {
      "items" : [ {
        "name" : "AutoProx",
        "routes" : [ {
          "route" : "/autoprox/calc",
          "templateHref" : "ui-addons/autoprox/partials/calc.html"
        }, {
          "route" : "/autoprox/calc/view/:type/:name",
          "templateHref" : "ui-addons/autoprox/partials/calc.html"
        }, {
          "route" : "/autoprox/rules",
          "templateHref" : "ui-addons/autoprox/partials/rules.html"
        }, {
          "route" : "/autoprox/rules/view/:name",
          "templateHref" : "ui-addons/autoprox/partials/rules.html"
        } ],
        "sections" : [ {
          "name" : "AutoProx Calculator",
          "route" : "/autoprox/calc"
        }, {
          "name" : "AutoProx Rules",
          "route" : "/autoprox/rules"
        } ],
        "initJavascriptHref" : "ui-addons/autoprox/js/autoprox.js"
      }, {
        "name" : "AutoProx"
      }, {
        "name" : "Dependency Grapher"
      }, {
        "name" : "Folo"
      }, {
        "name" : "Indexer"
      }, {
        "name" : "Revisions",
        "routes" : [ {
          "route" : "/revisions/changelog/stores",
          "templateHref" : 
              "ui-addons/revisions/partials/store-changelog.html"
        } ],
        "sections" : [ {
          "name" : "Store Changelogs",
          "route" : "/revisions/changelog/stores"
        } ],
        "initJavascriptHref" : "ui-addons/revisions/js/revisions.js"
      }, {
        "name" : "dotMaven"
      } ]
    }

#### Also, the Javascript to load active add-ons...

For completeness, it's worth mentioning that a **GET** request to the `/api/stats/addons/active.js` path will render the active add-ons to a JSON object declared as a Javascript variable `addons`, which is used by the web UI to construct the menus and initialize the browser-side controller logic necessary to support the add-on features:

    $ curl -i http://localhost:8080/api/stats/addons/active.js
    HTTP/1.1 200 OK
    Connection: keep-alive
    Set-Cookie: JSESSIONID=nyOs9EbvmuQVXLohg77_dobG; path=/
    Content-Type: application/json
    Content-Length: 1298
    Date: Mon, 11 May 2015 20:43:11 GMT

    var addons = {
      "items" : [ {
        "name" : "AutoProx",
        "routes" : [ {
          "route" : "/autoprox/calc",
          "templateHref" : "ui-addons/autoprox/partials/calc.html"
        }, {
          "route" : "/autoprox/calc/view/:type/:name",
          "templateHref" : "ui-addons/autoprox/partials/calc.html"
        }, {
          "route" : "/autoprox/rules",
          "templateHref" : "ui-addons/autoprox/partials/rules.html"
        }, {
          "route" : "/autoprox/rules/view/:name",
          "templateHref" : "ui-addons/autoprox/partials/rules.html"
        } ],
        "sections" : [ {
          "name" : "AutoProx Calculator",
          "route" : "/autoprox/calc"
        }, {
          "name" : "AutoProx Rules",
          "route" : "/autoprox/rules"
        } ],
        "initJavascriptHref" : "ui-addons/autoprox/js/autoprox.js"
      }, {
        "name" : "AutoProx"
      }, {
        "name" : "Dependency Grapher"
      }, {
        "name" : "Folo"
      }, {
        "name" : "Indexer"
      }, {
        "name" : "Revisions",
        "routes" : [ {
          "route" : "/revisions/changelog/stores",
          "templateHref" : 
              "ui-addons/revisions/partials/store-changelog.html"
        } ],
        "sections" : [ {
          "name" : "Store Changelogs",
          "route" : "/revisions/changelog/stores"
        } ],
        "initJavascriptHref" : "ui-addons/revisions/js/revisions.js"
      }, {
        "name" : "dotMaven"
      } ]
    };

If you wanted to construct an alternative [angular.js](http://www.angularjs.org) UI for Indy, this Javascript might be useful to you.
