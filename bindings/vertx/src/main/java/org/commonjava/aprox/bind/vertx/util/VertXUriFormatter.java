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
package org.commonjava.aprox.bind.vertx.util;

import javax.enterprise.context.RequestScoped;

import org.commonjava.aprox.core.util.UriFormatter;
import org.commonjava.maven.galley.util.PathUtils;

@RequestScoped
public class VertXUriFormatter
    implements UriFormatter
{

    @Override
    public String formatAbsolutePathTo( final String base, final String... parts )
    {
        final String[] arry = new String[parts.length + 1];
        arry[0] = base;
        System.arraycopy( parts, 0, arry, 1, parts.length );

        return PathUtils.normalize( arry );
    }

}
