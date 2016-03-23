package org.commonjava.indy.promote.rules

import org.commonjava.indy.model.core.StoreKey
import org.commonjava.indy.promote.validate.model.ValidationRequest
import org.commonjava.indy.promote.validate.model.ValidationRule
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship
import org.commonjava.maven.atlas.graph.rel.RelationshipType
import org.commonjava.maven.atlas.ident.DependencyScope
import org.commonjava.maven.atlas.ident.ref.ArtifactRef
import org.commonjava.maven.atlas.ident.ref.SimpleTypeAndClassifier
import org.commonjava.maven.galley.maven.rel.ModelProcessorConfig
import org.slf4j.LoggerFactory

class ArtifactRefAvailability implements ValidationRule {

    String validate(ValidationRequest request) {
        def verifyStore = request.getValidationParameter("availableInStoreKey")
        StoreKey verifyStoreKey = null
        if (verifyStore == null) {
            def logger = LoggerFactory.getLogger(getClass())
            logger.warn("No external store (availableInStoreKey parameter) specified for validating path availability in rule-set: {}. Using target: {} instead.", request.getRuleSet().getName(), request.getTarget())
            verifyStoreKey = request.getTarget()
        } else {
            verifyStoreKey = StoreKey.fromString(verifyStore)
            if (verifyStoreKey == null) {
                return "Invalid target: ${verifyStore} is not a StoreKey"
            }
        }

        def builder = new StringBuilder()
        def tools = request.getTools()
        def dc = new ModelProcessorConfig().setIncludeBuildSection(false).setIncludeManagedDependencies(false)

        dc.setIncludeBuildSection(false)
        dc.setIncludeManagedDependencies(false)

        def logger = LoggerFactory.getLogger(ValidationRule.class)

        def pomTC = new SimpleTypeAndClassifier("pom")
        request.getSourcePaths().each { it ->
            if (it.endsWith(".pom")) {
                def relationships = tools.getRelationshipsForPom(it, dc, request.getPromoteRequest(), verifyStoreKey)
                if (relationships != null) {
                    relationships.each { rel ->
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
                            def txfr = tools.getTransfer(verifyStoreKey, path)
                            logger.info("{} in {}: {}. Exists? {}", target, verifyStoreKey, txfr, txfr.exists())
                            if (!txfr.exists()) {
                                txfr = tools.getTransfer(request.getSource(), path)
                                logger.info("{} in {}: {}. Exists? {}", target, request.getSource(), txfr, txfr.exists())
                                if (!txfr.exists()) {
                                    if (builder.length() > 0) {
                                        builder.append("\n")
                                    }
                                    builder.append(it).append(" is invalid: ").append(path).append(" is not available via: ").append(verifyStoreKey)
                                }
                            }

                            if ((target instanceof ArtifactRef) && !pomTC.equals(((ArtifactRef) target).getTypeAndClassifier())) {
                                path = tools.toArtifactPath(target.asPomArtifact())
                                txfr = tools.getTransfer(verifyStoreKey, path)
                                logger.info("POM {} in {}: {}. Exists? {}", target.asPomArtifact(), verifyStoreKey, txfr, txfr.exists())
                                if (!txfr.exists()) {
                                    txfr = tools.getTransfer(request.getSource(), path)
                                    logger.info("{} in {}: {}. Exists? {}", target, request.getSource(), txfr, txfr.exists())
                                    if (!txfr.exists()) {
                                        if (builder.length() > 0) {
                                            builder.append("\n")
                                        }
                                        builder.append(it).append(" is invalid: ").append(path).append(" is not available via: ").append(verifyStoreKey)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        builder.length() > 0 ? builder.toString() : null
    }
}
