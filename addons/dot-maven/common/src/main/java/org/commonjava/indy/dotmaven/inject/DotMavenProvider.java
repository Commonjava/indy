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
package org.commonjava.indy.dotmaven.inject;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import net.sf.webdav.impl.ActivationMimeTyper;
import net.sf.webdav.impl.SimpleWebdavConfig;
import net.sf.webdav.spi.IMimeTyper;
import net.sf.webdav.spi.WebdavConfig;

import org.commonjava.indy.dotmaven.store.DotMavenStore;
import org.commonjava.indy.dotmaven.webctl.DotMavenService;
import org.commonjava.indy.dotmaven.webctl.RequestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DotMavenProvider
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private WebdavConfig config;

    private DotMavenService service;

    @Inject
    private RequestInfo requestInfo;

    @Inject
    private DotMavenStore store;

    private IMimeTyper mimeTyper;

    @Produces
    @Default
    public DotMavenService getService()
    {
        if ( service == null )
        {
            service = new DotMavenService( getConfig(), store, getMimeTyper(), requestInfo );
        }

        logger.info( "Returning WebDAV service: {}", service );
        return service;
    }

    @Produces
    @Default
    public synchronized IMimeTyper getMimeTyper()
    {
        if ( mimeTyper == null )
        {
            mimeTyper = new ActivationMimeTyper();
        }

        return mimeTyper;
    }

    @Produces
    @Default
    public synchronized WebdavConfig getConfig()
    {
        if ( config == null )
        {
            config = new SimpleWebdavConfig().withLazyFolderCreationOnPut()
                                             .withoutOmitContentLengthHeader();
        }

        return config;
    }
}
