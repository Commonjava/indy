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
package org.commonjava.indy.dotmaven.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class NameUtilsTest
{

    @Test
    public void checkInvalidURI()
    {
        assertThat( NameUtils.isValidResource( "/.DS_Store" ), equalTo( false ) );
    }

    @Test
    public void checkInvalidURILeaf()
    {
        assertThat( NameUtils.isValidResource( "/path/to/.DS_Store" ), equalTo( false ) );
    }

}
