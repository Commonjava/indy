package org.commonjava.indy.folo.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.hibernate.search.bridge.TwoWayStringBridge;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import java.io.IOException;

/**
 * A FieldBridge used for {@link TrackedContentEntry} storeKey field when do indexing by hibernate search.
 * Used json as the ser/de-ser way
 */
public class StoreKeyFieldBridge implements TwoWayStringBridge
{
    @Inject
    private ObjectMapper objMapper;

    public StoreKeyFieldBridge(){
        initMapper();
    }

    private void initMapper()
    {
        if ( objMapper == null )
        {
            final CDI<Object> cdi = CDI.current();
            objMapper = cdi.select( IndyObjectMapper.class ).get();
        }
    }

    @Override
    public Object stringToObject( String stringValue )
    {
        if("".equals( stringValue ))
        {
            return null;
        }
        else{
            try
            {
                return objMapper.readValue( stringValue, TrackedContentEntry.class );
            }
            catch ( IOException e )
            {
                throw new IllegalStateException( e );
            }
        }
    }

    @Override
    public String objectToString( Object object )
    {
        if(object instanceof StoreKey ){
            try
            {
                return objMapper.writeValueAsString( object );
            }
            catch ( JsonProcessingException e )
            {
                throw new IllegalStateException( e );
            }
        }
        return "";
    }
}
