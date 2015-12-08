package org.commonjava.indy.promote.rules

import org.commonjava.indy.promote.validate.model.ValidationRequest
import org.commonjava.indy.promote.validate.model.ValidationRule
import org.slf4j.LoggerFactory

class ParsablePom implements ValidationRule {

    String validate(ValidationRequest request) {
        def builder = new StringBuilder()
        def tools = request.getTools()
        def logger = LoggerFactory.getLogger(ValidationRule.class)
        logger.info("Parsing POMs in:\n  {}.", request.getSourcePaths().join("\n  "))

        request.getSourcePaths().each { it ->
            if (it.endsWith(".pom")) {
                logger.info("Parsing POM from path: {}.", it)
                try {
                    def pom = tools.readLocalPom(it, request.getPromoteRequest())
                }
                catch (Exception e) {
                    if (builder.length() > 0) {
                        builder.append("\n")
                    }
                    builder.append(it).append(": Failed to parse POM. Error was: ").append(e)
                }
            }
        }

        builder.length() > 0 ? builder.toString() : null
    }
}