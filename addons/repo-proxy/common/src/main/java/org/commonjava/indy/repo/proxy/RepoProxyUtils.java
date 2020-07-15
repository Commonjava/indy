/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.repo.proxy;

import org.apache.commons.lang3.StringUtils;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.pkg.PackageTypeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.commonjava.indy.repo.proxy.RepoProxyAddon.ADDON_NAME;
import static org.commonjava.maven.galley.util.PathUtils.normalize;

public class RepoProxyUtils
{
    private static final Logger logger = LoggerFactory.getLogger( RepoProxyUtils.class );

    private static final String STORE_PATH_PATTERN = ".*/(maven|npm)/(group|hosted)/(.+?)(/.*)?";

    // For legacy indy content api: /api/$repoType/$name/*
    private static final String STORE_PATH_PATTERN_NO_PKG = ".*/(group|hosted)/(.+?)(/.*)?";

    private static final String NPM_METADATA_NAME = "package.json";

    static Optional<String> getProxyTo( final String originalPath, final StoreKey proxyToKey )
    {
        if ( StoreType.remote != proxyToKey.getType() )
        {
            return empty();
        }
        final Optional<String> origStoreKey = getOriginalStoreKeyFromPath( originalPath );
        if ( origStoreKey.isPresent() )
        {
            final String originStoreKeyStr = origStoreKey.get();
            final StoreKey originStoreKey = StoreKey.fromString( originStoreKeyStr );
            if ( !originStoreKey.getPackageType().equals( proxyToKey.getPackageType() ) )
            {
                logger.warn(
                        "The proxy to store has different package type with original store: original: {}, proxyTo: {}",
                        originStoreKey.getPackageType(), proxyToKey.getPackageType() );
                return empty();
            }
            final String proxyTo = replaceAllWithNoRegex( originalPath, noPkgStorePath( originStoreKeyStr ),
                                                          noPkgStorePath( proxyToKey ) );
            logger.trace( "Found proxy to store rule: from {} to {}", originStoreKeyStr, proxyToKey );
            return of( proxyTo );
        }

        return empty();
    }

    static String noPkgStorePath( StoreKey key )
    {
        return String.format( "%s/%s", key.getType(), key.getName() );
    }

    static String noPkgStorePath( String keyString )
    {
        final StoreKey key = StoreKey.fromString( keyString );
        return noPkgStorePath( key );
    }

    static Optional<String> getOriginalStoreKeyFromPath( final String originalPath )
    {
        Pattern pat = Pattern.compile( STORE_PATH_PATTERN );
        Matcher match = pat.matcher( originalPath );
        String storeKeyString = null;
        if ( match.matches() )
        {
            storeKeyString = String.format( "%s:%s:%s", match.group( 1 ), match.group( 2 ), match.group( 3 ) );
            logger.trace( "Found matched original store key {} in path {}", storeKeyString, originalPath );
        }
        else
        {
            // Tweak for legacy content path: the legacy content path is like /api/$type/$name/* which does not contain package type, so
            // here we treat this type of content patch specially.
            pat = Pattern.compile( STORE_PATH_PATTERN_NO_PKG );
            match = pat.matcher( originalPath );
            if ( match.matches() )
            {
                final String pkgType = PackageTypeConstants.PKG_TYPE_MAVEN;
                storeKeyString = String.format( "%s:%s:%s", pkgType, match.group( 1 ), match.group( 2 ) );
                logger.trace( "Found matched original store key {} by legacy content path in path {}", storeKeyString,
                              originalPath );
            }
            else
            {
                logger.trace( "There is not matched original store key in path {}", originalPath );
            }
        }

        return ofNullable( storeKeyString );
    }

    static String extractPath( final String fullPath )
    {
        if ( StringUtils.isBlank( fullPath ) )
        {
            return "";
        }

        final Pattern pat = Pattern.compile( STORE_PATH_PATTERN_NO_PKG );
        final Matcher match = pat.matcher( fullPath );
        if ( match.matches() )
        {
            final String pkgType = PackageTypeConstants.PKG_TYPE_MAVEN;
            final String repoPath = String.format( "%s/%s", match.group( 1 ), match.group( 2 ) );
            String checkingRepoPath = repoPath;
            if ( repoPath.endsWith( "/" ) )
            {
                checkingRepoPath = repoPath.substring( 0, repoPath.length() - 1 );
            }
            final int pos = fullPath.indexOf( checkingRepoPath );
            final int pathStartPos = pos + checkingRepoPath.length() + 1;
            if ( pathStartPos >= fullPath.length() )
            {
                return "";
            }
            String path = fullPath.substring( pathStartPos );
            if ( StringUtils.isNotBlank( path ) && !path.startsWith( "/" ) )
            {
                path = "/" + path;
            }
            return path;
        }
        return "";
    }

    static boolean isNPMMetaPath( final String path )
    {
        if ( StringUtils.isBlank( path ) )
        {
            return false;
        }
        String checkingPath = path;
        if ( path.startsWith( "/" ) )
        {
            checkingPath = path.substring( 1 );
        }
        // This is considering the single path for npm standard like "/jquery"
        final boolean isSinglePath = checkingPath.split( "/" ).length < 2;
        // This is considering the scoped path for npm standard like "/@type/jquery"
        final boolean isScopedPath = checkingPath.startsWith( "@" ) && checkingPath.split( "/" ).length < 3;
        // This is considering the package.json file itself
        final boolean isPackageJson = checkingPath.trim().endsWith( "/" + NPM_METADATA_NAME );

        trace( logger, "path: {}, isSinglePath: {}, isScopedPath: {}, isPackageJson: {}", path, isSinglePath,
               isScopedPath, isPackageJson );
        return isSinglePath || isScopedPath || isPackageJson;
    }

    static String getRequestAbsolutePath( HttpServletRequest request )
    {
        return normalize( request.getServletPath(), request.getContextPath(), request.getPathInfo() );
    }

    public static void trace( final Logger logger, final String template, final Object... params )
    {
        if ( logger.isTraceEnabled() )
        {
            final String finalTemplate = ADDON_NAME + ": " + template;
            logger.trace( finalTemplate, params );
        }
    }

    public static String replaceAllWithNoRegex( String originalStr, String target, String replacement )
    {
        String result = originalStr;
        while ( result.indexOf( target ) > 0 )
        {
            result = result.replace( target, replacement );
        }
        return result;
    }
}
