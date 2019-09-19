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
package org.commonjava.indy.bind.jaxrs.util;

import static org.apache.commons.lang.StringUtils.join;

import java.net.MalformedURLException;

import org.commonjava.indy.model.core.PackageTypeDescriptor;
import org.commonjava.indy.model.core.PackageTypes;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.util.UriFormatter;
import org.commonjava.atlas.maven.ident.util.JoinString;
import org.commonjava.maven.galley.util.PathUtils;
import org.commonjava.maven.galley.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriInfo;

public class JaxRsUriFormatter
                implements UriFormatter
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public String formatAbsolutePathTo( final String base, final String... parts )
    {
        logger.debug( "Formatting URL from base: '{}' and parts: {}", base, new JoinString( ", ", parts ) );

        String url = null;
        try
        {
            url = UrlUtils.buildUrl( base, parts );
        }
        catch ( final MalformedURLException e )
        {
            logger.warn( "Failed to use UrlUtils to build URL from base: {} and parts: {}", base, join( parts, ", " ) );
            url = PathUtils.normalize( base, PathUtils.normalize( parts ) );
        }

        if ( url.length() > 0 && !url.matches( "[a-zA-Z0-9]+\\:\\/\\/.+" ) && url.charAt( 0 ) != '/' )
        {
            url = "/" + url;
        }

        logger.debug( "Resulting URL: '{}'", url );

        return url;
    }

    public static String getBaseUrlByStoreKey( UriInfo uriInfo, StoreKey storeKey )
    {
        PackageTypeDescriptor typeDescriptor = PackageTypes.getPackageTypeDescriptor( storeKey.getPackageType() );
        return uriInfo.getBaseUriBuilder()
                      .path( typeDescriptor.getContentRestBasePath() )
                      .path( storeKey.getType().singularEndpointName() )
                      .path( storeKey.getName() )
                      .build()
                      .toString();
    }
}
