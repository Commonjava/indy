package org.commonjava.indy.core.inject;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Created by ruhan on 12/1/17.
 */
public abstract class AbstractNotFoundCache implements NotFoundCache
{

    public Map<Location, Set<String>> getAllMissing( int pageIndex, int pageSize )
    {
        return Collections.emptyMap();
    }

    public Set<String> getMissing( Location location, int pageIndex, int pageSize )
    {
        return Collections.emptySet();
    }

    abstract public long getSize( StoreKey storeKey );

    abstract public long getSize();
}
