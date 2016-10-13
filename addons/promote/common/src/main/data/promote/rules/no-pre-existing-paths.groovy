package org.commonjava.indy.promote.rules

import org.commonjava.indy.model.core.StoreKey
import org.commonjava.indy.model.galley.KeyedLocation
import org.commonjava.indy.promote.validate.model.ValidationRequest
import org.commonjava.indy.promote.validate.model.ValidationRule
import org.slf4j.LoggerFactory

class NoPreExistingPaths implements ValidationRule {

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

        request.getSourcePaths().each { it ->
            def aref = tools.getArtifact(it);
            if (aref != null) {
                def transfer = tools.getTransfer(verifyStoreKey, it);
                if (transfer.exists()) {
                    def kl = (KeyedLocation) transfer.getLocation();
                    builder.append(it).append(" is already available in: ").append(kl.getKey());
                }
            }
        }

        builder.length() > 0 ? builder.toString() : null
    }
}
