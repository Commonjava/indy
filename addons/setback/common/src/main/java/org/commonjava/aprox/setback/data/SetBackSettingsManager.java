package org.commonjava.aprox.setback.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.commonjava.aprox.change.event.ArtifactStoreDeleteEvent;
import org.commonjava.aprox.change.event.ArtifactStoreUpdateEvent;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.subsys.flatfile.conf.FlatFileConfiguration;
import org.commonjava.aprox.subsys.template.AproxGroovyException;
import org.commonjava.aprox.subsys.template.TemplatingEngine;
import org.commonjava.aprox.util.ApplicationContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SetBackSettingsManager
{

    private static final String TEMPLATE = "setback-settings.xml";

    private static final String DATA_DIR = "setback";

    private static final String SETTINGS_XML_FILENAME = "settings.xml";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private TemplatingEngine templates;

    @Inject
    private FlatFileConfiguration config;

    protected SetBackSettingsManager()
    {
    }

    public SetBackSettingsManager( final StoreDataManager storeManager, final TemplatingEngine templates,
                                   final FlatFileConfiguration config )
    {
        this.storeManager = storeManager;
        this.templates = templates;
        this.config = config;
    }

    public void deleteSettingsOnEvent( @Observes final ArtifactStoreDeleteEvent event )
    {
        final StoreType type = event.getType();
        final Set<String> names = new HashSet<String>( event.getNames() );
        for ( final String name : names )
        {
            final StoreKey key = new StoreKey( type, name );
            try
            {
                deleteStoreSettings( key );
            }
            catch ( final SetBackDataException e )
            {
                logger.error( "SetBack deletion failed.", e );
            }
        }
    }

    public boolean deleteStoreSettings( final StoreKey key )
        throws SetBackDataException
    {
        if ( StoreType.hosted == key.getType() )
        {
            return false;
        }

        final File dataDir = config.getDataDir( DATA_DIR );
        final File settingsXml = Paths.get( dataDir.getPath(), key.getType()
                                                                  .singularEndpointName(), key.getName() )
                                      .toFile();
        if ( settingsXml.exists() )
        {
            try
            {
                FileUtils.forceDelete( settingsXml );
            }
            catch ( final IOException e )
            {
                throw new SetBackDataException(
                                                "Failed to delete SetBack settings.xml for: %s.\n  at: %s\n  Reason: %s",
                                                e, key, settingsXml, e.getMessage() );
            }

            return true;
        }

        return false;
    }

    public void updateSettingsOnEvent( @Observes final ArtifactStoreUpdateEvent event )
    {
        final Collection<ArtifactStore> stores = event.getChanges();
        for ( final ArtifactStore store : stores )
        {
            try
            {
                generateStoreSettings( store.getKey() );
            }
            catch ( final SetBackDataException e )
            {
                logger.error( "SetBack generation failed.", e );
            }
        }
    }

    public File generateStoreSettings( final StoreKey key )
        throws SetBackDataException
    {
        if ( StoreType.group == key.getType() )
        {
            return updateSettingsForGroup( key );
        }
        else if ( StoreType.remote == key.getType() )
        {
            return updateSettingsRelatedToRemote( key );
        }

        return null;
    }

    private File updateSettingsRelatedToRemote( final StoreKey key )
        throws SetBackDataException
    {
        Set<Group> groups;
        try
        {
            groups = storeManager.getGroupsContaining( key );
        }
        catch ( final ProxyDataException e )
        {
            logger.error( String.format( "Failed to retrieve groups containing: {}. Reason: {}", key,
                                         e.getMessage() ), e );
            return null;
        }

        for ( final Group group : groups )
        {
            updateSettingsForGroup( group.getKey() );
        }

        return updateSettingsForRemote( key );
    }

    private File updateSettingsForRemote( final StoreKey key )
        throws SetBackDataException
    {
        RemoteRepository store;
        try
        {
            store = storeManager.getRemoteRepository( key.getName() );
        }
        catch ( final ProxyDataException e )
        {
            throw new SetBackDataException( "Failed to lookup remote repository: %s. Reason: %s", e, key,
                                            e.getMessage() );
        }

        return updateSettings( key, Collections.<ArtifactStore> singletonList( store ),
                               Collections.<RemoteRepository> singletonList( store ) );
    }

    private File updateSettingsForGroup( final StoreKey key )
        throws SetBackDataException
    {
        logger.info( "Updating set-back settings.xml for group: {}", key.getName() );
        List<ArtifactStore> concreteStores;
        try
        {
            concreteStores = storeManager.getOrderedConcreteStoresInGroup( key.getName() );
        }
        catch ( final ProxyDataException e )
        {
            logger.error( String.format( "Failed to retrieve concrete membership for group: {}. Reason: {}",
                                         key.getName(), e.getMessage() ), e );
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

        return updateSettings( key, concreteStores, remotes );
    }

    private File updateSettings( final StoreKey key, final List<ArtifactStore> allStores,
                                 final List<RemoteRepository> remotes )
        throws SetBackDataException
    {
        final File settingsXml = getSettingsXml( key );

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put( "key", key );
        params.put( "remotes", remotes );
        params.put( "allStores", allStores );

        String rendered;
        try
        {
            rendered = templates.render( ApplicationContent.application_xml, TEMPLATE, params );
        }
        catch ( final AproxGroovyException e )
        {
            throw new SetBackDataException( "Failed to render template: %s for store: %s. Reason: %s", e, TEMPLATE,
                                            key, e.getMessage() );
        }

        try
        {
            settingsXml.getParentFile()
                       .mkdirs();

            FileUtils.write( settingsXml, rendered );
        }
        catch ( final IOException e )
        {
            throw new SetBackDataException( "Failed to write SetBack settings.xml for: %s\n  to: %s\n  Reason: %s", e,
                                            key, settingsXml, e.getMessage() );
        }

        return settingsXml;
    }

    private File getSettingsXml( final StoreKey key )
    {
        final File dataDir = config.getDataDir( DATA_DIR );
        return Paths.get( dataDir.getPath(), key.getType()
                                                .singularEndpointName(), key.getName(), SETTINGS_XML_FILENAME )
                    .toFile();
    }

    public File getSetBackSettings( final StoreKey key )
    {
        final File settingsXml = getSettingsXml( key );
        return settingsXml == null || !settingsXml.exists() ? null : settingsXml;
    }

}
