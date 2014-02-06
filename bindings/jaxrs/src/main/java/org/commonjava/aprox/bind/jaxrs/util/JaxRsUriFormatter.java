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

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.UriBuilder;

import org.commonjava.aprox.bind.jaxrs.RESTApplication;
import org.commonjava.aprox.core.util.UriFormatter;

public class JaxRsUriFormatter
    implements UriFormatter
{

    private static final ApplicationPath APP_PATH = RESTApplication.class.getAnnotation( ApplicationPath.class );

    private final UriBuilder builder;

    public JaxRsUriFormatter( final UriBuilder builder )
    {
        this.builder = builder;
    }

    @Override
    public String formatAbsolutePathTo( final String base, final String... parts )
    {
        UriBuilder b = builder.path( APP_PATH.value() );

        b = b.path( base );
        for ( final String part : parts )
        {
            b = b.path( part );
        }

        return b.build()
                .toString();
    }
}
