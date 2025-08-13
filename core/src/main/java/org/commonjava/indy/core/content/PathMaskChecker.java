/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PathMaskChecker
{
    private static final Logger logger = LoggerFactory.getLogger( PathMaskChecker.class );

    public static boolean checkMask(final ArtifactStore repo, final String path){
        Set<String> maskPatterns = repo.getPathMaskPatterns();
        logger.trace( "Checking mask in: {}, patterns: {}", repo.getKey(), maskPatterns );

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
                final String realRegex = pattern.substring( 2, pattern.length() - 1 );
                if ( path.matches( realRegex ) )
                {
                    logger.trace( "Checking mask in: {}, pattern with regex: {} - MATCH", repo.getName(), realRegex );
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
        logger.trace( "Checking mask in: {}, patterns: {}", store.getKey(), maskPatterns );

        if (maskPatterns == null || maskPatterns.isEmpty())
        {
            logger.trace( "Checking mask in: {}, - NO PATTERNS", store.getName() );
            return true;
        }

        // if the pattern contains the metadata path, let's try to extract the groupId to filter the repos.
        List<String> metadataPatterns = maskPatterns.stream()
                .filter(pattern -> pattern.endsWith("maven-metadata.xml"))
                .collect( Collectors.toList() );

        if ( metadataPatterns.isEmpty() )
        {
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
        }
        else
        {
            boolean matches = metadataPatterns.stream()
                    .map(pattern -> extractGroupIdPath(pattern))
                    .filter( Objects::nonNull )
                    .anyMatch(groupIdPath -> path.startsWith( groupIdPath ) || groupIdPath.startsWith( path ) );

            if ( matches )
            {
                logger.trace( "Checking mask in: {}, pattern with groupId path. - MATCH.", store.getName() );
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

    public static String extractGroupIdPath(String metadataPath)
    {
        if (metadataPath == null || !metadataPath.endsWith("maven-metadata.xml"))
        {
            return null;
        }

        // Need at least groupId/artifactId/filename
        String[] parts = metadataPath.split("/");
        if (parts.length < 3) return null;

        return String.join("/", Arrays.copyOf(parts, parts.length - 2)) + "/";
    }

    public static boolean checkMavenMetadataMask( final ArtifactStore store, final String path )
    {
        Set<String> maskPatterns = store.getPathMaskPatterns();
        logger.trace( "Checking metadata mask in: {}, patterns: {}", store.getKey(), maskPatterns );

        if ( maskPatterns == null || maskPatterns.isEmpty() )
        {
            logger.trace( "Checking mask in: {}, - NO PATTERNS", store.getName() );
            return true;
        }

        for ( String pattern : maskPatterns )
        {
            if ( isRegexPattern( pattern ) )
            {
                continue; // metadata patterns are listed as full paths, not use regex pattern
            }
            else if ( path.startsWith( pattern ) || pattern.startsWith( path ) )
            {
                logger.trace( "Checking mask in: {}, pattern: {} - MATCH", store.getName(), pattern );
                return true;
            }
        }

        logger.debug( "Metadata patterns not matched, path: {}, patterns: {}, repo: {}", path, maskPatterns,
                      store.getKey() );
        return false;
    }

    public static boolean isRegexPattern( String pattern )
    {
        return pattern != null && pattern.startsWith( "r|" ) && pattern.endsWith( "|" );
    }
}
