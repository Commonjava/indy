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
package org.commonjava.aprox.promote.validate.model;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.content.StoreResource;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.promote.model.PathsPromoteRequest;
import org.commonjava.aprox.promote.model.PromoteRequest;
import org.commonjava.aprox.promote.model.ValidationRuleSet;
import org.commonjava.aprox.promote.validate.PromotionValidationException;
import org.commonjava.aprox.promote.validate.PromotionValidationTools;
import org.commonjava.maven.galley.model.Transfer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by jdcasey on 9/11/15.
 */
public class ValidationRequest
{
    private final PromoteRequest promoteRequest;

    private final ValidationRuleSet ruleSet;

    private final PromotionValidationTools tools;

    private Set<String> requestPaths;

    public ValidationRequest( PromoteRequest promoteRequest, ValidationRuleSet ruleSet, PromotionValidationTools tools )
    {
        this.promoteRequest = promoteRequest;
        this.ruleSet = ruleSet;
        this.tools = tools;
    }

    public synchronized Set<String> getSourcePaths()
            throws PromotionValidationException
    {
        if ( requestPaths == null )
        {
            Set<String> paths = null;
            if ( promoteRequest instanceof PathsPromoteRequest )
            {
                paths = ( (PathsPromoteRequest) promoteRequest ).getPaths();
            }

            if ( paths == null )
            {
                ArtifactStore store = null;
                try
                {
                    store = tools.getArtifactStore( promoteRequest.getSource() );
                }
                catch ( AproxDataException e )
                {
                    throw new PromotionValidationException( "Failed to retrieve source ArtifactStore: {}. Reason: {}",
                                                            e, promoteRequest.getSource(), e.getMessage() );
                }

                if ( store != null )
                {
                    try
                    {
                        paths = new HashSet<>();
                        listRecursively( store, "/", paths );
                    }
                    catch ( AproxWorkflowException e )
                    {
                        throw new PromotionValidationException( "Failed to list paths in source: {}. Reason: {}", e,
                                                                promoteRequest.getSource(), e.getMessage() );
                    }
                }
            }
            requestPaths = paths;
        }

        return requestPaths;
    }

    private void listRecursively( ArtifactStore store, String path, Set<String> paths )
            throws AproxWorkflowException
    {
        List<StoreResource> listing = tools.list( store, path );
        if ( listing != null )
        {
            for ( StoreResource res : listing )
            {
                if ( res != null )
                {
                    Transfer txfr = tools.getTransfer( res );
                    if ( txfr != null )
                    {
                        if ( txfr.isDirectory() )
                        {
                            listRecursively( store, txfr.getPath(), paths );
                        }
                        else if ( txfr.exists() )
                        {
                            paths.add( txfr.getPath() );
                        }
                    }
                }
            }
        }
    }

    public PromoteRequest getPromoteRequest()
    {
        return promoteRequest;
    }

    public ValidationRuleSet getRuleSet()
    {
        return ruleSet;
    }

    public PromotionValidationTools getTools()
    {
        return tools;
    }

    public String getValidationParameter( String key )
    {
        return ruleSet.getValidationParameter( key );
    }

    public StoreKey getSource()
    {
        return promoteRequest.getSource();
    }

    public StoreKey getTarget()
    {
        return promoteRequest.getTargetKey();
    }
}
