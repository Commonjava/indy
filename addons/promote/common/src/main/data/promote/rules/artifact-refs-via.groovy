package org.commonjava.indy.promote.rules

import org.apache.commons.lang.StringUtils
import org.commonjava.indy.promote.validate.model.ValidationRequest
import org.commonjava.indy.promote.validate.model.ValidationRule

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship
import org.commonjava.maven.atlas.graph.rel.RelationshipType
import org.commonjava.maven.atlas.ident.DependencyScope
import org.commonjava.maven.galley.maven.rel.ModelProcessorConfig
import org.slf4j.LoggerFactory

class ArtifactRefAvailability implements ValidationRule {

    String validate(ValidationRequest request) {
        def verifyStoreKeys = request.getTools().getValidationStoreKeys(request, true);

        def builder = new StringBuilder()
        def tools = request.getTools()
        def dc = new ModelProcessorConfig().setIncludeBuildSection(false).setIncludeManagedDependencies(false)

        dc.setIncludeBuildSection(false)
        dc.setIncludeManagedDependencies(false)

        def logger = LoggerFactory.getLogger(ValidationRule.class)

        tools.paralleledEach(request.getSourcePaths(), { it ->
            if (it.endsWith(".pom")) {
                def relationships = tools.getRelationshipsForPom(it, dc, request, verifyStoreKeys)
                if (relationships != null) {
                    tools.paralleledEach(relationships, { rel ->
                        def skip = false
                        if (rel.getType() == RelationshipType.DEPENDENCY) {
                            def dr = (DependencyRelationship) rel
                            if ((dr.getScope() == DependencyScope.system) || dr.isOptional()) {
                                skip = true
                            }
                        }

                        if (!skip) {
                            def target = rel.getTarget()
                            def path = tools.toArtifactPath(target)
                            def pomPath = tools.toArtifactPath(target.asPomArtifact())

                            def found = false
                            def foundPom = false

                            tools.paralleledEach(verifyStoreKeys, { verifyStoreKey ->
                                if (!found) {
                                    def txfr = tools.getTransfer(verifyStoreKey, path)
                                    logger.info("{} in {}: {}. Exists? {}", target, verifyStoreKey, txfr, txfr == null ? false : txfr.exists())
                                    if (txfr != null && txfr.exists()) {
                                        logger.info("Marking as found: {}", target.asPomArtifact());
                                        found = true
                                    }
                                }

                                if (!foundPom) {
                                    def txfr = tools.getTransfer(verifyStoreKey, pomPath)
                                    logger.info("POM {} in {}: {}. Exists? {}", target.asPomArtifact(), verifyStoreKey, txfr, txfr == null ? false : txfr.exists())
                                    if (txfr != null && txfr.exists()) {
                                        logger.info("Marking as found: {}", target.asPomArtifact());
                                        foundPom = true
                                    }
                                }
                            })

                            if (!found) {
                                if (builder.length() > 0) {
                                    builder.append("\n")
                                }
                                builder.append(it)
                                        .append(" is invalid: ")
                                        .append(path)
                                        .append(" is not available via: ")
                                        .append(StringUtils.join(verifyStoreKeys, ", "))
                            }

                            if (!foundPom) {
                                if (builder.length() > 0) {
                                    builder.append("\n")
                                }
                                builder.append(it)
                                        .append(" is invalid: ")
                                        .append(tools.toArtifactPath(target.asPomArtifact()))
                                        .append(" is not available via: ")
                                        .append(StringUtils.join(verifyStoreKeys, ", "))
                            }
                        }
                    })
                }
            }
        })

        builder.length() > 0 ? builder.toString() : null
    }
}
