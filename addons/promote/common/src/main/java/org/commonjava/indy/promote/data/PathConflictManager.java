package org.commonjava.indy.promote.data;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreKeyPaths;
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

    private final Map<StoreKey, Set<StoreKeyPaths>> conflictsMap = new HashMap<>();

    public PathsPromoteResult checkAnd( StoreKeyPaths k, Function<StoreKeyPaths, PathsPromoteResult> function,
                                        Function<StoreKeyPaths, PathsPromoteResult> conflictedFunction )
    {
        Set<StoreKeyPaths> conflicts = null;
        boolean conflicted = false;
        try
        {
            logger.debug( "Check paths conflict for {}", k );
            synchronized ( conflictsMap )
            {
                conflicts = conflictsMap.get( k.getTarget() );
                if ( conflicts == null )
                {
                    conflicts = new HashSet<>();
                    conflicts.add( k );
                    conflictsMap.put( k.getTarget(), conflicts );
                }
                else
                {
                    conflicted = hasConflict( k.getPaths(), conflicts );
                    if ( !conflicted )
                    {
                        conflicts.add( k );
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
            synchronized ( conflictsMap )
            {
                conflicts.remove( k );
            }
        }
    }

    private boolean hasConflict( Set<String> paths, Set<StoreKeyPaths> conflicts )
    {
        for ( StoreKeyPaths keyPaths : conflicts )
        {
            Set<String> s = keyPaths.getPaths();
            // if anyone specify null paths (which means to lock whole store) or if the two have elements in common
            if ( s == null || paths == null || !disjoint( paths, s ) )
            {
                logger.debug( "Conflict detected, key: {}, paths: {}, conflict: {}", keyPaths.getTarget(), paths, s );
                return true;
            }
        }
        return false;
    }
}
