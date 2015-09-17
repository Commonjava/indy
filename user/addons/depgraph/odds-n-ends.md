---
title: "Configuration Odds and Ends"
---

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

