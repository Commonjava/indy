package org.commonjava.indy.promote.rules

import java.util.regex.Pattern
import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.promote.validate.model.ValidationRequest
import org.commonjava.indy.promote.validate.model.ValidationRule
import org.slf4j.LoggerFactory

class MavenProjectVersionPattern implements ValidationRule {

    String validate(ValidationRequest request) throws Exception {
        def versionPattern = request.getVersionPattern()
        def errors = Collections.synchronizedList(new ArrayList());

        if (versionPattern != null) {
            def tools = request.getTools()
            tools.paralleledEach(request.getSourcePaths(), { it ->
                def ref = tools.getArtifact(it)
                if (ref != null) {
                    def vs = ref.getVersionString()
                    def matcher = versionPattern.matcher(vs)
                    if (!matcher.matches()) {
                        errors.add(String.format("%s does not match version pattern: '%s' (version was: '%s')",
                                it, versionPattern.pattern(), vs))
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