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
package org.commonjava.aprox.dotmaven.webctl;

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

@Alternative
@Named
public class DotMavenService
    extends WebdavService
{
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
        //        final String mount = request.getParameter( RequestInfo.MOUNT_POINT );
        requestInfo.setRequest( request );

        super.service( request, response );
    }

}
