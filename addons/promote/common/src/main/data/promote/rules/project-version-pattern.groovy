package org.commonjava.indy.promote.rules

import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.promote.validate.model.ValidationRequest
import org.commonjava.indy.promote.validate.model.ValidationRule
import org.slf4j.LoggerFactory

class ProjectVersionPattern implements ValidationRule {

    String validate(ValidationRequest request) throws Exception {
        def versionPattern = request.getValidationParameter("versionPattern")
        def errors = new ArrayList()

        if (versionPattern != null) {
            def tools = request.getTools()
            tools.paralleledEach(request.getSourcePaths(), { it ->
                def ref = tools.getArtifact(it)
                if (ref != null) {
                    def vs = ref.getVersionString()
                    if (!vs.matches(versionPattern)) {
                        synchronized (errors) {
                            errors.add(String.format("%s does not match version pattern: '%s' (version was: '%s')",
                                    it, versionPattern, vs))
                        }
                    }
                }
            })
        } else {
            def logger = LoggerFactory.getLogger(getClass())
            logger.warn("No 'versionPattern' parameter specified in rule-set: {}. Cannot execute ProjectVersionPattern rule!", request.getRuleSet().getName())
        }

        errors.isEmpty() ? null: StringUtils.join(errors, "\n")
    }
}