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

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.junit.Test;

public class StoreAndVerifyViaClientContentDownloadTest
        extends AbstractContentManagementTest
{

    @Test
    public void storeFileThenDownloadAndVerifyContentViaClientApi()
        throws Exception
    {
        final String content = "This is a test: " + System.nanoTime();
        final InputStream stream = new ByteArrayInputStream( content.getBytes() );

        final String path = "/path/to/foo.class";

        assertThat( client.content()
                          .exists( hosted, STORE, path ), equalTo( false ) );

        client.content()
              .store( hosted, STORE, path, stream );

        assertThat( client.content()
                          .exists( hosted, STORE, path ), equalTo( true ) );

        final InputStream is = client.content()
                                     .get( hosted, STORE, path );
        final String result = IOUtils.toString( is );
        is.close();

        assertThat( result, equalTo( content ) );
    }
}
