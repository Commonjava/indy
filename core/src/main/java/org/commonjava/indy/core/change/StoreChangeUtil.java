/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.core.change;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.commonjava.maven.galley.util.PathUtils.ROOT;

public class StoreChangeUtil
{
    private static final Logger logger = LoggerFactory.getLogger( StoreChangeUtil.class );

    /**
     * Get an array of sets [s1, s2] where s1 holds the added members and s2 is for removed members.
     */
    public static Set<StoreKey>[] getDiffMembers( Group newGroup, Group oldGroup )
    {
        List<StoreKey> newMembers = newGroup.getConstituents();
        logger.debug( "New members of {}: {}", newGroup, newMembers );

        List<StoreKey> oldMembers = oldGroup.getConstituents();
        logger.debug( "Old members of {}: {}", oldGroup, oldMembers );

        return getDiff( newMembers, oldMembers );
    }

    static <T> Set<T>[] getDiff( List<T> newMembers, List<T> oldMembers )
    {
        List<T> s1 = new ArrayList<>( newMembers );
        s1.removeAll( oldMembers );
        Set<T> added = new HashSet<>( s1 );

        List<T> s2 = new ArrayList<>( oldMembers );
        s2.removeAll( newMembers );
        Set<T> removed = new HashSet<>( s2 );

        return new Set[] { added, removed };
    }

    /**
     * Get groups' members after diverged point. e.g, old = [a, b, c, d], new = [a, b, e], the result will be [c, d, e].
     * We need to improve the way to do clean-up. e.g, if some group got 700 members and we remove 1 in the middle,
     * the diverged would be 350, which is huge listing/cleaning work.
     *
     * We move to getDiffMembers and keep this method for reference for review purpose.
     */
    @Deprecated
    public static Set<StoreKey> getDivergedMembers( Group newGroup, Group oldGroup )
    {
        List<StoreKey> newMembers = newGroup.getConstituents();
        logger.debug( "New members of {}: {}", newGroup, newMembers );

        List<StoreKey> oldMembers = oldGroup.getConstituents();
        logger.debug( "Old members of {}: {}", oldGroup, oldMembers );

        return getDiverged( newMembers, oldMembers );

    }

    static <T> Set<T> getDiverged( List<T> newMembers, List<T> oldMembers )
    {
        Set<T> diverged = new HashSet<>();

        int newSize = newMembers.size();
        int oldSize = oldMembers.size();

        int min = Math.min( newSize, oldSize );

        int divergencePoint = -1;
        for ( int i = 0; i < min; i++ )
        {
            if ( !oldMembers.get( i ).equals( newMembers.get( i ) ) )
            {
                divergencePoint = i;
                break;
            }
        }
        if ( divergencePoint < 0 )
        {
            divergencePoint = min;
        }

        diverged.addAll( oldMembers.subList( divergencePoint, oldSize ) );
        diverged.addAll( newMembers.subList( divergencePoint, newSize ) );

        return diverged;
    }

    /**
     * List paths in the affected groups and execute pathAction for accepted paths.
     * @return accepted paths count
     */
    public static int listPathsAnd( Set<Group> affectedGroups, Predicate<? super String> pathFilter,
                                    BiConsumer<String, ArtifactStore> pathAction, DirectContentAccess contentAccess )
    {
        final AtomicInteger accepted = new AtomicInteger( 0 );
        affectedGroups.forEach( group -> {
            accepted.addAndGet( listPathsAnd( group.getKey(), pathFilter, ( p ) -> pathAction.accept( p, group ),
                                              contentAccess ) );
        } );
        return accepted.get();
    }

    /**
     * List paths in the target store and execute pathAction for accepted paths.
     * @return accepted paths count
     */
    public static int listPathsAnd( StoreKey key, Predicate<? super String> pathFilter, Consumer<String> pathAction,
                                     DirectContentAccess contentAccess )
    {
        final AtomicInteger accepted = new AtomicInteger( 0 );
        logger.debug( "List paths for: {}", key );
        Transfer root;
        try
        {
            root = contentAccess.getTransfer( key, ROOT );
        }
        catch ( IndyWorkflowException e )
        {
            logger.error( String.format( "Failed to retrieve root directory for: %s. Reason: %s", key, e ), e );
            return 0;
        }

        List<Transfer> toProcess = new ArrayList<>();
        toProcess.add( root );
        while ( !toProcess.isEmpty() )
        {
            Transfer next = toProcess.remove( 0 );
            try
            {
                Stream.of( next.list() ).forEach( filename -> {
                    Transfer t = next.getChild( filename );
                    if ( t.isDirectory() )
                    {
                        logger.trace( "Adding directory path for processing: {}", t.getPath() );
                        toProcess.add( t );
                    }
                    else
                    {
                        if ( pathFilter.test( t.getPath() ) )
                        {
                            logger.trace( "Accept file path: {}", t.getPath() );
                            accepted.incrementAndGet();
                            pathAction.accept( t.getPath() );
                        }
                        else
                        {
                            logger.trace( "Skipping file path: {}", t.getPath() );
                        }
                    }
                } );
            }
            catch ( IOException e )
            {
                logger.error( String.format( "Failed to list contents of: %s. Reason: %s", next, e ), e );
            }
        }
        return accepted.get();
    }

    public static boolean delete( Transfer t )
    {
        if ( t != null && t.exists() )
        {
            try
            {
                logger.info( "Deleting: {}", t );
                boolean deleted = t.delete( true );
                if ( t.exists() )
                {
                    logger.error( "{} WAS NOT DELETED!", t );
                }

                return deleted;
            }
            catch ( IOException e )
            {
                logger.error( String.format( "Failed to delete: %s. Reason: %s", t, e.getMessage() ), e );
            }
        }

        return false;
    }

}
