package org.commonjava.indy.promote.rules;

import org.commonjava.indy.promote.validate.model.ValidationRequest
import org.commonjava.indy.promote.validate.model.ValidationRule
import org.commonjava.atlas.maven.ident.ref.SimpleTypeAndClassifier
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class ProjectArtifacts implements ValidationRule {

    String validate(ValidationRequest request) throws Exception {
        def classifierAndTypeSet = request.getValidationParameter("classifierAndTypeSet")
        def errors = new ArrayList()

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

            def logger = LoggerFactory.getLogger(getClass())
            def tools = request.getTools()
            def projectTCs = new ConcurrentHashMap();
            tools.paralleledEach(request.getSourcePaths(), { it ->
                def ref = tools.getArtifact(it)
                if (ref != null) {
                    def gav = ref.asProjectVersionRef()
                    logger.trace("Checking promotion on GAV: {} from source path: {}", gav, it)
                    def found = null
                    synchronized (projectTCs) {
                        found = projectTCs.get(gav)
                        if (found == null) {
                            found = new HashSet()
                            projectTCs.put(gav, found)
                        }
                    }

                    synchronized (found) {
                        found.add(ref.getTypeAndClassifier())
                    }
                    logger.trace( "Found: {} -> {}", gav, found)
                }
            })

            tools.paralleledEach(projectTCs, { entry ->
                logger.trace( "Processing {} -> {}", entry.key, entry.value)
                if ( entry.value.size() > 1 || !entry.value.contains(new SimpleTypeAndClassifier("pom"))) {
                    tcs.each { tc ->
                        logger.trace("Checking if TC: {} is in: {} for: {}", tc, entry.value, entry.key)
                        if (!entry.value.contains(tc)) {
                            synchronized(errors) {
                                errors.add(String.format("%s: missing artifact with type/classifier: %s", entry.key, tc))
                            }
                        }
                    }
                }
            })
        } else {
            logger.warn("No 'classifierAndTypeSet' parameter specified in rule-set: {}. Cannot execute ProjectArtifacts rule!", request.getRuleSet().getName())
        }

        errors.isEmpty() ? null: StringUtils.join(errors, "\n")
    }
}