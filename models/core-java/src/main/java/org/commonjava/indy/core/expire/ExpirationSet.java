package org.commonjava.indy.core.expire;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Created by jdcasey on 1/4/16.
 */
public class ExpirationSet
    implements Iterable<Expiration>
{
    private Set<Expiration> items;

    public ExpirationSet(){}

    public ExpirationSet( Expiration... items )
    {
        this.items = new HashSet<>( Arrays.asList( items ));
    }

    public ExpirationSet( Set<Expiration> items )
    {
        this.items = items;
    }

    public Set<Expiration> getItems()
    {
        return items;
    }

    public void setItems( Set<Expiration> items )
    {
        this.items = items;
    }

    @Override
    public Iterator<Expiration> iterator()
    {
        return items == null ? Collections.emptyIterator() : items.iterator();
    }

    @Override
    public void forEach( Consumer<? super Expiration> action )
    {
        if ( items != null )
        {
            for ( Expiration exp : items )
            {
                action.accept( exp );
            }
        }
    }

    @Override
    public Spliterator<Expiration> spliterator()
    {
        return items == null ? Collections.<Expiration> emptySet().spliterator() : items.spliterator();
    }
}
