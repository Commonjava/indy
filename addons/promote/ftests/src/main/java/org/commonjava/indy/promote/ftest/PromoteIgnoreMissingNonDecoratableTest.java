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
package org.commonjava.indy.promote.ftest;

import org.commonjava.indy.promote.client.IndyPromoteClientModule;
import org.commonjava.indy.promote.model.PathsPromoteRequest;
import org.commonjava.indy.promote.model.PathsPromoteResult;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class PromoteIgnoreMissingNonDecoratableTest
                extends AbstractPromotionManagerTest
{

    protected final String nonDecoratablePath = "/path/to/foo.jar.md5";

    @Test
    public void run() throws Exception
    {
        final PathsPromoteResult result = client.module( IndyPromoteClientModule.class )
                                                .promoteByPath( new PathsPromoteRequest( source.getKey(),
                                                                                         target.getKey(), first, second,
                                                                                         nonDecoratablePath ) );

        final Set<String> pending = result.getPendingPaths();
        assertThat( pending.isEmpty(), equalTo( true ) );

        final Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 2 ) );

        // the missing non-decoratable path should be skipped
        final Set<String> skipped = result.getSkippedPaths();
        assertThat( skipped, notNullValue() );
        assertThat( skipped.contains( nonDecoratablePath ), equalTo( true ) );

        assertThat( result.getError(), nullValue() );

        assertThat( client.content().exists( target.getKey(), first ), equalTo( true ) );
        assertThat( client.content().exists( target.getKey(), second ), equalTo( true ) );
    }
}
