package org.commonjava.indy.core.content.group

import org.commonjava.indy.model.core.Group
import java.util.regex.Pattern

class RHPatternNameGroupRepositoryFilter extends ReversePatternNameGroupRepositoryFilter {
    def canProcessPattern = Pattern.compile(".+\\.(pom|jar|gz|zip|md5|sha1|sha256)\$")

    @Override
    boolean canProcess(String path, Group group) {
        return group.getPackageType().equals("maven") && canProcessPattern.matcher(path).matches()
    }

    RHPatternNameGroupRepositoryFilter() {
        super(".+-rh.+", "^build-\\d+")
    }

    @Override
    int getPriority() {
        return 10
    }
}