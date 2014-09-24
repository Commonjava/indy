package org.commonjava.aprox.core.expire;

import org.commonjava.aprox.model.StoreKey;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;

public class StoreKeyMatcher
    extends GroupMatcher<TriggerKey>
{

    private static final long serialVersionUID = 1L;

    public StoreKeyMatcher( final StoreKey key, final String eventType )
    {
        super( key.toString() + ":" + eventType, StringOperatorName.EQUALS );
    }

}
