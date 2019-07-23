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
package org.commonjava.indy.dotmaven.util;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import net.sf.webdav.exceptions.WebdavException;

import org.commonjava.indy.dotmaven.data.StorageAdvice;
import org.commonjava.indy.dotmaven.webctl.RequestInfo;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.template.IndyGroovyException;
import org.commonjava.indy.subsys.template.TemplatingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsTemplate
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final String TYPE_KEY = "type";

    public static final String NAME_KEY = "name";

    public static final String URL_KEY = "url";

    public static final String RELEASES_KEY = "releases";

    public static final String SNAPSHOTS_KEY = "snapshots";

    public static final String TEMPLATE = "settings.xml";

    private static final String DEPLOY_ENABLED_KEY = "deployEnabled";

    //    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final RequestInfo requestInfo;

    private final StoreKey key;

    private final StorageAdvice advice;

    private final TemplatingEngine templateEngine;

    private byte[] content;

    public SettingsTemplate( final StoreKey key, final StorageAdvice advice, final RequestInfo requestInfo,
                             final TemplatingEngine templateEngine )
    {
        this.templateEngine = templateEngine;
        this.key = key;
        this.advice = advice;
        this.requestInfo = requestInfo;
    }

    public byte[] getContent()
        throws WebdavException
    {
        formatSettings();
        return content;
    }

    private void formatSettings()
        throws WebdavException
    {
        if ( content != null )
        {
            return;
        }

        final String name = key.getName();
        final StoreType type = key.getType();

        final StringBuilder url = new StringBuilder();
        url.append( requestInfo.getBaseUrl() );

        logger.debug( "settings base-url is: '{}'", url );

        if ( url.charAt( url.length() - 1 ) != '/' )
        {
            url.append( '/' );
        }

        url.append( "api/" );
        url.append( type.singularEndpointName() )
           .append( '/' )
           .append( name );

        try
        {
            final Map<String, Object> params = new HashMap<String, Object>();
            params.put( TYPE_KEY, type.singularEndpointName() );
            params.put( NAME_KEY, name );
            params.put( URL_KEY, url );
            params.put( RELEASES_KEY, advice.isReleasesAllowed() );
            params.put( SNAPSHOTS_KEY, advice.isSnapshotsAllowed() );
            params.put( DEPLOY_ENABLED_KEY, advice.isDeployable() );

            final String rendered = templateEngine.render( TEMPLATE, params );

            content = rendered.getBytes( "UTF-8" );
        }
        catch ( final UnsupportedEncodingException e )
        {
            throw new IllegalStateException( "Cannot find encoding for UTF-8!", e );
        }
        catch ( final IndyGroovyException e )
        {
            throw new WebdavException( String.format( "Failed to render settings.xml template for: '%s'. Reason: %s",
                                                      key, e.getMessage() ), e );
        }
    }

    public long getLength()
        throws WebdavException
    {
        formatSettings();
        return content.length;
    }
}
