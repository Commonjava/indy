package org.commonjava.indy.promote.rules

import org.apache.commons.lang.StringUtils
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor
import org.commonjava.indy.promote.validate.PromotionValidationException
import org.commonjava.indy.promote.validate.model.ValidationRequest
import org.commonjava.indy.promote.validate.model.ValidationRule
import org.commonjava.maven.galley.io.checksum.ContentDigest

class MavenNoPreExistingPaths implements ValidationRule {

    String validate(ValidationRequest request) throws PromotionValidationException {
        def verifyStoreKeys = request.getTools().getValidationStoreKeys(request, false);

        def errors = Collections.synchronizedList(new ArrayList());
        def tools = request.getTools()
        tools.paralleledEach(request.getSourcePaths(), { it ->
            def aref = tools.getArtifact(it);
            if (aref != null) {
                tools.forEach(verifyStoreKeys, { verifyStoreKey ->
                    if (tools.exists(verifyStoreKey, it)
                            && !(tools.digest(verifyStoreKey, it, MavenPackageTypeDescriptor.MAVEN_PKG_KEY).get(ContentDigest.SHA_256)
                            .equals(tools.digest(request.getPromoteRequest().getSource(), it, MavenPackageTypeDescriptor.MAVEN_PKG_KEY).get(ContentDigest.SHA_256)))) {
                        errors.add(String.format("%s is already available with different checksum in: %s", it, verifyStoreKey))
                    }
                })
            }
        })

        errors.isEmpty() ? null: StringUtils.join(errors, "\n")
    }
}
