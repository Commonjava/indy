package org.commonjava.indy.promote.rules

import org.apache.commons.lang.StringUtils
import org.commonjava.indy.promote.validate.model.ValidationRequest
import org.commonjava.indy.promote.validate.model.ValidationRule

import org.commonjava.atlas.maven.graph.rel.DependencyRelationship
import org.commonjava.atlas.maven.graph.rel.RelationshipType
import org.commonjava.atlas.maven.ident.DependencyScope
import org.commonjava.maven.galley.maven.rel.ModelProcessorConfig
import org.slf4j.LoggerFactory

class ArtifactRefAvailability implements ValidationRule {

    String validate(ValidationRequest request) {
        def verifyStoreKeys = request.getTools().getValidationStoreKeys(request, true);

        def errors = new ArrayList()
        def tools = request.getTools()
        def dc = new ModelProcessorConfig().setIncludeBuildSection(false).setIncludeManagedDependencies(false)

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

                            synchronized (errors) {
                                if (!found) {
                                    errors.add(String.format("%s is invalid: %s is not available via: %s",
                                            it, path, StringUtils.join(verifyStoreKeys, ", ")))
                                }

                                if (!foundPom) {
                                    errors.add(String.format("%s is invalid: %s is not available via: %s", it,
                                            tools.toArtifactPath(target.asPomArtifact()),
                                            StringUtils.join(verifyStoreKeys, ", ")))
                                }
                            }
                        }
                    })
                }
            }
        })

        errors.isEmpty() ? null: StringUtils.join(errors, "\n")
    }
}
