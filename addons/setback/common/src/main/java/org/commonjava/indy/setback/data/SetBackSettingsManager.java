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
package org.commonjava.indy.setback.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.change.event.AbstractStoreDeleteEvent;
import org.commonjava.indy.change.event.ArtifactStorePostUpdateEvent;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.setback.conf.SetbackConfig;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.indy.subsys.template.IndyGroovyException;
import org.commonjava.indy.subsys.template.TemplatingEngine;
import org.commonjava.indy.util.ApplicationContent;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SetBackSettingsManager
{

    private static final String TEMPLATE = "setback-settings.xml";

    private static final String DATA_DIR = "setback";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private TemplatingEngine templates;

    @Inject
    private DataFileManager manager;

    @Inject
    private SetbackConfig config;

    protected SetBackSettingsManager()
    {
    }

    public SetBackSettingsManager( final StoreDataManager storeManager, final TemplatingEngine templates,
                                   final DataFileManager manager, SetbackConfig config )
    {
        this.storeManager = storeManager;
        this.templates = templates;
        this.manager = manager;
        this.config = config;
    }

    public void deleteSettingsOnEvent( @Observes final AbstractStoreDeleteEvent event )
    {
        if ( !config.isEnabled() )
        {
            return;
        }

        for ( final Map.Entry<ArtifactStore, Transfer> storeRoot : event.getStoreRoots()
                                                                        .entrySet() )
        {
            try
            {
                deleteStoreSettings( storeRoot.getKey() );
            }
            catch ( final SetBackDataException e )
            {
                logger.error( "SetBack deletion failed.", e );
            }
        }
    }

    public boolean deleteStoreSettings( final ArtifactStore store )
        throws SetBackDataException
    {
        if ( !config.isEnabled() )
        {
            throw new SetBackDataException( "SetBack is disabled!" );
        }

        final StoreKey key = store.getKey();
        if ( StoreType.hosted == key.getType() )
        {
            return false;
        }

        final DataFile settingsXml = getSettingsXml( key );
        if ( settingsXml.exists() )
        {
            try
            {
                settingsXml.delete( new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                       "SETBACK: Deleting generated SetBack settings.xml for: " + store ) );
            }
            catch ( final IOException e )
            {
                throw new SetBackDataException(
                                                "Failed to delete SetBack settings.xml for: %s.\n  at: %s\n  Reason: %s",
                                                e, store, settingsXml, e.getMessage() );
            }

            return true;
        }

        return false;
    }

    public void updateSettingsOnEvent( @Observes final ArtifactStorePostUpdateEvent event )
    {
        if ( !config.isEnabled() )
        {
            return;
        }

        final Collection<ArtifactStore> stores = event.getChanges();
        for ( final ArtifactStore store : stores )
        {
            try
            {
                generateStoreSettings( store );
            }
            catch ( final SetBackDataException e )
            {
                logger.error( "SetBack generation failed.", e );
            }
        }
    }

    public DataFile generateStoreSettings( final ArtifactStore store )
        throws SetBackDataException
    {
        if ( !config.isEnabled() )
        {
            throw new SetBackDataException( "SetBack is disabled!" );
        }

        final StoreKey key = store.getKey();
        if ( StoreType.group == key.getType() )
        {
            return updateSettingsForGroup( (Group) store );
        }
        else if ( StoreType.remote == key.getType() )
        {
            return updateSettingsRelatedToRemote( store );
        }

        return null;
    }

    private DataFile updateSettingsRelatedToRemote( final ArtifactStore store )
        throws SetBackDataException
    {
        if ( !config.isEnabled() )
        {
            throw new SetBackDataException( "SetBack is disabled!" );
        }

        Set<Group> groups;
        try
        {
            groups = storeManager.query().getGroupsContaining( store.getKey() );
        }
        catch ( final IndyDataException e )
        {
            logger.error( String.format( "Failed to retrieve groups containing: {}. Reason: {}", store, e.getMessage() ),
                          e );
            return null;
        }

        for ( final Group group : groups )
        {
            updateSettingsForGroup( group );
        }

        return updateSettingsForRemote( (RemoteRepository) store );
    }

    private DataFile updateSettingsForRemote( final RemoteRepository store )
        throws SetBackDataException
    {
        return updateSettings( store, Collections.<ArtifactStore> singletonList( store ),
                               Collections.<RemoteRepository> singletonList( store ) );
    }

    private DataFile updateSettingsForGroup( final Group group )
        throws SetBackDataException
    {
        if ( !config.isEnabled() )
        {
            throw new SetBackDataException( "SetBack is disabled!" );
        }

        logger.info( "Updating set-back settings.xml for group: {}", group.getName() );
        List<ArtifactStore> concreteStores;
        try
        {
            concreteStores = storeManager.query().packageType( group.getPackageType() ).getOrderedConcreteStoresInGroup( group.getName() );
        }
        catch ( final IndyDataException e )
        {
            logger.error( String.format( "Failed to retrieve concrete membership for group: {}. Reason: {}",
                                         group.getName(), e.getMessage() ), e );
            return null;
        }

        final List<RemoteRepository> remotes = new ArrayList<RemoteRepository>();
        for ( final ArtifactStore cs : concreteStores )
        {
            if ( StoreType.remote == cs.getKey()
                                       .getType() )
            {
                remotes.add( (RemoteRepository) cs );
            }
        }

        return updateSettings( group, concreteStores, remotes );
    }

    private DataFile updateSettings( final ArtifactStore store, final List<ArtifactStore> allStores,
                                     final List<RemoteRepository> remotes )
        throws SetBackDataException
    {
        if ( !config.isEnabled() )
        {
            throw new SetBackDataException( "SetBack is disabled!" );
        }

        final StoreKey key = store.getKey();
        final DataFile settingsXml = getSettingsXml( key );

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put( "key", key );
        params.put( "store", store );
        params.put( "remotes", remotes );
        params.put( "allStores", allStores );

        String rendered;
        try
        {
            rendered = templates.render( ApplicationContent.application_xml, TEMPLATE, params );
        }
        catch ( final IndyGroovyException e )
        {
            throw new SetBackDataException( "Failed to render template: %s for store: %s. Reason: %s", e, TEMPLATE,
                                            key, e.getMessage() );
        }

        try
        {
            settingsXml.getParent()
                       .mkdirs();

            settingsXml.writeString( rendered, "UTF-8", new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                           "SETBACK: Updating generated SetBack settings.xml for: "
                                                                               + key ) );
        }
        catch ( final IOException e )
        {
            throw new SetBackDataException( "Failed to write SetBack settings.xml for: %s\n  to: %s\n  Reason: %s", e,
                                            key, settingsXml, e.getMessage() );
        }

        return settingsXml;
    }

    private DataFile getSettingsXml( final StoreKey key )
    {
        if ( !config.isEnabled() )
        {
            return null;
        }

        return manager.getDataFile( DATA_DIR, key.getType()
                                                 .singularEndpointName(), "settings-" + key.getName() + ".xml" );
    }

    public DataFile getSetBackSettings( final StoreKey key )
    {
        if ( !config.isEnabled() )
        {
            return null;
        }

        final DataFile settingsXml = getSettingsXml( key );
        return settingsXml == null || !settingsXml.exists() ? null : settingsXml;
    }

}
