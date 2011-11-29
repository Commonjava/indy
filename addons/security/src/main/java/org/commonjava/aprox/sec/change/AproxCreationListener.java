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

import static org.commonjava.aprox.core.change.event.ProxyManagerUpdateType.ADD;
import static org.commonjava.aprox.core.change.event.ProxyManagerUpdateType.ADD_OR_UPDATE;

import java.util.Collection;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.aprox.core.change.event.ArtifactStoreUpdateEvent;
import org.commonjava.aprox.core.data.ProxyAppDescription;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.auth.couch.data.UserDataException;
import org.commonjava.auth.couch.data.UserDataManager;
import org.commonjava.auth.couch.inject.UserData;
import org.commonjava.auth.couch.model.Permission;
import org.commonjava.couch.change.j2ee.ApplicationEvent;
import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.util.logging.Logger;

@Singleton
public class AproxCreationListener
{

    private static final String[] READONLY_PERMS = {
        Permission.ADMIN,
        Permission.READ };

    private static final String[] ALL_PERMS = {
        Permission.ADMIN,
        Permission.READ,
        Permission.CREATE };

    private final Logger logger = new Logger( getClass() );

    @Inject
    private UserDataManager userMgr;

    @Inject
    @UserData
    private CouchDBConfiguration userConfig;

    public void storeEvent( @Observes final ArtifactStoreUpdateEvent event )
    {
        if ( ADD == event.getType() || ADD_OR_UPDATE == event.getType() )
        {
            Collection<ArtifactStore> stores = event.getChanges();
            for ( ArtifactStore store : stores )
            {
                try
                {
                    String[] verbs = store.getDoctype().isWritable() ? ALL_PERMS : READONLY_PERMS;

                    logger.info( "Creating permissions for new store: %s", store );
                    userMgr.createPermissions( store.getDoctype().name(), store.getName(), verbs );
                }
                catch ( UserDataException e )
                {
                    logger.error( "Failed to create permissions for store: %s. Error: %s", e,
                                  store.getKey(), e.getMessage() );
                }
            }
        }
    }

    public void installEvent( @Observes final ApplicationEvent appEvent )
    {
        if ( appEvent.getDescription() instanceof ProxyAppDescription )
        {
            try
            {
                userMgr.install();
                userMgr.setupAdminInformation();

                userMgr.storePermission( new Permission( StoreType.repository.name(),
                                                         Permission.ADMIN ) );

                userMgr.storePermission( new Permission( StoreType.group.name(), Permission.ADMIN ) );

                userMgr.storePermission( new Permission( StoreType.repository.name(),
                                                         Permission.READ ) );

                userMgr.storePermission( new Permission( StoreType.group.name(), Permission.READ ) );
            }
            catch ( UserDataException e )
            {
                logger.error( "Failed to initialize admin user/privilege information in database: %s. Reason: %s",
                              e, userConfig.getDatabaseUrl(), e.getMessage() );
            }
        }
    }

}
