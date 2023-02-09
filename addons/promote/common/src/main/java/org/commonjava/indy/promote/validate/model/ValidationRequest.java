/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.StoreResource;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.promote.model.PathsPromoteRequest;
import org.commonjava.indy.promote.model.PromoteRequest;
import org.commonjava.indy.promote.model.ValidationRuleSet;
import org.commonjava.indy.promote.validate.PromotionValidationException;
import org.commonjava.indy.promote.validate.PromotionValidationTools;
import org.commonjava.indy.util.RequestContextHelper;
import org.commonjava.maven.galley.model.Transfer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
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

    private final ArtifactStore sourceRepository;

    private static final Predicate<String> DEFAULT_FILTER =
        getMetadataPredicate().negate().and( getChecksumPredicate().negate() );

    private static final String VERSION_PATTERN = "versionPattern";
    private static final String SCOPED_VERSION_PATTERN = "scopedVersionPattern";

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
        return getSourcePaths( false, false, DEFAULT_FILTER );
    }

    public synchronized Set<String> getSourcePaths( boolean includeMetadata, boolean includeChecksums )
            throws PromotionValidationException
    {
        Predicate<String> metadata = asPredicate( includeMetadata ).or( getMetadataPredicate().negate() );
        Predicate<String> checksums = asPredicate( includeChecksums ).or( getChecksumPredicate().negate() );
        return getSourcePaths( includeMetadata, includeChecksums ,metadata.and( checksums ) );
    }

    private Set<String> getSourcePaths( boolean includeMetadata,
            boolean includeChecksums , Predicate<String> filter )
            throws PromotionValidationException
    {
        if ( requestPaths == null )
        {
            Set<String> paths = null;
            if ( promoteRequest instanceof PathsPromoteRequest )
            {
                paths = ( (PathsPromoteRequest) promoteRequest ).getPaths();
            }

            if ( paths == null || paths.isEmpty())
            {
                if ( sourceRepository != null )
                {
                    try
                    {
                        paths = new HashSet<>();
                        // This is used to let galley ignore the NPMPathStorageCalculator handling,
                        // which will append package.json to a directory transfer and make listing not applicable.
                        ThreadContext context = ThreadContext.getContext( true );
                        context.put( RequestContextHelper.IS_RAW_VIEW, Boolean.TRUE );
                        listRecursively( sourceRepository, "/", paths );
                        context.put( RequestContextHelper.IS_RAW_VIEW, Boolean.FALSE );
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

    private Predicate<String> asPredicate( boolean value ) {
        return ( path ) -> value;
    }

    private static Predicate<String> getMetadataPredicate () {
        return Pattern.compile( ".+/maven-metadata\\.xml(\\.(md5|sha[0-9]+))?" ).asPredicate();
    }

    private static Predicate<String> getChecksumPredicate () {
        return Pattern.compile( ".+\\.(md5|sha[0-9]+)" ).asPredicate();
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

    public Pattern getVersionPattern( String key )
    {
        return ruleSet.getVersionPattern( key );
    }

    public Pattern getVersionPattern()
    {
        return ruleSet.getVersionPattern( VERSION_PATTERN );
    }

    public Pattern getScopedVersionPattern( String key )
    {
        return ruleSet.getScopedVersionPattern( key );
    }

    public Pattern getScopedVersionPattern()
    {
        return ruleSet.getScopedVersionPattern( SCOPED_VERSION_PATTERN );
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
