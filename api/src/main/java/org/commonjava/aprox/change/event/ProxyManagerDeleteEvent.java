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
        return String.format( "ProxyManagerDeleteEvent [type=%s, names=%s]", type, names );
    }

}
