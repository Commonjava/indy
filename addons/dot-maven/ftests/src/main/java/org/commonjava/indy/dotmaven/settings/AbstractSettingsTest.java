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
package org.commonjava.indy.dotmaven.settings;

import static org.apache.http.client.utils.URIUtils.resolve;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientHttp;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.module.IndyRawHttpModule;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.maven.galley.util.PathUtils;

/**
 * Base class for tests that verify dotMaven settings generation.
 */
public class AbstractSettingsTest
    extends AbstractIndyFunctionalTest
{

    protected final String getDotMavenUrl( final String path )
        throws URISyntaxException, MalformedURLException
    {
        final String mavdavPath = PathUtils.normalize( "/../mavdav", path );
        System.out.println( "Resolving dotMaven URL. Base URL: '" + client.getBaseUrl() + "'\ndotMaven path: '"
            + mavdavPath + "'" );
        
        final URI result = resolve( new URI( client.getBaseUrl() ), new URI( mavdavPath ) );
        System.out.println( "Resulting URI: '" + result.toString() + "'" );
        
        final String url = result.toURL()
                                 .toExternalForm();
        
        System.out.println( "Resulting URL: '" + url + "'" );
        return url;
    }

    /**
     * Retrieve the Indy client HTTP component, which helps with raw requests to the Indy instance.
     */
    protected final IndyClientHttp getHttp()
        throws IndyClientException
    {
        return client.module( IndyRawHttpModule.class )
                     .getHttp();
    }

    /**
     * Add the {@link IndyRawHttpModule} module to expose the raw HTTP helper component for use in these
     * tests.
     */
    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Collections.<IndyClientModule> singleton( new IndyRawHttpModule() );
    }

}
