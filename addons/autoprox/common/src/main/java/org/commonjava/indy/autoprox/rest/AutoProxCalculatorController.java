/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.autoprox.rest;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.autoprox.data.AutoProxCatalogManager;
import org.commonjava.indy.autoprox.data.AutoProxRuleException;
import org.commonjava.indy.autoprox.data.RuleMapping;
import org.commonjava.indy.autoprox.rest.dto.AutoProxCalculation;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.util.ApplicationStatus;

@ApplicationScoped
public class AutoProxCalculatorController
{

    @Inject
    private AutoProxCatalogManager catalog;

    @Inject
    private StoreDataManager dataManager;

    public AutoProxCalculation evalRemoteRepository( final String name )
        throws IndyWorkflowException
    {
        try
        {
            final RemoteRepository store = catalog.createRemoteRepository( name );
            if ( store != null )
            {
                final RuleMapping mapping = catalog.getRuleMappingMatching( name );

                return new AutoProxCalculation( store, mapping.getScriptName() );
            }
        }
        catch ( final AutoProxRuleException e )
        {
            throw new IndyWorkflowException( ApplicationStatus.SERVER_ERROR.code(),
                                              "Failed to evaluate: '{}'. Reason: {}",
                                              e, name, e.getMessage() );
        }

        return null;
    }

    public AutoProxCalculation evalHostedRepository( final String name )
        throws IndyWorkflowException
    {
        try
        {
            final HostedRepository store = catalog.createHostedRepository( name );
            if ( store != null )
            {
                final RuleMapping mapping = catalog.getRuleMappingMatching( name );

                return new AutoProxCalculation( store, mapping.getScriptName() );
            }
        }
        catch ( final AutoProxRuleException e )
        {
            throw new IndyWorkflowException( ApplicationStatus.SERVER_ERROR.code(),
                                              "Failed to evaluate: '{}'. Reason: {}",
                                              e, name, e.getMessage() );
        }

        return null;
    }

    // FIXME: catalog setEnable() use is NOT threadsafe!!!
    public AutoProxCalculation evalGroup( final String name )
        throws IndyWorkflowException
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
                final RuleMapping mapping = catalog.getRuleMappingMatching( name );
                final List<ArtifactStore> supplemental = new ArrayList<ArtifactStore>();
                evalSupplementalStores( store, supplemental );

                return new AutoProxCalculation( store, supplemental, mapping.getScriptName() );
            }
        }
        catch ( final AutoProxRuleException e )
        {
            throw new IndyWorkflowException( ApplicationStatus.SERVER_ERROR.code(),
                                              "Failed to evaluate: '{}'. Reason: {}",
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

}
