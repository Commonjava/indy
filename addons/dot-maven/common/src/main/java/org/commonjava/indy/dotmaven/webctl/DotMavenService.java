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
package org.commonjava.indy.dotmaven.webctl;

import java.io.IOException;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;

import net.sf.webdav.WebdavService;
import net.sf.webdav.exceptions.WebdavException;
import net.sf.webdav.spi.IMimeTyper;
import net.sf.webdav.spi.IWebdavStore;
import net.sf.webdav.spi.WebdavConfig;
import net.sf.webdav.spi.WebdavRequest;
import net.sf.webdav.spi.WebdavResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Alternative
@Named
public class DotMavenService
    extends WebdavService
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final String NAME = "mavdav";

    //    @Inject
    private final RequestInfo requestInfo;

    //    @Inject
    public DotMavenService( final WebdavConfig config, final IWebdavStore store, final IMimeTyper mimeTyper, final RequestInfo requestInfo )
    {
        super( config, store, mimeTyper );
        this.requestInfo = requestInfo;
    }

    @Override
    public void service( final WebdavRequest request, final WebdavResponse response )
        throws WebdavException, IOException
    {
        logger.debug( "Setting request in RequestInfo: {}", requestInfo );

        //        final String mount = request.getParameter( RequestInfo.MOUNT_POINT );
        requestInfo.setRequest( request );

        super.service( request, response );
    }

}
