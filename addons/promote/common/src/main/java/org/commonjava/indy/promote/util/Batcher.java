package org.commonjava.indy.promote.util;

import java.util.ArrayList;
import java.util.Collection;

public class Batcher
{
    public static <T> Collection<Collection<T>> batch( Collection<T> collection, int batchSize )
    {
        Collection<Collection<T>> batches = new ArrayList<>();
        Collection<T> batch = new ArrayList<>( batchSize );
        int count = 0;
        for ( T t : collection )
        {
            ( (ArrayList<T>) batch ).add( t );
            count++;
            if ( count >= batchSize )
            {
                ( (ArrayList<Collection<T>>) batches ).add( batch );
                batch = new ArrayList<>( batchSize );
                count = 0;
            }
        }
        if ( batch != null && !batch.isEmpty() )
        {
            ( (ArrayList<Collection<T>>) batches ).add( batch ); // first batch
        }
        return batches;
    }
}
