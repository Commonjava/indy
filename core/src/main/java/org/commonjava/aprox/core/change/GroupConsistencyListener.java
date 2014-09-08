/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.core.change;

import java.security.PrivilegedAction;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.aprox.audit.SecuritySystem;
import org.commonjava.aprox.change.event.ArtifactStoreDeleteEvent;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.util.ChangeSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@javax.enterprise.context.ApplicationScoped
public class GroupConsistencyListener
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager proxyDataManager;

    @Inject
    private SecuritySystem securitySystem;

    private final ChangeSynchronizer changeSync = new ChangeSynchronizer();

    private void processChanged( final StoreKey key )
    {
        securitySystem.runAsSystemUser( new PrivilegedAction<Void>()
        {
            @Override
            public Void run()
            {
                try
                {
                    final Set<Group> groups = proxyDataManager.getGroupsContaining( key );
                    for ( final Group group : groups )
                    {
                        group.removeConstituent( key );
                        proxyDataManager.storeGroup( group, "Auto-update groups containing: " + key
                            + " (to maintain consistency)" );
                    }

                    changeSync.setChanged();
                }
                catch ( final ProxyDataException e )
                {
                    logger.error( String.format( "Failed to remove group constituent listings for: %s. Error: %s", key,
                                                 e.getMessage() ), e );
                }

                return null;
            }
        } );
    }

    // public void storeDeleted( @Observes final CouchChangeJ2EEEvent event )
    // {
    // final CouchDocChange change = event.getChange();
    // final String id = change.getId();
    //
    // final boolean canProcess =
    // change.isDeleted()
    // && ( id.startsWith( StoreType.repository.name() ) || id.startsWith( StoreType.deploy_point.name() ) ||
    // id.startsWith( StoreType.group.name() ) );
    //
    // if ( canProcess )
    // {
    // final StoreKey key = StoreKey.fromString( id );
    // processChanged( key );
    // }
    // }

    public void storeDeleted( @Observes final ArtifactStoreDeleteEvent event )
    {
        //        logger.info( "Processing proxy-manager store deletion: {}", event );
        final StoreType type = event.getType();
        for ( final String name : event )
        {
            final StoreKey key = new StoreKey( type, name );
            processChanged( key );
        }
    }

    public void waitForChange( final long total, final long poll )
    {
        changeSync.waitForChange( 1, total, poll );
    }

}
