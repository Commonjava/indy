package org.commonjava.indy.promote.rules

import java.util.regex.Pattern
import org.apache.commons.lang.StringUtils
import org.commonjava.indy.pkg.PackageTypeConstants;
import org.commonjava.indy.promote.validate.model.ValidationRequest
import org.commonjava.indy.promote.validate.model.ValidationRule
import org.slf4j.LoggerFactory

class NPMVersionPattern implements ValidationRule {
    String validate(ValidationRequest request) throws Exception {
        def versionPattern = request.getVersionPattern()
        def scopedVersionPattern = request.getScopedVersionPattern()
        def validScope = request.getValidationParameter("validScope")
        def errors = Collections.synchronizedList(new ArrayList())
        def logger = LoggerFactory.getLogger(getClass())
        if (request.getTarget().getPackageType() == PackageTypeConstants.PKG_TYPE_NPM && request.getSource().getPackageType() == PackageTypeConstants.PKG_TYPE_NPM) {
            logger.info("Start to do NPMVersionPattern validation check for request paths: {}", request.getSourcePaths())
            if (versionPattern == null) {
                logger.info("No 'versionPattern' parameter specified in rule-set: {}, will only check scoped rule for scopes: {}.", request.getRuleSet().getName(), validScope)
            }
            def tools = request.getTools()
            tools.paralleledEach(request.getSourcePaths(), { it ->
                def pkg = tools.getNPMPackagePath(it)
                if (pkg.isPresent()) {
                    def pkgPath = pkg.get()
                    def isValidScoped = pkgPath.scoped && validScope.equals(pkgPath.scopedName.substring(1))
                    if (!isValidScoped) {
                        if (versionPattern != null) {
                            def vs = pkgPath.version
                            logger.info("Start to do version match for path {} with version {}", pkgPath, vs)

                            def matcher = versionPattern.matcher(vs)
                            if (!matcher.matches()) {
                                errors.add(String.format("%s is not %s scoped and does not match version pattern: '%s' (version was: '%s')",
                                        it, validScope, versionPattern.pattern(), vs))
                            } else {
                                logger.info("path {} with version {} matches with version pattern {}", pkgPath, vs, versionPattern.pattern())
                            }
                        }
                    } else if (scopedVersionPattern != null) {
                        def vs = pkgPath.version
                        logger.info("Start to do version match for path {} with version {}", pkgPath, vs)
                        def matcher = scopedVersionPattern.matcher(vs)
                        if (!matcher.matches()) {
                            errors.add(String.format("%s is %s scoped and does not match scoped version pattern: '%s' (version was: '%s')",
                                    it, validScope, scopedVersionPattern.pattern(), vs))
                        } else {
                            logger.info("path {} with version {} matches with version pattern {}", pkgPath, vs, scopedVersionPattern.pattern())
                        }
                    }
                }
            })
        } else {
            logger.warn("Source or Target repo package type is not NPM, will not execute this rule!", request.getRuleSet().getName())
        }
        errors.isEmpty() ? null : StringUtils.join(errors, "\n")
    }
}
