/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.content.index.conf;

import org.commonjava.indy.change.event.ArtifactStorePostUpdateEvent;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Collection;

@ApplicationScoped
public class AuthoritativeIndexSettingManager
{
    @Inject
    private ContentIndexConfig indexCfg;

    protected AuthoritativeIndexSettingManager()
    {
    }

    public AuthoritativeIndexSettingManager( final ContentIndexConfig indexCfg )
    {
        this.indexCfg = indexCfg;
    }

    public void setAuthoritativeManager( @Observes final ArtifactStorePostUpdateEvent event )
    {
        if ( indexCfg == null || !indexCfg.isAuthoritativeIndex() )
        {
            return;
        }

        final Collection<ArtifactStore> stores = event.getChanges();
        stores.stream().filter( store -> store.getKey().getType() == StoreType.hosted )
              .forEach( store ->
                        {
                            final HostedRepository hosted = (HostedRepository) store;
                            if ( hosted.isReadonly() )
                            {
                                hosted.setAuthoritativeIndex( true );
                            }
                            else
                            {
                                hosted.setAuthoritativeIndex( false );
                            }
                        } );
    }
}
