package org.commonjava.indy.action;

import java.util.Collection;

/**
 * Created by ruhan on 11/16/16.
 */
public interface UserLifecycleManager
{
    <T extends IndyLifecycleAction> Collection<T> getUserLifecycleActions(String lifecycleName, Class<T> type);
}
