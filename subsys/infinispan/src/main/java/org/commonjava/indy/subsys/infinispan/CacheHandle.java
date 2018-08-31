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
package org.commonjava.indy.subsys.infinispan;

import com.codahale.metrics.Timer;
import org.commonjava.indy.metrics.IndyMetricsManager;
import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Holder class that helps manage the shutdown process for things that use Infinispan.
 */
public class CacheHandle<K,V> extends BasicCacheHandle<K,V>
{
    protected CacheHandle(){}

    public CacheHandle( String named, Cache<K, V> cache, IndyMetricsManager metricsManager, String metricPrefix )
    {
        super( named, cache, metricsManager, metricPrefix );
    }

    public CacheHandle( String named, Cache<K, V> cache )
    {
        this( named, cache, null, null );
    }

    public <R> R executeCache( Function<Cache<K, V>, R> operation )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        if ( !isStopped() )
        {
            Timer.Context context = startMetrics( "execute" );

            try
            {
                return (R) operation.apply( (Cache) cache );
            }
            catch ( RuntimeException e )
            {
                logger.error( "Failed to complete operation: " + e.getMessage(), e );
            }
            finally
            {
                if ( context != null )
                {
                    context.stop();
                }
            }
        }
        else
        {
            logger.error( "Cannot complete operation. Cache {} is shutting down.", getName() );
        }

        return null;
    }

    public void beginTransaction()
            throws NotSupportedException, SystemException
    {
        Timer.Context context = startMetrics( "beginTransaction" );

        try
        {
            AtomicReference<NotSupportedException> suppEx = new AtomicReference<>();
            AtomicReference<SystemException> sysEx = new AtomicReference<>();
            executeCache( ( c ) -> {
                try
                {
                    c.getAdvancedCache().getTransactionManager().begin();
                }
                catch ( NotSupportedException e )
                {
                    suppEx.set( e );
                }
                catch ( SystemException e )
                {
                    sysEx.set( e );
                }

                return null;
            } );

            if ( suppEx.get() != null )
            {
                throw suppEx.get();
            }

            if ( sysEx.get() != null )
            {
                throw sysEx.get();
            }
        }
        finally
        {
            if ( context != null )
            {
                context.stop();
            }
        }
    }

    public void rollback()
            throws SystemException
    {
        Timer.Context context = startMetrics( "rollback" );

        try
        {
            AtomicReference<SystemException> sysEx = new AtomicReference<>();
            executeCache( ( c ) -> {
                try
                {
                    c.getAdvancedCache().getTransactionManager().rollback();
                }
                catch ( SystemException e )
                {
                    sysEx.set( e );
                }

                return null;
            } );


            if ( sysEx.get() != null )
            {
                throw sysEx.get();
            }
        }
        finally
        {
            if ( context != null )
            {
                context.stop();
            }
        }
    }

    public void commit()
            throws SystemException, HeuristicMixedException, HeuristicRollbackException, RollbackException
    {
        Timer.Context context = startMetrics( "commit" );
        try
        {
            AtomicReference<SystemException> sysEx = new AtomicReference<>();
            AtomicReference<HeuristicMixedException> hmEx = new AtomicReference<>();
            AtomicReference<HeuristicRollbackException> hrEx = new AtomicReference<>();
            AtomicReference<RollbackException> rEx = new AtomicReference<>();
            executeCache( ( c ) -> {
                try
                {
                    c.getAdvancedCache().getTransactionManager().commit();
                }
                catch ( SystemException e )
                {
                    sysEx.set( e );
                }
                catch ( HeuristicMixedException e )
                {
                    hmEx.set( e );
                }
                catch ( HeuristicRollbackException e )
                {
                    hrEx.set( e );
                }
                catch ( RollbackException e )
                {
                    rEx.set( e );
                }

                return null;
            } );

            if ( sysEx.get() != null )
            {
                throw sysEx.get();
            }

            if ( hmEx.get() != null )
            {
                throw hmEx.get();
            }

            if ( hrEx.get() != null )
            {
                throw hrEx.get();
            }

            if ( rEx.get() != null )
            {
                throw rEx.get();
            }
        }
        finally
        {
            if ( context != null )
            {
                context.stop();
            }
        }
    }

    public int getTransactionStatus()
            throws SystemException
    {
        AtomicReference<SystemException> sysEx = new AtomicReference<>();

        Integer result = executeCache( ( c ) -> {
            try
            {
                return c.getAdvancedCache().getTransactionManager().getStatus();
            }
            catch ( SystemException e )
            {
                sysEx.set( e );
            }

            return null;
        } );

        if ( sysEx.get() != null )
        {
            throw sysEx.get();
        }

        return result;
    }

    public Object getLockOwner( K key )
    {
        return executeCache( ( c ) -> c.getAdvancedCache().getLockManager().getOwner( key ) );
    }

    public boolean isLocked( K key )
    {
        return executeCache( ( c ) -> c.getAdvancedCache().getLockManager().isLocked( key ) );
    }

    public void lock( K... keys )
    {
        executeCache( ( c ) -> c.getAdvancedCache().lock( keys ) );
    }

}
