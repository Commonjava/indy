---
title: "Commonjava Project Dependencies"
---

### Indy Technology Spin-Offs

During development of Indy, several other libraries have been developed that have usefulness outside of Indy itself. These libraries address problems that are largely self-contained. In order to enable their use outside of Indy, they have been spun off into separate projects. The downside of this is that sometimes new features or bugfixes require changes in these dependency projects as well as Indy itself. When this happens, Indy's pre-release codebase will depend on SNAPSHOT artifacts from these projects until the new feature or bugfix has been stabilized and the project can be released. While this does inject some build risk, the benefits of reuse make this well worthwhile.

These projects are:

* [Atlas](https://github.com/Commonjava/atlas) (Artifact/Project identities; dependency graph database used in depgraph add-on)
* [Galley](https://github.com/Commonjava/galley) (File/Artifact transport and caching; Maven POM/metadata parsing)
* [Cartographer](https://github.com/Commonjava/cartographer) (dependency graphing; used in depgraph add-on)
* [Partyline](https://github.com/Commonjava/partyline) (Multiple, simultaneous read/write access to files)
* [Weft](https://github.com/Commonjava/weft) (Thread/Executor management)
* [Webdav Handler](https://github.com/Commonjava/webdav-handler) (WebDAV support; forked from webdav-servlet on SourceForge)
* [HTTP Test-Server](https://github.com/Commonjava/http-testserver) (HTTP test fixture)
