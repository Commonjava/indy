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
package org.commonjava.indy.pkg.npm.content;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.junit.Assert.assertNotEquals;

/**
 * This case tests hosted content will be changed too if upload from a group
 * when: <br />
 * <ul>
 *      <li>creates one hosted repo</li>
 *      <li>creates one group repo containing the hosted member</li>
 *      <li>stores file in the hosted repo</li>
 *      <li>uploads the updated content to the group repo</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the content in hosted will be updated too</li>
 * </ul>
 */
public class NPMGroupUploadContentChangedForHostedTest
        extends AbstractContentManagementTest
{

    private static final String HOSTED = "HOSTED";

    private static final String GROUP = "GROUP";

    private static final String PATH = "jquery";

    @Test
    public void test()
            throws Exception
    {

        final HostedRepository hosted = new HostedRepository( NPM_PKG_KEY, HOSTED );
        client.stores().create( hosted, "adding npm hosted repo", HostedRepository.class );

        final String content = "This is content #1.";
        client.content().store( hosted.getKey(), PATH, new ByteArrayInputStream( content.getBytes() ) );
        String originalContent = IOUtils.toString( client.content().get( hosted.getKey(), PATH ) );

        final Group group = new Group( NPM_PKG_KEY, GROUP, hosted.getKey() );
        client.stores().create( group, "adding npm group repo", Group.class );

        final String update = "This is a test: " + System.nanoTime();
        client.content().store( group.getKey(), PATH, new ByteArrayInputStream( update.getBytes() ) );
        String updatedContent = IOUtils.toString( client.content().get( hosted.getKey(), PATH ) );

        assertNotEquals( originalContent, updatedContent );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
