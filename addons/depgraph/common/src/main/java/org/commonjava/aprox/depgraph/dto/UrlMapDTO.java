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
package org.commonjava.aprox.depgraph.dto;

import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.galley.CacheOnlyLocation;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.util.UriFormatter;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.ProjectVersionRefComparator;
import org.commonjava.maven.cartographer.request.RepositoryContentRequest;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class UrlMapDTO
{

    public class UrlMapProject
    {
        private String repoUrl;

        private Set<String> files;

        public UrlMapProject( final String url, final Set<String> files )
        {
            repoUrl = url;
            this.files = files;
        }

        public String getRepoUrl()
        {
            return repoUrl;
        }

        public void setRepoUrl( final String repoUrl )
        {
            this.repoUrl = repoUrl;
        }

        public Set<String> getFiles()
        {
            return files;
        }

        public void setFiles( final Set<String> files )
        {
            this.files = files;
        }
    }

    private final Map<ProjectVersionRef, UrlMapProject> projects;

    public UrlMapDTO( final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents,
                         final RepositoryContentRequest recipe, final String baseUri, final UriFormatter uriFormatter )
    {
        projects = new TreeMap<>( new ProjectVersionRefComparator() );
        for ( final ProjectVersionRef gav : contents.keySet() )
        {
            final Map<ArtifactRef, ConcreteResource> items = contents.get( gav );

            final Set<String> files = new HashSet<String>();
            KeyedLocation kl = null;

            for ( final ConcreteResource item : items.values() )
            {
                final KeyedLocation loc = (KeyedLocation) item.getLocation();

                // FIXME: we're squashing some potential variation in the locations here!
                // if we're not looking for local urls, allow any cache-only location to be overridden...
                if ( kl == null || ( !recipe.getLocalUrls() && ( kl instanceof CacheOnlyLocation ) ) )
                {
                    kl = loc;
                }

                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.debug( "Adding {} (keyLocation: {})", item, kl );
                files.add( new File( item.getPath() ).getName() );
            }

            final Set<String> sortedFiles = new TreeSet<>( files );

            final String url = formatUrlMapRepositoryUrl( kl, recipe.getLocalUrls(), baseUri, uriFormatter );

            projects.put( gav, new UrlMapProject( url, sortedFiles ) );
        }
    }

    private String formatUrlMapRepositoryUrl( final KeyedLocation kl, final boolean localUrls, final String baseUri,
                                              final UriFormatter uriFormatter )
    {
        if ( localUrls || kl instanceof CacheOnlyLocation )
        {
            final StoreKey key = kl.getKey();
            return uriFormatter.formatAbsolutePathTo( baseUri, key.getType()
                                                                  .singularEndpointName(), key.getName() );
        }
        else
        {
            return kl.getUri();
        }
    }
}
