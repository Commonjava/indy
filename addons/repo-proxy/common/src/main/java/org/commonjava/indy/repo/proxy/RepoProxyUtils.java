/**
 * Copyright (C) 2013 Red Hat, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.repo.proxy;

import org.apache.commons.lang3.StringUtils;
import org.commonjava.indy.model.core.StoreKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.commonjava.indy.repo.proxy.RepoProxyAddon.ADDON_NAME;

public class RepoProxyUtils
{
    private static final Logger logger = LoggerFactory.getLogger( RepoProxyUtils.class );

    private static final String STORE_PATH_PATTERN = ".*/(maven|npm)/(group|hosted)/(.+?)(/.*)?";

    public static Optional<String> proxyTo( final String originalPath, final String originStoreKeyStr,
                                            Map<String, String> configRules )
    {
        final String proxyToStoreString = configRules.get( originStoreKeyStr );
        if ( StringUtils.isNotBlank( proxyToStoreString ) )
        {
            logger.trace( "Found proxy to store rule: from {} to {}", originStoreKeyStr, proxyToStoreString );
            return of( ( originalPath.replace( originStoreKeyStr.replaceAll( ":", "/" ),
                                               proxyToStoreString.replaceAll( ":", "/" ) ) ) );
        }
        else
        {
            return empty();
        }
    }

    public static Optional<StoreKey> getProxyToStoreKey( final String originStoreKey, Map<String, String> configRules )
    {
        final String proxyToStoreString = configRules.get( originStoreKey );
        try
        {
            logger.trace( "Found proxy to store: {}", proxyToStoreString );
            return StringUtils.isBlank( proxyToStoreString ) ?
                    empty() :
                    of( StoreKey.fromString( proxyToStoreString ) );
        }
        catch ( IllegalArgumentException e )
        {
            return empty();
        }
    }

    public static Optional<String> getOriginalStoreKeyFromPath( final String originalPath )
    {
        final Pattern pat = Pattern.compile( STORE_PATH_PATTERN );
        final Matcher match = pat.matcher( originalPath );
        String storeKeyString = null;
        if ( match.matches() )
        {
            storeKeyString = String.format( "%s:%s:%s", match.group( 1 ), match.group( 2 ), match.group( 3 ) );
            logger.trace( "Found matched original store key {} in path {}", storeKeyString, originalPath );
        }
        else
        {
            logger.trace( "There is not matched original store key in path {}", originalPath );
        }

        return storeKeyString == null ? empty() : of( storeKeyString );
    }

    public static String extractPath( final String fullPath, final String repoPath )
    {
        if ( StringUtils.isBlank( fullPath ) || !fullPath.contains( repoPath ) )
        {
            return "";
        }
        String checkingRepoPath = repoPath;
        if ( repoPath.endsWith( "/" ) )
        {
            checkingRepoPath = repoPath.substring( 0, repoPath.length() - 1 );
        }
        final int pos = fullPath.indexOf( checkingRepoPath );
        String path = fullPath.substring( pos + checkingRepoPath.length() + 1 );
        if ( StringUtils.isNotBlank( path ) && !path.startsWith( "/" ) )
        {
            path = "/" + path;
        }
        return path;
    }

    public static void trace( final Logger logger, final String template, final Object... params )
    {
        final String finalTemplate = ADDON_NAME + ": " + template;
        logger.trace( finalTemplate, params );
    }
}
