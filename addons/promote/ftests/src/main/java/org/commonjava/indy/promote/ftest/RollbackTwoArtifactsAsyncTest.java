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
import org.commonjava.maven.galley.io.SpecialPathConstants;
import org.junit.Test;

import java.util.Set;

import static org.commonjava.indy.promote.model.AbstractPromoteResult.ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class RollbackTwoArtifactsAsyncTest
    extends AbstractAsyncPromotionManagerTest<PathsPromoteRequest, PathsPromoteResult>
{

    @Test
    public void run()
        throws Exception
    {
        PathsPromoteResult result = client.module( IndyPromoteClientModule.class )
                                     .promoteByPath( getAsyncRequest( new PathsPromoteRequest( source.getKey(), target.getKey() ) ));

        assertEquals( result.getResultCode(), ACCEPTED );
        result = getAsyncPromoteResult( PathsPromoteResult.class );


        assertThat( result.getRequest()
                          .getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest()
                          .getTarget(), equalTo( target.getKey() ) );

        Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        result = client.module( IndyPromoteClientModule.class )
                       .rollbackPathPromote( result );

        assertEquals( result.getResultCode(), ACCEPTED );
        result = getAsyncPromoteResult( PathsPromoteResult.class );


        assertThat( result.getRequest()
                          .getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest()
                          .getTarget(), equalTo( target.getKey() ) );

        completed = result.getCompletedPaths();
        assertThat( completed == null || completed.isEmpty(), equalTo( true ) );

        pending = result.getPendingPaths();
        assertThat( pending, notNullValue() );
        assertThat( pending.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        assertThat( client.content().exists( target.getKey(), first ), equalTo( false ) );
        assertThat( client.content().exists( target.getKey(), second ), equalTo( false ) );
    }
}
