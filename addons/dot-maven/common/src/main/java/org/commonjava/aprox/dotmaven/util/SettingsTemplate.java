/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.dotmaven.util;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import net.sf.webdav.exceptions.WebdavException;

import org.commonjava.aprox.dotmaven.data.StorageAdvice;
import org.commonjava.aprox.dotmaven.webctl.RequestInfo;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.subsys.template.AproxGroovyException;
import org.commonjava.aprox.subsys.template.TemplatingEngine;

public class SettingsTemplate
{

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
        if ( url.charAt( url.length() - 1 ) != '/' )
        {
            url.append( '/' );
        }

        url.append( "api/1.0/" );
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
        catch ( final AproxGroovyException e )
        {
            throw new WebdavException( String.format( "Failed to render settings.xml template for: '%s'. Reason: %s", key,
                                       e.getMessage() ), e );
        }
    }

    public long getLength()
        throws WebdavException
    {
        formatSettings();
        return content.length;
    }
}
