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
package org.commonjava.aprox.change.event;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.commonjava.aprox.model.StoreType;

public class ProxyManagerDeleteEvent
    implements Iterable<String>, AproxEvent
{

    private final StoreType type;

    private final Collection<String> names;

    public ProxyManagerDeleteEvent( final StoreType type, final Collection<String> names )
    {
        this.type = type;
        this.names = Collections.unmodifiableCollection( names );
    }

    public ProxyManagerDeleteEvent( final StoreType type, final String... names )
    {
        this.names = Collections.unmodifiableCollection( Arrays.asList( names ) );
        this.type = type;
    }

    public StoreType getType()
    {
        return type;
    }

    @Override
    public Iterator<String> iterator()
    {
        return names.iterator();
    }

    public Collection<String> getNames()
    {
        return names;
    }

    @Override
    public String toString()
    {
        return String.format( "ProxyManagerDeleteEvent [type={}, names={}]", type, names );
    }

}
