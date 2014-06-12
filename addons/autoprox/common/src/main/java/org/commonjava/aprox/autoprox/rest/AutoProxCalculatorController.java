package org.commonjava.aprox.autoprox.rest;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.autoprox.data.AutoProxCatalog;
import org.commonjava.aprox.autoprox.data.AutoProxRuleException;
import org.commonjava.aprox.autoprox.rest.dto.AutoProxCalculation;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.util.ApplicationStatus;

@ApplicationScoped
public class AutoProxCalculatorController
{

    @Inject
    private AutoProxCatalog catalog;

    @Inject
    private StoreDataManager dataManager;

    public AutoProxCalculation evalRemoteRepository( final String name )
        throws AproxWorkflowException
    {
        try
        {
            final RemoteRepository store = catalog.createRemoteRepository( name );
            return store == null ? null : new AutoProxCalculation( store );
        }
        catch ( final AutoProxRuleException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to evaluate: '{}'. Reason: {}",
                                              e, name, e.getMessage() );
        }
    }

    public AutoProxCalculation evalHostedRepository( final String name )
        throws AproxWorkflowException
    {
        try
        {
            final HostedRepository store = catalog.createHostedRepository( name );
            return store == null ? null : new AutoProxCalculation( store );
        }
        catch ( final AutoProxRuleException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to evaluate: '{}'. Reason: {}",
                                              e, name, e.getMessage() );
        }
    }

    // FIXME: catalog setEnable() use is NOT threadsafe!!!
    public AutoProxCalculation evalGroup( final String name )
        throws AproxWorkflowException
    {
        catalog.setEnabled( false );
        try
        {
            final Group store = catalog.createGroup( name );
            if ( store == null )
            {
                return null;
            }
            else
            {
                final List<ArtifactStore> supplemental = new ArrayList<ArtifactStore>();
                evalSupplementalStores( store, supplemental );

                return new AutoProxCalculation( store, supplemental );
            }
        }
        catch ( final AutoProxRuleException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to evaluate: '{}'. Reason: {}",
                                              e, name, e.getMessage() );
        }
        finally
        {
            catalog.setEnabled( true );
        }
    }

    private void evalSupplementalStores( final Group store, final List<ArtifactStore> supplemental )
        throws AutoProxRuleException
    {
        for ( final StoreKey key : store.getConstituents() )
        {
            if ( !dataManager.hasArtifactStore( key ) )
            {
                final String name = key.getName();

                switch ( key.getType() )
                {
                    case group:
                    {
                        final Group g = catalog.createGroup( name );
                        if ( g != null )
                        {
                            supplemental.add( g );
                            evalSupplementalStores( g, supplemental );
                        }
                        break;
                    }
                    case remote:
                    {
                        final RemoteRepository r = catalog.createRemoteRepository( name );
                        if ( r != null )
                        {
                            supplemental.add( r );
                        }
                        break;
                    }
                    default:
                    {
                        final HostedRepository h = catalog.createHostedRepository( name );
                        if ( h != null )
                        {
                            supplemental.add( h );
                        }
                    }
                }
            }
        }
    }

    public RemoteRepository evalGroupValidationRepo( final String name )
        throws AproxWorkflowException
    {
        try
        {
            return catalog.createGroupValidationRemote( name );
        }
        catch ( final AutoProxRuleException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to evaluate: '{}'. Reason: {}",
                                              e, name, e.getMessage() );
        }
    }

    public String evalRepositoryValidationPath( final String name )
        throws AproxWorkflowException
    {
        try
        {
            return catalog.getRemoteValidationUrl( name );
        }
        catch ( final AutoProxRuleException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to evaluate: '{}'. Reason: {}",
                                              e, name, e.getMessage() );
        }
    }

}
