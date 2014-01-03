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
package org.commonjava.aprox.depgraph.discover;

import java.util.List;

import org.commonjava.maven.cartographer.data.CartoDataException;

public class RetryFailedException
    extends CartoDataException
{

    private static final long serialVersionUID = 1L;

    public RetryFailedException( final String message, final List<Throwable> nested, final Object... params )
    {
        super( message, nested, params );
    }

    public RetryFailedException( final String message, final Object... params )
    {
        super( message, params );
    }

    public RetryFailedException( final String message, final Throwable error, final Object... params )
    {
        super( message, error, params );
    }

}
