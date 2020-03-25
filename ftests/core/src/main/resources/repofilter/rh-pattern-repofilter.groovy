package org.commonjava.indy.core.content.group

import org.commonjava.indy.model.core.Group

class RHPatternNameGroupRepositoryFilter extends ReversePatternNameGroupRepositoryFilter {
    @Override
    boolean canProcess(String path, Group group) {
        return group.getPackageType().equals("maven") && path.matches( ".+\\.(pom|jar|gz|zip|md5|sha1|sha256)\$" )
    }

    @Override
    protected String getPathPattern() {
        return ".+-rh.+"
    }

    @Override
    protected String getFilterPattern() {
        return "^build-\\d+"
    }

    @Override
    int getPriority() {
        return 10
    }
}