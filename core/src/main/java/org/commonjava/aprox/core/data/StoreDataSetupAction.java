package org.commonjava.aprox.core.data;

import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.action.AproxLifecycleException;
import org.commonjava.aprox.action.MigrationAction;
import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named( "Store-Initialization" )
public class StoreDataSetupAction
    implements MigrationAction
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager storeManager;

    @Override
    public String getId()
    {
        return "Default artifact store initilialization";
    }

    @Override
    public int getPriority()
    {
        return 95;
    }

    @Override
    public boolean migrate()
        throws AproxLifecycleException
    {
        final ChangeSummary summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, "Initializing default data." );

        boolean changed = false;
        try
        {
            logger.info( "Verfiying that AProx basic stores are installed..." );
            storeManager.install();

            if ( !storeManager.hasRemoteRepository( "central" ) )
            {
                final RemoteRepository central =
                    new RemoteRepository( "central", "http://repo.maven.apache.org/maven2/" );
                central.setCacheTimeoutSeconds( 86400 );
                storeManager.storeRemoteRepository( central, summary, true );
                changed = true;
            }

            if ( !storeManager.hasHostedRepository( "local-deployments" ) )
            {
                final HostedRepository local = new HostedRepository( "local-deployments" );
                local.setAllowReleases( true );
                local.setAllowSnapshots( true );
                local.setSnapshotTimeoutSeconds( 86400 );

                storeManager.storeHostedRepository( local, summary, true );
                changed = true;
            }

            if ( !storeManager.hasGroup( "public" ) )
            {
                final Group pub = new Group( "public" );
                pub.addConstituent( new StoreKey( StoreType.remote, "central" ) );
                pub.addConstituent( new StoreKey( StoreType.hosted, "local-deployments" ) );

                storeManager.storeGroup( pub, summary, true );
                changed = true;
            }
        }
        catch ( final AproxDataException e )
        {
            throw new RuntimeException( "Failed to boot aprox components: " + e.getMessage(), e );
        }

        return changed;
    }

}
