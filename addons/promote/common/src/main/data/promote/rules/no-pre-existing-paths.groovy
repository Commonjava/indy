package org.commonjava.indy.promote.rules

import org.commonjava.indy.model.core.StoreKey
import org.commonjava.indy.model.galley.KeyedLocation
import org.commonjava.indy.promote.validate.PromotionValidationException
import org.commonjava.indy.promote.validate.PromotionValidationTools
import org.commonjava.indy.promote.validate.model.ValidationRequest
import org.commonjava.indy.promote.validate.model.ValidationRule
import org.slf4j.LoggerFactory

class NoPreExistingPaths implements ValidationRule {

    String validate(ValidationRequest request) throws PromotionValidationException {
        def verifyStoreKeys = request.getTools().getValidationStoreKeys(request, false);

        def builder = new StringBuilder()
        def tools = request.getTools()

        request.getSourcePaths().each { it ->
            def aref = tools.getArtifact(it);
            if (aref != null) {
                verifyStoreKeys.each { verifyStoreKey ->
                    def transfer = tools.getTransfer(verifyStoreKey, it);
                    if (transfer != null && transfer.exists()) {
                        def kl = (KeyedLocation) transfer.getLocation();
                        if (builder.length() > 0) {
                            builder.append("\n")
                        }
                        builder.append(it).append(" is already available in: ").append(kl.getKey());
                    }
                }
            }
        }

        builder.length() > 0 ? builder.toString() : null
    }
}
