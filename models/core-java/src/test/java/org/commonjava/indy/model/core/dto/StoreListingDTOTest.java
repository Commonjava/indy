/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.model.core.dto;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
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
            "    \"key\" : \"maven:hosted:eSPeEZPG\",\n" +
            "    \"snapshotTimeoutSeconds\" : 0,\n" + 
            "    \"name\" : \"eSPeEZPG\",\n" + 
            "    \"doctype\" : \"hosted\",\n" + 
            "    \"allow_snapshots\" : false,\n" + 
            "    \"allow_releases\" : true\n" + 
            "  }, {\n" + 
            "    \"type\": \"hosted\",\n" + 
            "    \"key\" : \"maven:hosted:qI3Cq2OZ\",\n" +
            "    \"snapshotTimeoutSeconds\" : 0,\n" + 
            "    \"name\" : \"qI3Cq2OZ\",\n" + 
            "    \"doctype\" : \"hosted\",\n" + 
            "    \"allow_snapshots\" : false,\n" + 
            "    \"allow_releases\" : true\n" + 
            "  }, {\n" + 
            "    \"type\": \"hosted\",\n" + 
            "    \"key\" : \"maven:hosted:local-deployments\",\n" +
            "    \"snapshotTimeoutSeconds\" : 86400,\n" + 
            "    \"name\" : \"local-deployments\",\n" + 
            "    \"doctype\" : \"hosted\",\n" + 
            "    \"allow_snapshots\" : true,\n" + 
            "    \"allow_releases\" : true\n" + 
            "  }, {\n" + 
            "    \"type\": \"hosted\",\n" + 
            "    \"key\" : \"maven:hosted:AFhJnQLW\",\n" +
            "    \"snapshotTimeoutSeconds\" : 0,\n" + 
            "    \"name\" : \"AFhJnQLW\",\n" + 
            "    \"doctype\" : \"hosted\",\n" + 
            "    \"allow_snapshots\" : false,\n" + 
            "    \"allow_releases\" : true\n" + 
            "  } ]\n" + 
            "}";
        /* @formatter:on */

        final StoreListingDTO<HostedRepository> value =
            new IndyObjectMapper( true ).readValue( json, new TypeReference<StoreListingDTO<HostedRepository>>()
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
