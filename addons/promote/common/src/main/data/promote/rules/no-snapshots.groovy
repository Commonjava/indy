package org.commonjava.indy.promote.rules

import org.apache.commons.lang.StringUtils
import org.apache.commons.io.IOUtils
import org.commonjava.indy.promote.validate.model.ValidationRequest
import org.commonjava.indy.promote.validate.model.ValidationRule
import org.commonjava.maven.galley.maven.rel.ModelProcessorConfig

class NoSnapshots implements ValidationRule {

    String validate(ValidationRequest request) {
        def verifyStoreKeys = request.getTools().getValidationStoreKeys(request, true)

        def errors = new ArrayList()
        def tools = request.getTools()
        def dc = new ModelProcessorConfig().setIncludeBuildSection(true).setIncludeManagedPlugins(true).setIncludeManagedDependencies(true)

        tools.paralleledEach(request.getSourcePaths(), { it ->
            if (it.endsWith(".pom")) {
                def ref = tools.getArtifact(it)
                if (ref != null) {
                    if (!ref.getVersionSpec().isRelease()) {
                        synchronized (errors) {
                            errors.add(String.format("%s is a variable/snapshot version.", it))
                        }
                    }
                }

                def relationships = tools.getRelationshipsForPom(it, dc, request, verifyStoreKeys)
                if (relationships != null) {
                    tools.paralleledEach(relationships, { rel ->
                        def target = rel.getTarget()
                        if (!target.getVersionSpec().isRelease()) {
                            synchronized (errors) {
                                errors.add(String.format("%s uses a variable/snapshot version in: %s", target, it))
                            }
                        }
                    })
                }
            }
        })

        errors.isEmpty() ? null: StringUtils.join(errors, "\n")
    }
}