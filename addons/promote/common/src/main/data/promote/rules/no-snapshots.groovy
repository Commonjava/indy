package org.commonjava.indy.promote.rules

import org.commonjava.indy.model.core.StoreKey
import org.commonjava.indy.promote.validate.model.ValidationRequest
import org.commonjava.indy.promote.validate.model.ValidationRule
import org.commonjava.maven.galley.maven.rel.ModelProcessorConfig
import org.slf4j.LoggerFactory

class NoSnapshots implements ValidationRule {

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
        def dc = new ModelProcessorConfig().setIncludeBuildSection(true).setIncludeManagedPlugins(true).setIncludeManagedDependencies(true)

        request.getSourcePaths().each { it ->
            if (it.endsWith(".pom")) {
                def ref = tools.getArtifact(it)
                if (ref != null) {
                    if ( !ref.getVersionSpec().isRelease() )
                    {
                        if (builder.length() > 0) {
                            builder.append("\n")
                        }
                        builder.append(it).append(" is a variable/snapshot version.")
                    }
                }

                def relationships = tools.getRelationshipsForPom(it, dc, request.getPromoteRequest(), verifyStoreKey)
                if (relationships != null) {
                    relationships.each { rel ->
                        def target = rel.getTarget()
                        if (!target.getVersionSpec().isRelease()) {
                            if (builder.length() > 0) {
                                builder.append("\n")
                            }
                            builder.append(target).append(" uses a variable/snapshot version in: ").append(it)
                        }
                    }
                }
            }
        }

        builder.length() > 0 ? builder.toString() : null
    }
}