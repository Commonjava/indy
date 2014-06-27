package org.commonjava.aprox.core.dto;

import java.util.Arrays;

import org.commonjava.aprox.core.dto.ReplicationAction.ActionType;
import org.commonjava.aprox.model.io.StoreKeySerializer;
import org.commonjava.web.json.ser.JsonSerializer;
import org.junit.Test;

public class ReplicationDTOTest
{

    @Test
    public void serializeBasicReplicationRequest()
    {
        final ReplicationDTO dto = new ReplicationDTO();
        dto.setApiUrl( "http://foo.com/api/1.0" );
        dto.setActions( Arrays.asList( new ReplicationAction( ActionType.PROXY ) ) );

        final String json = new JsonSerializer( new StoreKeySerializer() ).toString( dto );
        System.out.println( json );
    }

}
