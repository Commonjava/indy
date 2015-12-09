---
title: "REST-ish? What's That?"
---

##### Please keep your hands and feet inside the vehicle...

In the examples that follow, you'll notice some decidedly strange semantics to the calls we make, at least if you're a REST purist. Depgraph (and a few other add-ons) don't adhere to the normal GET-Response, POST-200-Stored-Copy, PUT-201-Location models commonly seen in REST applications. Depgraph's endpoints represent operations that can trigger graph discovery and traversal, not to mention rendering of the extracted graph result. 

Accordingly, Depgraph operations have requirements for a configuration complexity that is not easily represented in any URL that a human would want to type. Therefore, Depgraph (and a few other endpoints in Indy) have opted for an almost RPC-style semantic, except that the request is a representation of a domain-specific object, and does not contain generic, anonymous data structures attached to free-form field names. For consistency with the rest of Indy, these requests use JSON to send the configuration necessary to complete the desired operation.

Just as you can deserialize JSON responses to domain objects (for example, using the Indy Java client APIs), you could build these Depgraph requests as objects of particular classes, then serialize them to JSON. In fact, Depgraph's module for use with the Indy Java client API includes a fluent API for building these request objects.
