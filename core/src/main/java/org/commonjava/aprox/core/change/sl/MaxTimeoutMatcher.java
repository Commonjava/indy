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
package org.commonjava.aprox.core.change.sl;

import java.util.Date;

import org.commonjava.shelflife.match.ExpirationMatcher;
import org.commonjava.shelflife.model.Expiration;

public class MaxTimeoutMatcher
    implements ExpirationMatcher
{

    private final long maxTimeout;

    public MaxTimeoutMatcher( final long maxTimeout )
    {
        this.maxTimeout = maxTimeout;
    }

    @Override
    public boolean matches( final Expiration expiration )
    {
        final long expires = expiration.getExpires();
        return expires <= maxTimeout;
    }

    @Override
    public String formatQuery()
    {
        return "expires <= " + new Date( maxTimeout );
    }

}
