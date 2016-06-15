package org.commonjava.indy.content.index;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.infinispan.query.Transformer;

import java.io.IOException;

/**
 * A customized infinispan {@link org.infinispan.query.Transformer} used for {@link org.commonjava.indy.content.index.IndexedStorePath}
 * to support it to be used as infinispan cache key in indexing.
 */
public class IndexedStorePathTransformer
        implements Transformer
{
    //FIXME: not sure why the IndyObjectMapper cannot be successfully injected here, so use default objmapper instead
    //    @Inject
    //    private IndyObjectMapper objectMapper;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Object fromString( String s )
    {
        try
        {
            return objectMapper.readValue( s, IndexedStorePath.class );
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( e );
        }
    }

    @Override
    public String toString( Object customType )
    {

        if ( customType instanceof IndexedStorePath )
        {
            try
            {
                return objectMapper.writeValueAsString( customType );
            }
            catch ( JsonProcessingException e )
            {
                throw new IllegalStateException( e );
            }
        }
        else
        {
            throw new IllegalArgumentException( "Expected customType to be a " + IndexedStorePath.class.getName() );
        }
    }
}
