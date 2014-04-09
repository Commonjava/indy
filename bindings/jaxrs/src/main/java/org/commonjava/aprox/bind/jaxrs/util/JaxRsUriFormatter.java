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
