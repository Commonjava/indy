package org.commonjava.indy.promote.rules

import org.commonjava.indy.promote.validate.model.ValidationRequest
import org.commonjava.indy.promote.validate.model.ValidationRule
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory

class NPMParsablePackageMeta implements ValidationRule {

    String validate(ValidationRequest request) {
        def errors = Collections.synchronizedList(new ArrayList());
        def tools = request.getTools()
        def logger = LoggerFactory.getLogger(ValidationRule.class)
        def paths = request.getSourcePaths(true, false)
        logger.info("Parsing package.json in:\n  {}", paths.join("\n  "))

        tools.paralleledEach(paths, { it ->
            if (it.endsWith("package.json")) {
                logger.info("Parsing package.json from path: {}.", it)
                try {
                    tools.readLocalPackageJson(it, request)
                }
                catch (Exception e) {
                    errors.add(String.format("%s: Failed to parse package.json. Error was: %s", it, e))
                }
            }
        })

        errors.isEmpty() ? null: StringUtils.join(errors, "\n")
    }
}