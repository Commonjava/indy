package org.commonjava.aprox.model.core.dto;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;

public class StoreListingDTOTest
{

    @Test
    public void deserializeStoreListingDTO()
        throws Exception
    {
        /* @formatter:off */
        final String json = "{\n" + 
            "  \"items\" : [ {\n" + 
            "    \"type\": \"hosted\",\n" + 
            "    \"key\" : \"hosted:eSPeEZPG\",\n" + 
            "    \"snapshotTimeoutSeconds\" : 0,\n" + 
            "    \"name\" : \"eSPeEZPG\",\n" + 
            "    \"doctype\" : \"hosted\",\n" + 
            "    \"allow_snapshots\" : false,\n" + 
            "    \"allow_releases\" : true\n" + 
            "  }, {\n" + 
            "    \"type\": \"hosted\",\n" + 
            "    \"key\" : \"hosted:qI3Cq2OZ\",\n" + 
            "    \"snapshotTimeoutSeconds\" : 0,\n" + 
            "    \"name\" : \"qI3Cq2OZ\",\n" + 
            "    \"doctype\" : \"hosted\",\n" + 
            "    \"allow_snapshots\" : false,\n" + 
            "    \"allow_releases\" : true\n" + 
            "  }, {\n" + 
            "    \"type\": \"hosted\",\n" + 
            "    \"key\" : \"hosted:local-deployments\",\n" + 
            "    \"snapshotTimeoutSeconds\" : 86400,\n" + 
            "    \"name\" : \"local-deployments\",\n" + 
            "    \"doctype\" : \"hosted\",\n" + 
            "    \"allow_snapshots\" : true,\n" + 
            "    \"allow_releases\" : true\n" + 
            "  }, {\n" + 
            "    \"type\": \"hosted\",\n" + 
            "    \"key\" : \"hosted:AFhJnQLW\",\n" + 
            "    \"snapshotTimeoutSeconds\" : 0,\n" + 
            "    \"name\" : \"AFhJnQLW\",\n" + 
            "    \"doctype\" : \"hosted\",\n" + 
            "    \"allow_snapshots\" : false,\n" + 
            "    \"allow_releases\" : true\n" + 
            "  } ]\n" + 
            "}";
        /* @formatter:on */

        final StoreListingDTO<HostedRepository> value =
            new AproxObjectMapper( true ).readValue( json, new TypeReference<StoreListingDTO<HostedRepository>>()
            {
            } );

        final List<HostedRepository> items = value.getItems();
        assertThat( items.size(), equalTo( 4 ) );

        int i = 0;
        assertThat( items.get( i++ )
                         .getName(), equalTo( "eSPeEZPG" ) );
        assertThat( items.get( i++ )
                         .getName(), equalTo( "qI3Cq2OZ" ) );
        assertThat( items.get( i++ )
                         .getName(), equalTo( "local-deployments" ) );
        assertThat( items.get( i++ )
                         .getName(), equalTo( "AFhJnQLW" ) );
    }
}
