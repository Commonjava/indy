---
title: "Depgraph Project-Relationship Add-On"
breadcrumbs:
- href: "../../index.html"
  title: "Add-Ons"
---

At its most basic, every Maven POM file is an expression of some collection of relationships between this project and others. Whether these are declarations that the current project depends on code from some other in order to execute, or that it uses a specific version of a Maven plugin during its build, these are all relationships to some other code. That other code, in turn, normally has a POM associated with it that expresses *its* relationships to still more projects. Discovering, extracting, and storing this relationship information in a graph can enable you to answer some very interesting questions about collections of projects.

This is the purpose of the depgraph add-on.

Depgraph is an integration point with the Cartographer project, which provides operations to discover and traverse these relationship graphs. It defines a REST-ish facade that makes Cartographer available via HTTP, and the typical client api and domain model you're no doubt used to seeing with Indy add-ons.

### Contents

* [REST-ish? What's REST-ish??](rest-ish.html)
* [Terms](terms.html)
* [Request Types](request-types.html)
* [Feature Documentation](features.html)
* [Configuration Odds and Ends](odds-n-ends.html)
