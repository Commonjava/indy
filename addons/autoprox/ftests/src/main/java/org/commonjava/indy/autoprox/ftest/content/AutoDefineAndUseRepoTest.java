/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.autoprox.ftest.content;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.model.core.StoreType;
import org.junit.Test;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 10/6/15.
 */
public class AutoDefineAndUseRepoTest
        extends AbstractAutoproxContentTest
{

    @Test
    public void run()
            throws Exception
    {
        String path = "path/to/test.txt";
        String content = "This is a test";

        http.expect( http.formatUrl( NAME, path ), 200, content );

        InputStream stream = client.content().get( StoreType.remote, NAME, path );
        assertThat( stream, notNullValue() );

        String result = IOUtils.toString( stream );
        assertThat( result, equalTo( content ) );

        stream.close();
    }
}
