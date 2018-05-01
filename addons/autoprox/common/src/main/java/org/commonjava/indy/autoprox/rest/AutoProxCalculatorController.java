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
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.util.ApplicationStatus;

import static org.commonjava.indy.model.core.StoreType.*;

@ApplicationScoped
public class AutoProxCalculatorController
{

    @Inject
    private AutoProxCatalogManager catalog;

    @Inject
    private StoreDataManager dataManager;

    public AutoProxCalculation eval( final StoreKey key )
        throws IndyWorkflowException
    {
        try
        {
            if ( remote == key.getType() )
            {
                final RemoteRepository store = catalog.createRemoteRepository( key );
                if ( store != null )
                {
                    final RuleMapping mapping = catalog.getRuleMappingMatching( key );

                    return new AutoProxCalculation( store, mapping.getScriptName() );
                }
            }
            else if ( hosted == key.getType() )
            {
                final HostedRepository store = catalog.createHostedRepository( key );
                if ( store != null )
                {
                    final RuleMapping mapping = catalog.getRuleMappingMatching( key );

                    return new AutoProxCalculation( store, mapping.getScriptName() );
                }
            }
            else
            {
                // FIXME: catalog setEnable() use is NOT threadsafe!!!
                catalog.setEnabled( false );
                try
                {
                    final Group store = catalog.createGroup( key );
                    if ( store == null )
                    {
                        return null;
                    }
                    else
                    {
                        final RuleMapping mapping = catalog.getRuleMappingMatching( key );
                        final List<ArtifactStore> supplemental = new ArrayList<>();
                        evalSupplementalStores( store, supplemental );

                        return new AutoProxCalculation( store, supplemental, mapping.getScriptName() );
                    }
                }
                finally
                {
                    catalog.setEnabled( true );
                }
            }
        }
        catch ( final AutoProxRuleException e )
        {
            throw new IndyWorkflowException( ApplicationStatus.SERVER_ERROR.code(),
                                              "Failed to evaluate: '{}'. Reason: {}",
                                              e, key, e.getMessage() );
        }

        return null;
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
                        final Group g = catalog.createGroup( key );
                        if ( g != null )
                        {
                            supplemental.add( g );
                            evalSupplementalStores( g, supplemental );
                        }
                        break;
                    }
                    case remote:
                    {
                        final RemoteRepository r = catalog.createRemoteRepository( key );
                        if ( r != null )
                        {
                            supplemental.add( r );
                        }
                        break;
                    }
                    default:
                    {
                        final HostedRepository h = catalog.createHostedRepository( key );
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
