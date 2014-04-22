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
package org.commonjava.aprox.bind.jaxrs.util;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.util.UriFormatter;

public class JaxRsUriFormatter
    implements UriFormatter
{

    //    private static final ApplicationPath APP_PATH = RESTApplication.class.getAnnotation( ApplicationPath.class );

    private final UriInfo info;

    public JaxRsUriFormatter( final UriInfo info )
    {
        this.info = info;
    }

    @Override
    public String formatAbsolutePathTo( final String base, final String... parts )
    {
        UriBuilder b = info.getBaseUriBuilder(); //.path( APP_PATH.value() );

        if ( base != null && base.trim()
                                 .length() > 0 )
        {
            b = b.path( base );
        }

        for ( final String part : parts )
        {
            if ( part == null || part.trim()
                                     .length() < 1 )
            {
                continue;
            }

            b = b.path( part );
        }

        return b.build()
                .toString();
    }
}
