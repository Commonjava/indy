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

import java.util.Collection;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.aprox.change.event.ProxyManagerDeleteEvent;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.sec.data.AProxSecDataManager;
import org.commonjava.aprox.util.ChangeSynchronizer;
import org.commonjava.util.logging.Logger;

@javax.enterprise.context.ApplicationScoped
public class SecurityConsistencyListener
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private AProxSecDataManager dataManager;

    @Inject
    private ChangeSynchronizer changeSync;

    public void storeDeleted( @Observes final ProxyManagerDeleteEvent event )
    {
        logger.info( "\n\n\n\nProcessing JEE change notification: %s\n\n\n\n", event );
        final StoreType type = event.getType();
        final Collection<String> names = event.getNames();
        for ( final String name : names )
        {
            dataManager.deleteStorePermissions( type, name );
        }
    }

    public void waitForChange( final long totalMillis, final long pollingMillis )
    {
        changeSync.waitForChange( 1, totalMillis, pollingMillis );
    }

}
