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
package org.commonjava.indy.ftest.core.content;

import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class EmptyDisabledTimeoutsMapRetrievalTest
        extends AbstractContentManagementTest
{

    public class DelayInputStream
        extends InputStream
    {
        @Override
        public int read()
            throws IOException
        {
            try
            {
                Thread.sleep( 5000 );
            }
            catch ( final InterruptedException e )
            {
            }

            return 0;
        }
    }

    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Test
    public void run()
        throws Exception
    {
        Map<StoreKey, Date> storeTimeouts = client.schedules().getDisabledStoreTimeouts();
        assertThat( storeTimeouts.isEmpty(), equalTo( true ) );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }

    protected void initBaseTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        writeConfigFile( "conf.d/indexer.conf", "[indexer]\nenabled=false" );
    }
}
