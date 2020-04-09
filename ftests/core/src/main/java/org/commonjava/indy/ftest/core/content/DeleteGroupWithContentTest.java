/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.ftest.core.content;

import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class DeleteGroupWithContentTest
                extends AbstractContentManagementTest
{
    protected boolean createStandardTestStructures()
    {
        return false;
    }

    @Test
    @Category( EventDependent.class )
    public void run() throws Exception
    {
        final HostedRepository repo = new HostedRepository( MAVEN_PKG_KEY, "test_1" );
        final Group group = new Group( MAVEN_PKG_KEY, "test_1" );
        group.addConstituent( repo );

        assertThat( client.stores().create( repo, name.getMethodName(), HostedRepository.class ), notNullValue() );
        assertThat( client.stores().create( group, name.getMethodName(), Group.class ), notNullValue() );

        client.stores().delete( group.getKey(), "Delete", true );
        assertThat( client.stores().exists( group.getKey() ), equalTo( false ) );
    }
}
