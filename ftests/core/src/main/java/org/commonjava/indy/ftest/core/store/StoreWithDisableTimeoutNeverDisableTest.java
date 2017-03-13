/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.ftest.core.store;

import org.commonjava.indy.core.change.StoreEnablementManager;
import org.commonjava.indy.ftest.core.category.TimingDependent;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreType;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by gli on 3/10/17.
 */
public class StoreWithDisableTimeoutNeverDisableTest
        extends AbstractStoreManagementTest
{
    @Test
    @Category( TimingDependent.class )
    public void updateDisableTimeout()
            throws Exception
    {
        final Group repo = new Group( newName() );
        repo.setDisabled( true );
        repo.setDisableTimeout( StoreEnablementManager.TIMEOUT_NEVER_DISABLE );
        Group result = client.stores().create( repo, name.getMethodName(), Group.class );
        assertThat( result.isDisabled(), equalTo( false ) );

    }
}
