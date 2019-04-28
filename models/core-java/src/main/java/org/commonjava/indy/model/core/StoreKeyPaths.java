package org.commonjava.indy.model.core;

import java.util.Objects;
import java.util.Set;

public class StoreKeyPaths
{
    final StoreKey target;

    final Set<String> paths;

    public StoreKeyPaths( StoreKey target, Set<String> paths )
    {
        this.target = target;
        this.paths = paths;
    }

    public StoreKey getTarget()
    {
        return target;
    }

    public Set<String> getPaths()
    {
        return paths;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
            return true;
        if ( o == null || getClass() != o.getClass() )
            return false;
        StoreKeyPaths that = (StoreKeyPaths) o;
        return target.equals( that.target ) && Objects.equals( paths, that.paths );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( target, paths );
    }

    @Override
    public String toString()
    {
        return "PathsLockKey{" + "target=" + target + ", paths=" + paths + '}';
    }
}
