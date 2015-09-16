---
title: "Depgraph Project-Relationship Grapher Add-On"
---

At its most basic, every Maven POM file is an expression of some collection of relationships between this project and others. Whether these are declarations that the current project depends on code from some other in order to execute, or that it uses a specific version of a Maven plugin during its build, these are all relationships to some other code. That other code, in turn, normally has a POM associated with it that expresses *its* relationships to still more projects. Discovering, extracting, and storing this relationship information in a graph can enable you to answer some very interesting questions about collections of projects.

This is the purpose of the depgraph add-on.

Depgraph is an integration point with the Cartographer project, which provides operations to discover and traverse these relationship graphs. It defines a REST-ish facade that makes Cartographer available via HTTP, and the typical client api and domain model you're no doubt used to seeing with AProx add-ons.

### Contents

* [REST-ish? What's REST-ish??](#rest-ish)
* [Terms](#terms)
* Features
  * **[Primer: Properties Common to Depgraph Request Configurations](#request)**
  * **[Administration / Maintenance](#admin)**
    * Listing / deleting workspaces
    * Re-scanning previously discovered GAVs for metadata (extra info like SCM available in POMs)
    * Reindexing previously discovered relationships
    * Storing / updating arbitrary metadata attached to GAVs
  * **[Graph Query / Extraction](#query)**
    * List GAVs (with option for matching a GA pattern) in a RelationshipGraph that are:
      * included
      * missing (not yet resolved)
      * in error (attempted but failed resolve)
      * variable (snapshots and the like)
    * List paths from a [set of] root GAV to some specific [set of] target GA
    * List ancestry or build order for a particular [set of] GAV
    * List relationship cycles in the graph
    * Retrieve the full graph in JSON format
    * Retrieve the full list of artifacts in the graph, in JSON format
  * **[Rendering](#render)**
    * Create a BOM / POM from a RelationshipGraph
    * Create a Maven console-style log of "Downloading..." lines for artifacts in the graph
    * Create a zip archive containing all artifacts in the graph
    * Generate `dependency:tree` or `dependency:list` style output for the graph
* Odds and Ends
  * [Relationship-Set Patchers](#patchers)
  * [Metadata Scanners](#scanners)


### REST-ish? What's REST-ish??
<a name="rest-ish"></a>

***Please keep your hands and feet inside the vehicle...***

In the examples that follow, you'll notice some decidedly strange semantics to the calls we make, at least if you're a REST purist. Depgraph (and a few other add-ons) don't adhere to the normal GET-Response, POST-200-Stored-Copy, PUT-201-Location models commonly seen in REST applications. Depgraph's endpoints represent operations that can trigger graph discovery and traversal, not to mention rendering of the extracted graph result. 

Accordingly, Depgraph operations have requirements for a configuration complexity that is not easily represented in any URL that a human would want to type. Therefore, Depgraph (and a few other endpoints in AProx) have opted for an almost RPC-style semantic, except that the request is a representation of a domain-specific object, and does not contain generic, anonymous data structures attached to free-form field names. For consistency with the rest of AProx, these requests use JSON to send the configuration necessary to complete the desired operation.

Just as you can deserialize JSON responses to domain objects (for example, using the AProx Java client APIs), you could build these Depgraph requests as objects of particular classes, then serialize them to JSON. In fact, Depgraph's module for use with the AProx Java client API includes a fluent API for building these request objects.

### Terms
<a name="terms"></a>

Depgraph uses a few terms that can be a little confusing at first, so let's define them up front.

* **Workspace** - This is equivalent to a stored graph database instance. It's the basic level of isolation between collections of relationships.
* **View / ViewParams** - Each workspace can have one or more views on the relationships stored within. Views are defined by:
  * **a workspace** ...duh.
  * **one or more "root" or top-level GAVs** where any traversal (or graph discovery) begins
  * **a filter** which selects which relationships are included in any graph traversal
  * **a set of selected versions** which provide a way to guide the discovery of a relationship graph by pushing discovered GAV versions to use something else (like what a Maven BOM does)
  * **a mutator** which operates during a graph traverse, and under most circumstances uses the selected versions and any "managed" relationships up-path to manage GAV versions in successive path nodes (think of how Maven's dependencyManagement influences transitive dependencies)
* **Graph / RelationshipGraph** - Interface combining a workspace and a view definition, and providing operations to work with the relationships that are available in that view.
* **Filter** - A class that determines whether a relationship belongs in the current view / traversal or not, and can construct a "child" filter based on itself to apply to successive nodes in a graph path (useful for resolving runtime dependencies of a plugin declared in a POM, for example)
* **Traversal** - A class that applies one or more filters to a RelationshipGraph and stores paths and/or nodes in order to arrive at some specific extraction from the graph. A Build Order traversal, for example, accumulates a partially ordered list of the GAVs in the graph.
* **Node** - a GAV stored in a graph, optionally augmented with metadata from the POM or elsewhere
* **Relationship** - an expression of one way that two GAVs are related, possibly with additional information specific to relationship according to its type

#### Why are there scare quotes on "root" (as in "root" GAVs)

Mathematically speaking, graphs don't really have roots. And in reality, neither do project-relationship graphs. However, you do need to have some entry point in order to traverse (or discover!) a graph, and it seems simpler to think of these as roots. If you like, you could call them traversal roots, since that's what they are. The quotes are just a way of acknowledging this potentially confusing situation.

### Features

Depgraph's features can be broken down into a few broad categories including administration and maintenance, graph querying and extraction, and graph rendering. The following sections provide detail on each of these feature categories, but first let's take a look at some common features of the request configurations that Depgraph uses.

#### Primer: Properties Common to Depgraph Request Configurations
<a name="request"></a>

Depgraph requests come in one of four basic flavors: raw single-graph, raw multi-graph, and two that I like to think of as "single-graph with extras" and "multi-graph with extras".

##### Request Properties Common to All

Regardless of what flavor of Depgraph request you're building, it will have some of the following properties:

**Required:**

* **workspaceId** - The graph's database name
* **source** - The AProx store key to use when discovering graph relationships and nodes

**Optional:**

* **patcherIds** - List of keys identifying "patchers", or components run after relationships are extracted from a POM that will amend the relationship set based on some rule or other (see below).
* **resolve** - Defaults to `true`. If you set this to `false`, your operation will execute with whatever information is in the graph, and not try to discover missing nodes/relationships.
* **injectedBOMs** - List of GAVs referencing BOMs whose `dependencyManagement` sections should be read to establish a map of version selections to use in coercing GAVs during graph traversal or discovery.
* **versionSelections** - Map of GA to GAV that directly specifies the GAV coercions to be applied during graph traversal or discovery.
* **excludedSubgraphs** - List of GAVs beyond which traversal and discovery should halt.

##### Single-Graph (with extras) Requests

Single-graph requests are simply that: one or more "root" GAVs whose graphs of relationships all exist in the same workspace with the same view parameters. Operations using single-graph requests are by far the most common in Depgraph, since you can achieve so much with filtering and "root" specification. These requests are often embellished with extra information required for particular operations, such as a set of target GAVs (for the paths operation), or a set of matching GAV patterns (for operations listing various types of project nodes), or even a set of metadata keys to collate.

All variants of the single-graph request have one common configuration section in common: a field in the main object called "graph", which is of type GraphDescription. A GraphDescription contains a set of root GAVs, a filter, a preset and accompanying map of preset parameters (which are used to select a filter on the server side), and a default preset. Of these, the filter and default preset are managed entirely on the server side, and are not used by clients at all.

##### Multi-Graph (with extras) Requests

These requests concern the combination of two or more relationship graphs using a calculation of some sort as the combining operation. Regardless of the variant request type, multi-graph requests all contain a field called "graphs" of type GraphComposition. GraphComposition consists of a list of one or more GraphDescription objects (see single-graph requests above) and a (nullable) calculation. Available calculations include (from the GraphCalculationType enum):

* ADD
* SUBTRACT
* INTERSECT

If only one GraphDescription is provided, then the calculation is disregarded and the request operates as if it were a single-graph request. In fact, many of the operations that accept multi-graph requests are most commonly used in single-graph mode. The multi-graph graphs field is simply there to provide more flexibility.

Currently, the only truly multi-graph operations include calculating the diff (additions, subtractions) between two graphs, and calculating the target drift between two graphs (intended to be graphs of two versions of a GAV or GAV-set).

#### Administration and Maintenance
<a name="admin"></a>

#### Graph Querying and Extraction
<a name="query"></a>

#### Graph Rendering
<a name="render"></a>

### Odds and Ends

#### Relationship-Set Patchers
<a name="patchers"></a>

Patchers are designed to alter the relationship set extracted from a POM in order to correct for common scenarios where a relationship isn't declared in the normal structures of the POM. Instead, these relationships are expressed purely through some plugin configuration or other questionable [ab]use of the POM.

Available patchers include:

* **dist-pom**

  Distribution POM support, which detects POMs that look like their purpose is to generate an assembly artifact using provided-scope dependencies. This patcher will shift the scope for non-managed dependencies from **provided** to a Cartographer-specific scope called **embedded**. The purpose of this custom scope is to say that the artifact was included in the output of this project. This corrects the graph for an abuse of the provided scope, which takes dilutes the original concept of an artifact which will already be present in the deployment environment, without confusing the corrected relationships with something originally declared as having runtime or compile scope.

* **dependency-plugin**

  Another place where relationships (specifically, dependencies) are typically expressed informally is in the configuration of the `maven-dependency-plugin`. This plugin offers a variety of tools to build engineers, but a few goals (notably `dependency:copy` and `dependency:unpack`) allow the plugin to download and place an artifact into the build's eventual output without ever declaring a dependency. This patcher detects use of the configuration section `artifactItems` and adds new dependency relationships for each artifact it detects, again making use of Cartographer's custom **embedded** scope.

#### Metadata Scanners
<a name="scanners"></a>

Metadata scanners are designed to comb over the POM after project relationships have been extracted, and attach metadata like SCM URL to the GAV that represents the POM in the graph. This offers the opportunity to collate these GAVs according to one or more of these detected metadata keys.

Available metadata scanners include:

* **ScmUrlScanner** - extracts the `<url>` and `<connection>` URLs from the POM's `<scm>` section
* **LicenseScanner** - extracts the license name and url from the POM's `<licenses>` section

In addition, basic node information such as `groupId`, `artifactId`, and `version` are automatically added to the metadata whenever a metadata operation is performed on the graph.

