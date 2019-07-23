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
package org.commonjava.indy.bind.jaxrs.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class JaxRsUriFormatterTest
{

    @Test
    public void formatAbsoluteUrl()
    {
        final String base = "http://localhost:12345";
        final String path = "/api/path/to/something";
        final String url = new JaxRsUriFormatter().formatAbsolutePathTo( base, path );

        assertThat( url, equalTo( base + path ) );
    }

    @Test
    public void formatAbsolutePath()
    {
        final String base = "/some";
        final String path = "/api/path/to/something";
        final String url = new JaxRsUriFormatter().formatAbsolutePathTo( base, path );

        assertThat( url, equalTo( base + path ) );
    }

    @Test
    public void formatAbsolutePath_BaseNotAbsolute()
    {
        final String base = "some";
        final String path = "/api/path/to/something";
        final String url = new JaxRsUriFormatter().formatAbsolutePathTo( base, path );

        assertThat( url, equalTo( "/" + base + path ) );
    }

}
