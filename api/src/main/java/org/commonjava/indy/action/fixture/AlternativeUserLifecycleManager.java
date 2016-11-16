package org.commonjava.indy.action.fixture;

import org.commonjava.indy.action.IndyLifecycleAction;
import org.commonjava.indy.action.UserLifecycleManager;

import javax.enterprise.inject.Alternative;
import java.util.Collection;
import java.util.Collections;

/**
 * The default implementation for UserLifecycleManager is in core. For those who use indy-api but not indy-core,
 * this can be an alternative injection class via beans.xml.
 *
 * Created by ruhan on 11/16/16.
 */
@Alternative
public final class AlternativeUserLifecycleManager
        implements UserLifecycleManager
{
    public <T extends IndyLifecycleAction> Collection<T> getUserLifecycleActions(String lifecycleName, Class<T> type)
    {
        return Collections.EMPTY_SET;
    }
}
