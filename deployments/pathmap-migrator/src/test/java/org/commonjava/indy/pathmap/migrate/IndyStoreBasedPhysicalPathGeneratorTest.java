/**
 * Copyright (C) 2013~2019 Red Hat, Inc.
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
package org.commonjava.indy.pathmap.migrate;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class IndyStoreBasedPhysicalPathGeneratorTest
{
    private final String PHYSICAL_PATH1 = "/opt/indy/var/lib/indy/storage/maven/hosted-public/org/commonjava/indy/indy-api/1.0/indy-api-1.0.pom";

    private final String PHYSICAL_PATH2 = "/opt/indy/var/lib/indy/storage/maven/hosted-shared-imports-redhat/org/commonjava/indy/indy-api/1.0/indy-api-1.0.pom";

    private final String PHYSICAL_PATH3 = "/opt/indy/var/lib/indy/storage/maven/remote-koji-org.jboss.classfilewriter-jboss-classfilewriter-1.2.3.Final_redhat_00001-1/org/commonjava/indy/indy-api/1.0/indy-api-1.0.pom";


    IndyStoreBasedPathGenerator generator =
            new IndyStoreBasedPathGenerator( "/opt/indy/var/lib/indy/storage/" );

    @Test
    public void getPath()
    {
        assertThat( generator.generatePath( PHYSICAL_PATH1 ), equalTo( "/org/commonjava/indy/indy-api/1.0/indy-api-1.0.pom" ) );
        assertThat( generator.generatePath( PHYSICAL_PATH2 ), equalTo( "/org/commonjava/indy/indy-api/1.0/indy-api-1.0.pom" ) );
        assertThat( generator.generatePath( PHYSICAL_PATH3 ), equalTo( "/org/commonjava/indy/indy-api/1.0/indy-api-1.0.pom" ) );
    }

    @Test
    public void getFileSystem()
    {
        assertThat( generator.generateFileSystem( PHYSICAL_PATH1 ), equalTo( "maven:hosted:public" ) );
        assertThat( generator.generateFileSystem( PHYSICAL_PATH2 ), equalTo( "maven:hosted:shared-imports-redhat" ) );
        assertThat( generator.generateFileSystem( PHYSICAL_PATH3 ), equalTo( "maven:remote:koji-org.jboss.classfilewriter-jboss-classfilewriter-1.2.3.Final_redhat_00001-1" ) );
    }

    @Test
    public void getStorePath()
    {
        assertThat( generator.generateStorePath( PHYSICAL_PATH1 ), equalTo( "/maven/hosted-public/org/commonjava/indy/indy-api/1.0/indy-api-1.0.pom" ) );
        assertThat( generator.generateStorePath( PHYSICAL_PATH2 ), equalTo( "/maven/hosted-shared-imports-redhat/org/commonjava/indy/indy-api/1.0/indy-api-1.0.pom" ) );
        assertThat( generator.generateStorePath( PHYSICAL_PATH3 ), equalTo( "/maven/remote-koji-org.jboss.classfilewriter-jboss-classfilewriter-1.2.3.Final_redhat_00001-1/org/commonjava/indy/indy-api/1.0/indy-api-1.0.pom" ) );
    }
}
