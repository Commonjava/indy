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
package org.commonjava.indy.koji.util;

import com.redhat.red.build.koji.model.xmlrpc.KojiBuildInfo;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by ruhan on 3/23/18.
 */
public class KojiUtilsTest
{
    final String defaultUrl = "http://my.koji.hub/kojihub/packages/javax.activation-activation/1.1.1.redhat_5/1/maven";

    final String volUrl =
                    "http://my.koji.hub/kojihub/vol/1/packages/javax.activation-activation/1.1.1.redhat_5/1/maven";

    @Test
    public void formatStorageUrl() throws Exception
    {
        KojiUtils kojiUtils = new KojiUtils();

        String root = "http://my.koji.hub/kojihub";
        KojiBuildInfo buildInfo = new KojiBuildInfo();
        buildInfo.setName( "javax.activation-activation" );
        buildInfo.setVersion( "1.1.1.redhat_5" );
        buildInfo.setRelease( "1" );
        buildInfo.setVolumeName( "DEFAULT" );
        String url = kojiUtils.formatStorageUrl( root, buildInfo );

        assertEquals( defaultUrl, url );

        buildInfo.setVolumeName( null );
        url = kojiUtils.formatStorageUrl( root, buildInfo );
        assertEquals( defaultUrl, url );

        buildInfo.setVolumeName( "1" );
        url = kojiUtils.formatStorageUrl( root, buildInfo );
        assertEquals( volUrl, url );
    }
}
