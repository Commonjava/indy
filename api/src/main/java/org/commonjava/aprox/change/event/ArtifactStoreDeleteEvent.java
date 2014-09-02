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

import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreType;

/**
 * Event signaling the deletion of one or more {@link ArtifactStore} instances. This event will always contain the same type of store, when there is
 * more than one. Instance names are collected and available via getNames(), while the store type is available separately via the getType() method.
 */
public class ArtifactStoreDeleteEvent
    implements Iterable<String>, AproxEvent
{

    private final StoreType type;

    private final Collection<String> names;

    public ArtifactStoreDeleteEvent( final StoreType type, final Collection<String> names )
    {
        this.type = type;
        this.names = Collections.unmodifiableCollection( names );
    }

    public ArtifactStoreDeleteEvent( final StoreType type, final String... names )
    {
        this.names = Collections.unmodifiableCollection( Arrays.asList( names ) );
        this.type = type;
    }

    /**
     * Return the type of store described in this event.
     */
    public StoreType getType()
    {
        return type;
    }

    /**
     * Iterate over the store names described in this event.
     */
    @Override
    public Iterator<String> iterator()
    {
        return names.iterator();
    }

    /**
     * Return the store names described in this event.
     */
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
