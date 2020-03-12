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
package org.commonjava.indy.repo.proxy;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.commonjava.indy.repo.proxy.RepoProxyAddon.ADDON_NAME;

public class RepoProxyUtils
{
    private static final Logger logger = LoggerFactory.getLogger( RepoProxyUtils.class );

    public static String proxyTo( final String originalPath, Map<String, String> configRules )
    {
        for ( Map.Entry<String, String> rule : configRules.entrySet() )
        {
            trace( logger, "rule key ({}), rule value ({})", rule.getKey(), rule.getValue() );
            if ( originalPath.indexOf( rule.getKey() ) > 0 )
            {

                trace( logger, "found proxy rules for path {}: from {} to {}", originalPath, rule.getKey(),
                       rule.getValue() );
                return originalPath.replace( rule.getKey(), rule.getValue() );

            }
        }
        trace( logger, "no proxy rules for path {}, will not do any proxy", originalPath );
        return null;
    }

    public static String extractPath( final String fullPath, final String repoPath )
    {
        if ( StringUtils.isBlank( fullPath ) || !fullPath.contains( repoPath ) )
        {
            return "";
        }
        final int pos = fullPath.indexOf( repoPath );
        final boolean ruleInPath = pos >= 0;
        return fullPath.substring( pos + repoPath.length() + 1 );
    }

    public static void trace( final Logger logger, final String template, final Object... params )
    {
        final String finalTemplate = ADDON_NAME + ": " + template;
        logger.trace( finalTemplate, params );
    }
}
