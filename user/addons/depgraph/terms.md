---
title: "Depgraph Terms"
---

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

