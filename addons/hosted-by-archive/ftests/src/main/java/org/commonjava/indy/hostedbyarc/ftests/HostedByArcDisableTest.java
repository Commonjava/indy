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
package org.commonjava.indy.hostedbyarc.ftests;

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.hostedbyarc.client.IndyHostedByArchiveClientModule;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.commonjava.indy.util.ApplicationStatus;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>Indy disabled hosted by archive addon by configuration</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Upload a zip file to indy for creating hosted</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>Indy returns 405 "Method not allowed"</li>
 * </ul>
 */
public class HostedByArcDisableTest
        extends AbstractHostedByArcTest
{

    @Test
    public void testUploadZipAndCreate()
            throws Exception
    {
        IndyHostedByArchiveClientModule module = client.module( IndyHostedByArchiveClientModule.class );

        final String hostedRepoName = "hosted-zip-ignore";

        try
        {
            module.createRepo( getZipFile(), hostedRepoName, "maven-repository" );
        }
        catch ( IndyClientException e )
        {
            logger.info( e.getMessage() );
            assertThat( e.getStatusCode(), equalTo( ApplicationStatus.METHOD_NOT_ALLOWED.code() ) );
        }

    }

    @Override
    protected String getZipFileResource()
    {
        return "repo-with-ignore.zip";
    }

    @Override
    protected boolean enabled()
    {
        return false;
    }
}
