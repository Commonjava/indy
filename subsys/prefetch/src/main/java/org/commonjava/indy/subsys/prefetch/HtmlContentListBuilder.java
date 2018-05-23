/**
 * Copyright (C) 2013 Red Hat, Inc.
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
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.ListingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.commonjava.indy.model.core.RemoteRepository.PREFETCH_LISTING_TYPE_HTML;
import static org.commonjava.indy.model.core.RemoteRepository.PREFETCH_LISTING_TYPE_KOJI;

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
    public List<ConcreteResource> buildContent( RemoteRepository repository )
    {
        if ( repository.getPrefetchPriority() <= 0 )
        {
            logger.error( "The repository {} prefetch disabled, can not use html content listing",
                          repository.getName() );
            return Collections.emptyList();
        }
        if ( PREFETCH_LISTING_TYPE_KOJI.equals( repository.getPrefetchListingType() ) )
        {
            logger.warn( "Will start to use normal html content listing for the koji proxy remote {}",
                         repository.getName() );
        }

        final String rootPath = "/";
        final KeyedLocation loc = LocationUtils.toLocation( repository );
        final StoreResource res = new StoreResource( loc, rootPath );
        try
        {
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
        return Collections.emptyList();

    }

    @Override
    public String type()
    {
        return PREFETCH_LISTING_TYPE_HTML;
    }
}
