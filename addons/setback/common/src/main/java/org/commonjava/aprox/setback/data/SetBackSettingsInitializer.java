package org.commonjava.aprox.setback.data;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.action.start.MigrationAction;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.subsys.datafile.DataFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named( "set-back-initializer" )
public class SetBackSettingsInitializer
    implements MigrationAction
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private SetBackSettingsManager settingsManager;

    protected SetBackSettingsInitializer()
    {
    }

    public SetBackSettingsInitializer( final StoreDataManager storeManager, final SetBackSettingsManager settingsManager )
    {
        this.storeManager = storeManager;
        this.settingsManager = settingsManager;
    }

    @Override
    public String getId()
    {
        return "Set-Back settings.xml initializer";
    }

    @Override
    public boolean migrate()
    {
        boolean changed = false;
        try
        {
            final List<ArtifactStore> stores = storeManager.getAllArtifactStores();

            for ( final ArtifactStore store : stores )
            {
                if ( StoreType.hosted == store.getKey()
                                              .getType() )
                {
                    continue;
                }

                final DataFile settingsXml = settingsManager.getSetBackSettings( store.getKey() );
                if ( settingsXml == null || !settingsXml.exists() )
                {
                    try
                    {
                        settingsManager.generateStoreSettings( store.getKey() );
                        changed = true;
                    }
                    catch ( final SetBackDataException e )
                    {
                        logger.error( "Failed to generate SetBack settings.xml for: " + store.getKey(), e );
                    }
                }
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to retrieve full list of ArtifactStores available on the system", e );
        }

        return changed;
    }

}
