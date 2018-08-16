package org.commonjava.indy.promote.rules

import org.commonjava.indy.promote.validate.PromotionValidationException
import org.commonjava.indy.promote.validate.model.ValidationRequest
import org.commonjava.indy.promote.validate.model.ValidationRule

class NoPreExistingPaths implements ValidationRule {

    String validate(ValidationRequest request) throws PromotionValidationException {
        def verifyStoreKeys = request.getTools().getValidationStoreKeys(request, false);

        def builder = new StringBuilder()
        def tools = request.getTools()

        tools.paralleledEach(request.getSourcePaths(), { it ->
            def aref = tools.getArtifact(it);
            if (aref != null) {
                tools.paralleledEach(verifyStoreKeys, { verifyStoreKey ->
                    if (tools.exists(verifyStoreKey, it)) {
                        if (builder.length() > 0) {
                            builder.append("\n")
                        }
                        builder.append(it).append(" is already available in: ").append(verifyStoreKey);
                    }
                })
            }
        })

        builder.length() > 0 ? builder.toString() : null
    }
}
