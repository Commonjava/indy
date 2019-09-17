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
package org.commonjava.indy.subsys.prefetch;

import org.commonjava.indy.content.StoreResource;
import org.commonjava.indy.core.content.PathMaskChecker;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.ListingResult;
import org.omg.PortableInterceptor.USER_EXCEPTION;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.commonjava.indy.model.core.RemoteRepository.PREFETCH_LISTING_TYPE_HTML;

/**
 * The normal and default content list builder, which will build the content list from a web page in external repo.
 */
@ApplicationScoped
public class HtmlContentListBuilder
        implements ContentListBuilder
{
    private static final Logger logger = LoggerFactory.getLogger( HtmlContentListBuilder.class );

    @Inject
    private TransferManager transferManager;

    public HtmlContentListBuilder()
    {
    }

    public HtmlContentListBuilder( final TransferManager transferManager )
    {
        this.transferManager = transferManager;
    }

    @Override
    public List<ConcreteResource> buildContent( final RemoteRepository repository, final boolean isRescan )
    {
        if ( repository.getPrefetchPriority() <= 0 )
        {
            logger.warn( "The repository {} prefetch disabled, can not use html content listing",
                         repository.getName() );
            return Collections.emptyList();
        }

        final Set<String> pathMasks = repository.getPathMaskPatterns();
        boolean useDefault = true;
        if ( pathMasks != null && !pathMasks.isEmpty() )
        {
            useDefault = pathMasks.stream().anyMatch( p -> PathMaskChecker.isRegexPattern( p ) );
        }

        if ( useDefault )
        {
            final String rootPath = "/";
            final KeyedLocation loc = LocationUtils.toLocation( repository );
            final StoreResource res = new StoreResource( loc, rootPath );
            try
            {
                // If this is a rescan prefetch, we need to clear the listing cache and re-fetch from external
                if ( isRescan )
                {
                    transferManager.delete( new StoreResource( loc, "/.listing.txt" ) );
                }
                final ListingResult lr = transferManager.list( res );
                if ( lr != null && lr.getListing() != null )
                {
                    String[] files = lr.getListing();
                    List<ConcreteResource> resources = new ArrayList<>( files.length );
                    for ( final String file : lr.getListing() )
                    {
                        resources.add( new StoreResource( loc, rootPath, file ) );
                    }
                    return resources;
                }
            }
            catch ( TransferException e )
            {
                logger.error( String.format( "Can not get transfer for repository %s", repository ), e );
            }
        }
        else
        {
            // if all path mask patterns are plaintext, we will use these as the download list directly.
            return pathMasks.stream()
                            .map( p -> new StoreResource( LocationUtils.toLocation( repository ), p ) )
                            .collect( Collectors.toList() );
        }
        return Collections.emptyList();

    }

    @Override
    public String type()
    {
        return PREFETCH_LISTING_TYPE_HTML;
    }
}
