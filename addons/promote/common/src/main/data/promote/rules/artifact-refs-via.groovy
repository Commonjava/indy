package org.commonjava.aprox.promote.rules

import org.commonjava.aprox.model.core.StoreKey
import org.commonjava.aprox.promote.validate.model.ValidationRequest
import org.commonjava.aprox.promote.validate.model.ValidationRule
import org.commonjava.cartographer.graph.discover.DiscoveryConfig
import org.commonjava.maven.atlas.ident.ref.ArtifactRef
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
        def dc = DiscoveryConfig.getDisabledConfig();

        dc.setIncludeBuildSection(false)
        dc.setIncludeManagedDependencies(false)

        def pomTC = new SimpleTypeAndClassifier("pom")
        request.getSourcePaths().each { it ->
            if (it.endsWith(".pom")) {
                def relationships = tools.getRelationshipsForPom(it, dc, request.getPromoteRequest(), verifyStoreKey)
                if (relationships != null) {
                    relatioships.each { rel ->
                        def target = rel.getTarget()
                        def path = tools.toArtifactPath(target)
                        def txfr = tools.getTransfer(verifyStoreKey, path)
                        if (!txfr.exists()) {
                            if (builder.length() > 0) {
                                builder.append("\n")
                            }
                            builder.append(path).append(" not available via: ").append(verifyStore)
                        }

                        if ((target instanceof ArtifactRef) && !pomTC.equals(((ArtifactRef) target).getTypeAndClassifier())) {
                            path = tools.toArtifactPath(target.asPomArtifact())
                            txfr = tools.getTransfer(verifyStoreKey, path)
                            if (!txfr.exists()) {
                                if (builder.length() > 0) {
                                    builder.append("\n")
                                }
                                builder.append(path).append(" not available via: ").append(verifyStore)
                            }
                        }
                    }
                }
            }
        }

        builder.length() > 0 ? builder.toString() : null
    }
}