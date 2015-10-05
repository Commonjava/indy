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
package org.commonjava.aprox.promote.ftest.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.ftest.core.AbstractAproxFunctionalTest;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.promote.client.AproxPromoteClientModule;
import org.commonjava.aprox.promote.model.ValidationRuleSet;
import org.commonjava.aprox.test.fixture.core.CoreServerFixture;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

public abstract class AbstractValidationRuleTest<T extends ArtifactStore> extends AbstractAproxFunctionalTest
{
    protected HostedRepository source;

    protected T target;

    protected AproxPromoteClientModule module;

    private final Class<T> targetCls;

    protected AbstractValidationRuleTest(Class<T> targetCls)
    {
        this.targetCls = targetCls;
    }

    @Override
    public void start()
            throws Throwable
    {
        super.start();

        module = client.module( AproxPromoteClientModule.class );

        HostedRepository shr = new HostedRepository( "target" );
        shr.setAllowSnapshots( true );

        source = client.stores().create( shr, "creating test source", HostedRepository.class );
        if ( Group.class.equals( targetCls ) )
        {
            target = (T) client.stores().create( new Group( "target" ), "creating test target", Group.class );
        }
        else if ( HostedRepository.class.equals( targetCls ) )
        {
            HostedRepository hr = new HostedRepository( "target" );
            hr.setAllowSnapshots( true );

            target = (T) client.stores().create( hr, "creating test target", HostedRepository.class );
        }
        else
        {
            throw new IllegalStateException( targetCls.getName() + " is not currently supported as promotion targets." );
        }
    }

    protected void deploy( String path, String content)
            throws AproxClientException
    {
        client.content().store( source.getKey(), path, new ByteArrayInputStream( content.getBytes() ) );
    }

    @Override
    protected void initTestData( CoreServerFixture fixture )
            throws IOException
    {
        writeDataFile( "promote/rules/" + getRuleScriptFile(), getRuleScriptContent() );

        ValidationRuleSet rs = getRuleSet();
        String json = new ObjectMapper().writeValueAsString( rs );
        writeDataFile( "promote/rule-sets/" + name.getMethodName() + ".json", json );

        super.initTestData( fixture );
    }

    protected ValidationRuleSet getRuleSet()
    {
        ValidationRuleSet ruleSet = new ValidationRuleSet();
        ruleSet.setName( "test" );
        ruleSet.setStoreKeyPattern( "group:target" );
        ruleSet.setRuleNames( Collections.singletonList( getRuleScriptFile() ) );

        return ruleSet;
    }

    protected abstract String getRuleScriptFile();

    protected abstract String getRuleScriptContent()
            throws IOException;

    @Override
    protected Collection<AproxClientModule> getAdditionalClientModules()
    {
        return Collections.singletonList( new AproxPromoteClientModule() );
    }
}
