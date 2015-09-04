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
package org.commonjava.aprox.promote.ftest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Set;

import org.commonjava.aprox.promote.client.AproxPromoteClientModule;
import org.commonjava.aprox.promote.model.PathsPromoteRequest;
import org.commonjava.aprox.promote.model.PathsPromoteResult;
import org.junit.Test;

public class PromoteAllWithPurgeTest
    extends AbstractPromotionManagerTest
{

    @Test
    public void promoteAll_PurgeSource_PushTwoArtifactsToHostedRepo_VerifyCopiedToOtherHostedRepo()
        throws Exception
    {
        final PathsPromoteResult result =
            client.module( AproxPromoteClientModule.class )
                  .promoteByPath( new PathsPromoteRequest( source.getKey(), target.getKey() ).setPurgeSource( true ) );

        assertThat( result.getRequest()
                          .getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest()
                          .getTarget(), equalTo( target.getKey() ) );

        final Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        final Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        assertThat( client.content()
                          .exists( target.getKey()
                                         .getType(), target.getName(), first ), equalTo( true ) );
        assertThat( client.content()
                          .exists( target.getKey()
                                         .getType(), target.getName(), second ), equalTo( true ) );

        assertThat( client.content()
                          .exists( source.getKey()
                                         .getType(), source.getName(), first ), equalTo( false ) );
        assertThat( client.content()
                          .exists( source.getKey()
                                         .getType(), source.getName(), second ), equalTo( false ) );
    }
}
