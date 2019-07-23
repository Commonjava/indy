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

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.promote.client.IndyPromoteClientModule;
import org.commonjava.indy.promote.model.GroupPromoteRequest;
import org.commonjava.indy.promote.model.GroupPromoteResult;
import org.commonjava.indy.promote.model.ValidationResult;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class GroupPromoteMatchesSucceedingValidationTest
    extends AbstractPromotionManagerTest
{

    @Test
    public void run()
        throws Exception
    {
        final GroupPromoteResult result = client.module( IndyPromoteClientModule.class )
                                           .promoteToGroup(
                                                   new GroupPromoteRequest( source.getKey(), target.getName() ) );

        assertThat( result.succeeded(), equalTo( true ) );

        assertThat( result.getRequest().getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest()
                          .getTargetGroup(), equalTo( target.getName() ) );

        assertThat( result.getError(), nullValue() );

        ValidationResult validations = result.getValidations();
        assertThat( validations, notNullValue() );

        assertThat( validations.isValid(), equalTo( true ) );

        assertThat( client.content().exists( target.getKey().getType(), target.getName(), first ), equalTo( true ) );
        assertThat( client.content().exists( target.getKey().getType(), target.getName(), second ), equalTo( true ) );

        Group g = client.stores().load( StoreType.group, target.getName(), Group.class );
        assertThat( g.getConstituents().contains( source.getKey() ), equalTo( true ) );
    }

    @Override
    protected ArtifactStore createTarget( String changelog )
            throws Exception
    {
        Group group = new Group( "test" );
        return client.stores().create( group, changelog, Group.class );
    }

    @Override
    protected void initTestData( CoreServerFixture fixture )
            throws IOException
    {
        writeDataFile( "promote/rules/fail.groovy", readTestResource( getClass().getSimpleName() + "/fail.groovy" ) );
        writeDataFile( "promote/rule-sets/fail.json", readTestResource( getClass().getSimpleName() + "/fail.json" ) );
        writeDataFile( "promote/rule-sets/succeed.json", readTestResource( getClass().getSimpleName() + "/succeed.json" ) );
    }
}
