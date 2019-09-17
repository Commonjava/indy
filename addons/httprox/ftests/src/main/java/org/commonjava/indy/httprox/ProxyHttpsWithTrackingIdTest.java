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
package org.commonjava.indy.httprox;

import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.dto.StoreListingDTO;
import org.junit.Test;

import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_PKG_KEY;
import static org.junit.Assert.assertTrue;

public class ProxyHttpsWithTrackingIdTest
                extends ProxyHttpsTest
{

    private static final String TRACKING_ID = "A8DinNReIBj9NH";

    private static final String USER = TRACKING_ID + "+tracking";

    private static final String PASS = "password";

    String https_url =
                    "https://oss.sonatype.org/content/repositories/releases/org/commonjava/indy/indy-api/1.3.1/indy-api-1.3.1.pom";

    @Override
    protected String getBaseHttproxConfig()
    {
        return DEFAULT_BASE_HTTPROX_CONFIG + "\nsecured=true";
    }

    @Test
    public void run() throws Exception
    {
        String ret = get( https_url, true, USER, PASS );
        assertTrue( ret.contains( "<artifactId>indy-api</artifactId>" ) );

        StoreListingDTO<Group> groups = this.client.stores().listGroups( GENERIC_PKG_KEY );
        groups.forEach( group -> System.out.println("Group >> " + group) );
        assertTrue( groups.getItems().size() == 1 );
    }

}
