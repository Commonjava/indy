/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.koji.conf;

import org.commonjava.maven.atlas.ident.util.ArtifactPathInfo;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 6/7/16.
 */
public class PathInfoTest
{
    @Test
    public void parseRedhatBuildPath()
    {
        String path = "commons-io/commons-io/2.4.0.redhat-1/commons-io-2.4.0.redhat-1.pom";
        ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );

        assertThat( pathInfo, notNullValue() );
    }
}
