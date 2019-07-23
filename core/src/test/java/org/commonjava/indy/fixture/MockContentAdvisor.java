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
package org.commonjava.indy.fixture;

import org.commonjava.indy.spi.pkg.ContentAdvisor;
import org.commonjava.indy.spi.pkg.ContentQuality;
import org.commonjava.atlas.maven.ident.util.ArtifactPathInfo;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;

//TODO: This provider is fully duplicated with org.commonjava.indy.test.fixture.core.MockContentAdvisor in
//      test/fixture-core for some dependency reasons in addons. Should be refactored in another common
//      test module in the future.

@Alternative
public class MockContentAdvisor
        implements ContentAdvisor
{
    @Override
    public ContentQuality getContentQuality( String path )
    {
        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        return pathInfo != null && pathInfo.isSnapshot() ? ContentQuality.SNAPSHOT : ContentQuality.RELEASE;
    }
}
