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
package org.commonjava.indy.ftest.core.content.prefetch;

import org.apache.commons.io.FileUtils;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public abstract class AbstractRemotePrefetchTest
        extends AbstractContentManagementTest
{
    @Rule
    public ExpectationServer server = new ExpectationServer();

    protected void assertContent( File file, String content )
            throws IOException
    {
        assertThat( FileUtils.readFileToString( file ), equalTo( content ) );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        super.initTestConfig( fixture );
        writeConfigFile( "conf.d/threadpools.conf", "[threadpools]\nenabled=true" );
    }

    @Override
    protected int getTestTimeoutMultiplier()
    {
        return 2;
    }
}
