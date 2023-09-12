/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.content;

import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.pkg.PackageTypeConstants;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.junit.Before;
import org.junit.Test;

import static org.commonjava.indy.model.core.PathStyle.hashed;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class IndyPathGeneratorTest
{
    IndyPathGenerator pathGenerator = new IndyPathGenerator();
    HostedRepository repo = new HostedRepository( PackageTypeConstants.PKG_TYPE_GENERIC_HTTP, "test" );
    KeyedLocation location;

    @Before
    public void setUp()
    {
        repo.setPathStyle( hashed );
        location = LocationUtils.toLocation( repo );
    }

    @Test
    public void testGetHashedRootBasedPathUnification()
    {
        ConcreteResource resource = new StoreResource( location, "license.html" );
        String pathWithoutSlash = pathGenerator.getPath( resource );
        resource = new StoreResource( location, "/license.html" );
        String pathWithSlash = pathGenerator.getPath( resource );
        assertThat( pathWithoutSlash, is( pathWithSlash ) );
        resource = new StoreResource( location, "//license.html" );
        String pathWithDoubleSlash = pathGenerator.getPath( resource );
        assertThat( pathWithoutSlash, is( pathWithDoubleSlash ) );
    }

    @Test
    public void testGetHashedPathUnification()
    {
        ConcreteResource resource = new StoreResource( location, "abc/xyz/license.html" );
        String pathWithoutSlash = pathGenerator.getPath( resource );
        resource = new StoreResource( location, "/abc/xyz/license.html" );
        String pathWithSlash = pathGenerator.getPath( resource );
        assertThat( pathWithoutSlash, is( pathWithSlash ) );
        resource = new StoreResource( location, "//abc/xyz/license.html" );
        String pathWithDoubleSlash = pathGenerator.getPath( resource );
        assertThat( pathWithoutSlash, is( pathWithDoubleSlash ) );
    }
}
