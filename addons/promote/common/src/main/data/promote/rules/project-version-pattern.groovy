package org.commonjava.aprox.promote.rules;

import org.commonjava.aprox.promote.validate.model.ValidationRequest
import org.commonjava.aprox.promote.validate.model.ValidationRule
import org.slf4j.LoggerFactory

class ProjectVersionPattern implements ValidationRule {

    String validate(ValidationRequest request) throws Exception {
        def versionPattern = request.getValidationParameter("versionPattern")
        def builder = new StringBuilder()

        if (versionPattern != null) {
            def tools = request.getTools()
            request.getSourcePaths().each { it ->
                def ref = tools.getArtifact(it)
                if (ref != null) {
                    def vs = ref.getVersionString()
                    if (!vs.matches(versionPattern)) {
                        if (builder.length() > 0) {
                            builder.append("\n")
                        }
                        builder.append(it).append(" does not match version pattern: '").append(versionPattern).append("' (version was: '").append(vs).append("')")
                    }
                }
            }
        } else {
            def logger = LoggerFactory.getLogger(getClass())
            logger.warn("No 'versionPattern' parameter specified in rule-set: {}. Cannot execute ProjectVersionPattern rule!", request.getRuleSet().getName())
        }

        builder.length() > 0 ? builder.toString() : builder
    }
}