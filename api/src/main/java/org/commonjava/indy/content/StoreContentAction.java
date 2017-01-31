package org.commonjava.indy.content;

import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;

import java.util.Set;

/**
 * Created by jdcasey on 1/27/17.
 */
public interface StoreContentAction
{
    void clearStoreContent( Set<String> paths, StoreKey originKey, Set<Group> affectedGroups, boolean clearOriginPath );
}
