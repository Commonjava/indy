package org.commonjava.indy.core.content.group

import org.commonjava.indy.model.core.Group

class DefaultPatternNameGroupRepositoryFilter extends PatternNameGroupRepositoryFilter {
    @Override
    boolean canProcess(String path, Group group) {
        return group.getPackageType().equals("maven")
    }

    @Override
    protected String getPathPattern() {
        return "*"
    }

    @Override
    protected String getFilterPattern() {
        return "*"
    }

    @Override
    int getPriority() {
        return 0
    }
}