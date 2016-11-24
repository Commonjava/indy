package org.commonjava.indy.promote.rules;

import org.commonjava.indy.promote.validate.model.ValidationRequest
import org.commonjava.indy.promote.validate.model.ValidationRule
import org.commonjava.maven.atlas.ident.ref.SimpleTypeAndClassifier
import org.slf4j.LoggerFactory

class ProjectArtifacts implements ValidationRule {

    String validate(ValidationRequest request) throws Exception {
        def classifierAndTypeSet = request.getValidationParameter("classifierAndTypeSet")
        def builder = new StringBuilder()

        if (classifierAndTypeSet != null) {
            def ctStrings = classifierAndTypeSet.split("\\s*,\\s*")
            def tcs = []
            ctStrings.each { ctString ->
                def parts = ctString.split(":")
                if (parts.length > 0) {
                    if (parts.length < 2) {
                        parts += ""
                    }

                    tcs << new SimpleTypeAndClassifier(parts[1], parts[0])
                }
            }

            def tools = request.getTools()
            def projectTCs = [:]
            request.getSourcePaths().each {
                def ref = tools.getArtifact(it)
//                if (ref != null && ref.getType() != "pom") {
                if (ref != null) {
                    def gav = ref.asProjectVersionRef()
                    def found = projectTCs[gav]
                    if (found == null) {
                        found = []
                        projectTCs[gav] = found
                    }
                    found << ref.getTypeAndClassifier()
                }
            }

            projectTCs.each { entry ->
                if ( entry.value.size > 1 || !entry.value.contains(new SimpleTypeAndClassifier("pom"))) {
                    tcs.each { tc ->
                        if (!entry.value.contains(tc)) {
                            if (builder.length() > 0) {
                                builder.append("\n")
                            }
                            builder.append(entry.key).append(": missing artifact with type/classifier: ").append(tc)
                        }
                    }
                }
            }
        } else {
            def logger = LoggerFactory.getLogger(getClass())
            logger.warn("No 'classifierAndTypeSet' parameter specified in rule-set: {}. Cannot execute ProjectArtifacts rule!", request.getRuleSet().getName())
        }

        builder.length() > 0 ? builder.toString() : builder
    }
}