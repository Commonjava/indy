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
package org.commonjava.indy.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class PathUtilsTest
{
    @Test
    public void testGetCurrentDirPath()
    {
        assertThat( PathUtils.getCurrentDirPath( "org/commonjava/indy/core/1.2.3/" ),
                    equalTo( "org/commonjava/indy/core/1.2.3/" ) );
        assertThat( PathUtils.getCurrentDirPath( "org/commonjava/indy/core/1.2.3/indy-core-1.2.3.jar" ),
                    equalTo( "org/commonjava/indy/core/1.2.3/" ) );
        assertThat( PathUtils.getCurrentDirPath( "org/commonjava/indy/core/" ),
                    equalTo( "org/commonjava/indy/core/" ) );
        assertThat( PathUtils.getCurrentDirPath( "org/commonjava/indy/core/maven-metadata.xml" ),
                    equalTo( "org/commonjava/indy/core/" ) );
    }
}
