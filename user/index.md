---
title: "Indy User's Guide"
---

### NOTE: Migrating from AProx

Indy has built-in migration logic to port your groovy scripts (promotion validation rules and autoprox rules) off of AProx package names. However, you must migrate the `etc/aprox` directory yourself if you have a customized configuration. To do this, rename the `etc/aprox` directory to `etc/indy`, then alter each file to replace:

* `${aprox.home}` with `${indy.home}`
* `${aprox.config}` with `${indy.config}`
* `${aprox.config.dir}` with `${indy.config.dir}`

After this, you should be ready to go!

### Basics

* [Quick Start](quickstart.html)
* [Repositories and Groups](repos-groups.html)
* [Java Client API](java-client-basics.html)
* [REST API](rest-client-basics.html)

### Deployment Options

* [Deployment via Docker](docker.html)
* [Traditional Installation](traditional-install.html)
<!-- * [Embedding](embedding.html) -->

### Features and Add-Ons

Indy provides various add-ons that supplement the core functionality. For the full list, with links to documentation on each, take a look here:

* [Feature Documentation](addons/index.html)

