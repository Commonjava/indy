package org.commonjava.indy.promote.data;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.promote.model.PathsPromoteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.disjoint;

@ApplicationScoped
public class PathConflictManager
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Map<StoreKey, Set<StoreKeyPaths>> inUseMap = new HashMap<>();

    public PathsPromoteResult checkAnd( StoreKeyPaths k, Function<StoreKeyPaths, PathsPromoteResult> function,
                                        Function<StoreKeyPaths, PathsPromoteResult> conflictedFunction )
    {
        Set<StoreKeyPaths> inUse = null;
        boolean conflicted = false;
        try
        {
            logger.debug( "Check paths conflict for {}", k );
            synchronized ( inUseMap )
            {
                inUse = inUseMap.get( k.getTarget() );
                if ( inUse == null )
                {
                    inUse = new HashSet<>();
                    inUse.add( k );
                    inUseMap.put( k.getTarget(), inUse );
                }
                else
                {
                    conflicted = hasConflict( k.getPaths(), inUse );
                    if ( !conflicted )
                    {
                        inUse.add( k );
                    }
                }
            }

            logger.debug( "Check done, conflicted: {}", conflicted );
            if ( conflicted )
            {
                return conflictedFunction.apply( k );
            }
            else
            {
                return function.apply( k );
            }
        }
        finally
        {
            // clean up
            synchronized ( inUseMap )
            {
                inUse.remove( k );
                if ( inUse.isEmpty() )
                {
                    inUseMap.remove( k.getTarget() );
                }
            }
        }
    }

    private boolean hasConflict( Set<String> paths, Set<StoreKeyPaths> inUse )
    {
        for ( StoreKeyPaths keyPaths : inUse )
        {
            Set<String> s = keyPaths.getPaths();
            if ( !disjoint( paths, s ) )
            {
                logger.warn( "Conflict detected, key: {}, paths: {}, inUse: {}", keyPaths.getTarget(), paths, s );
                return true;
            }
        }
        return false;
    }
}
