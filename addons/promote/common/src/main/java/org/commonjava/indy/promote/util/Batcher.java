package org.commonjava.indy.promote.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Batcher
{
    public static <T> Collection<Collection<T>> batch( Collection<T> collection, int batchSize )
    {
        Collection<Collection<T>> batches = new ArrayList<>();
        List<T> batch = new ArrayList<>( batchSize );
        int count = 0;
        for ( T t : collection )
        {
            batch.add( t );
            count++;
            if ( count >= batchSize )
            {
                batches.add( batch );
                batch = new ArrayList<>( batchSize );
                count = 0;
            }
        }
        if ( !batch.isEmpty() )
        {
            batches.add( batch ); // first or last batch
        }
        return batches;
    }
}
