/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.dotmaven.webctl;

import java.io.IOException;

import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
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

    @Inject
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
