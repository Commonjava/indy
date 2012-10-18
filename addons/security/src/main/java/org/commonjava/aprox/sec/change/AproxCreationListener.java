/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
