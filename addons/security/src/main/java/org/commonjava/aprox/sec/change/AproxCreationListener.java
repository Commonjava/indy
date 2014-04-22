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
package org.commonjava.aprox.sec.change;

import static org.commonjava.aprox.change.event.ProxyManagerUpdateType.ADD;
import static org.commonjava.aprox.change.event.ProxyManagerUpdateType.ADD_OR_UPDATE;

import java.util.Collection;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.aprox.change.event.ArtifactStoreUpdateEvent;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.sec.data.AProxSecDataManager;

@javax.enterprise.context.ApplicationScoped
public class AproxCreationListener
{

    @Inject
    private AProxSecDataManager dataManager;

    public void storeEvent( @Observes final ArtifactStoreUpdateEvent event )
    {
        if ( ADD == event.getType() || ADD_OR_UPDATE == event.getType() )
        {
            final Collection<? extends ArtifactStore> stores = event.getChanges();
            for ( final ArtifactStore store : stores )
            {
                dataManager.createStorePermissions( store );
            }
        }
    }

}
