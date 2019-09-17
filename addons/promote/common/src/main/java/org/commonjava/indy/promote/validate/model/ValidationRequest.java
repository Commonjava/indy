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
package org.commonjava.indy.promote.validate.model;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.StoreResource;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.promote.model.PathsPromoteRequest;
import org.commonjava.indy.promote.model.PromoteRequest;
import org.commonjava.indy.promote.model.ValidationRuleSet;
import org.commonjava.indy.promote.validate.PromotionValidationException;
import org.commonjava.indy.promote.validate.PromotionValidationTools;
import org.commonjava.maven.galley.model.Transfer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by jdcasey on 9/11/15.
 */
public class ValidationRequest
{
    private final PromoteRequest promoteRequest;

    private final ValidationRuleSet ruleSet;

    private final PromotionValidationTools tools;

    private Set<String> requestPaths;

    private ArtifactStore sourceRepository;

    public ValidationRequest( PromoteRequest promoteRequest, ValidationRuleSet ruleSet, PromotionValidationTools tools, ArtifactStore sourceRepository )
    {
        this.promoteRequest = promoteRequest;
        this.ruleSet = ruleSet;
        this.tools = tools;
        this.sourceRepository = sourceRepository;
    }
    public synchronized Set<String> getSourcePaths()
            throws PromotionValidationException
    {
        return getSourcePaths(false, false);
    }

    public synchronized Set<String> getSourcePaths( boolean includeMetadata, boolean includeChecksums )
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
                if ( sourceRepository != null )
                {
                    try
                    {
                        paths = new HashSet<>();
                        listRecursively( sourceRepository, "/", paths );
                    }
                    catch ( IndyWorkflowException e )
                    {
                        throw new PromotionValidationException( "Failed to list paths in source: {}. Reason: {}", e,
                                                                promoteRequest.getSource(), e.getMessage() );
                    }
                }
            }
            requestPaths = paths;
        }

        if ( !includeMetadata || !includeChecksums )
        {
            Predicate<String> filter = ( path ) -> ( includeMetadata || !path.matches( ".+/maven-metadata\\.xml(\\.(md5|sha[0-9]+))?" ) )
                    && ( includeChecksums || !path.matches( ".+\\.(md5|sha[0-9]+)" ) );
            return requestPaths.stream().filter( filter ).collect( Collectors.toSet() );
        }

        return requestPaths;
    }

    private void listRecursively( ArtifactStore store, String path, Set<String> paths )
            throws IndyWorkflowException
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
        return sourceRepository.getKey();
    }

    public StoreKey getTarget()
    {
        return promoteRequest.getTargetKey();
    }

    public ArtifactStore getSourceRepository()
    {
        return sourceRepository;
    }
}
