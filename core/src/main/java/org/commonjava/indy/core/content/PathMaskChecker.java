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
package org.commonjava.indy.core.content;

import org.commonjava.indy.model.core.ArtifactStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class PathMaskChecker
{
    private static final Logger logger = LoggerFactory.getLogger( PathMaskChecker.class );

    public static boolean checkMask(final ArtifactStore repo, final String path){
        Set<String> maskPatterns = repo.getPathMaskPatterns();

        logger.trace( "Checking mask in: {}, type: {}", repo.getName(), repo.getKey().getType() );
        logger.trace( "Mask patterns in {}: {}", repo.getName(), maskPatterns );

        if (maskPatterns == null || maskPatterns.isEmpty())
        {
            logger.trace( "Checking mask in: {}, - NO PATTERNS", repo.getName() );
            return true;
        }

        for ( String pattern : maskPatterns )
        {
            // adding allPlaintext to the condition to reduce the number of isRegexPattern() calls
            if ( isRegexPattern( pattern ) )
            {
                if ( path.matches( pattern.substring( 2, pattern.length() - 1 ) ) )
                {
                    return true;
                }
            }
            else if ( path.startsWith( pattern ) )
            {
                logger.trace( "Checking mask in: {}, pattern: {} - MATCH", repo.getName(), pattern );
                return true;
            }
        }

        logger.debug( "Path {} not available in path mask {} of repo {}", path, maskPatterns, repo );

        return false;
    }

    // path mask checking
    public static boolean checkListingMask( final ArtifactStore store, final String path )
    {
        Set<String> maskPatterns = store.getPathMaskPatterns();

        logger.trace( "Checking mask in: {}", store.getKey() );
        logger.trace( "Mask patterns in {}: {}", store.getName(), maskPatterns );

        if (maskPatterns == null || maskPatterns.isEmpty())
        {
            logger.trace( "Checking mask in: {}, - NO PATTERNS", store.getName() );
            return true;
        }

        for ( String pattern : maskPatterns )
        {
            if ( isRegexPattern( pattern ) )
            {
                // if there is a regexp pattern we cannot check presence of directory listing, because we would have to
                // check only the beginning of the regexp and that's impossible, so we have to assume that the path is
                // present
                return true;
            }
        }

        for ( String pattern : maskPatterns )
        {
            if ( path.startsWith( pattern ) || pattern.startsWith( path ) )
            {
                logger.trace( "Checking mask in: {}, pattern: {} - MATCH", store.getName(), pattern );
                return true;
            }
        }

        logger.debug( "Listing for path {} not enabled by path mask {} of repo {}", path, maskPatterns, store.getKey() );

        return false;
    }

    public static boolean isRegexPattern( String pattern )
    {
        return pattern != null && pattern.startsWith( "r|" ) && pattern.endsWith( "|" );
    }
}
