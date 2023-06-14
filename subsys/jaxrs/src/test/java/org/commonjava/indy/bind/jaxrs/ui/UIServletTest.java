/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.bind.jaxrs.ui;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class UIServletTest
{
    @Test
    public void isValidUIResourceTest() throws IOException
    {
        final File uiDir = new File("/var/lib/indy/ui");

        // Normal file
        String path = "/test.html";
        File resource = new File( uiDir, path );
        assertThat( UIServlet.isValidUIResource(uiDir, resource), equalTo( true ) );

        // Invalid file
        path = "/../../../../../../../../../../etc/passwd";
        resource = new File( uiDir, path );
        assertThat( resource.getCanonicalPath(), equalTo( "/etc/passwd" ) );
        assertThat( UIServlet.isValidUIResource(uiDir, resource), equalTo( false ) );
    }
}
