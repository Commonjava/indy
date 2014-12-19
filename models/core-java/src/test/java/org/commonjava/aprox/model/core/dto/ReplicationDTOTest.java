package org.commonjava.aprox.model.core.dto;

import java.util.Arrays;

import org.commonjava.aprox.model.core.dto.ReplicationAction;
import org.commonjava.aprox.model.core.dto.ReplicationDTO;
import org.commonjava.aprox.model.core.dto.ReplicationAction.ActionType;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.junit.Test;

public class ReplicationDTOTest
{

    @Test
    public void serializeBasicReplicationRequest()
        throws Exception
    {
        final ReplicationDTO dto = new ReplicationDTO();
        dto.setApiUrl( "http://foo.com/api/1.0" );
        dto.setActions( Arrays.asList( new ReplicationAction( ActionType.PROXY ) ) );

        final String json = new AproxObjectMapper( true )
                                              .writeValueAsString( dto );
        System.out.println( json );
    }

}
