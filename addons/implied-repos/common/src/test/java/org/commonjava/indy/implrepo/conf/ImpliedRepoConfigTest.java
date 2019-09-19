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
package org.commonjava.indy.implrepo.conf;

import org.junit.Test;

import java.net.MalformedURLException;

import static org.junit.Assert.assertTrue;

/**
 * Created by jdcasey on 11/30/15.
 */
public class ImpliedRepoConfigTest
{

    @Test
    public void testNexusStagingUrlDisable()
            throws MalformedURLException
    {
        ImpliedRepoConfig config = new ImpliedRepoConfig();
        config.addBlacklistedHost( ".+service.local.staging.*" );

        String url = "http://localhost:8081/service/local/staging/deploy";
        boolean blacklisted = config.isBlacklisted( url );

        assertTrue( "URL should have been blacklisted: " + url, blacklisted );
    }
}
