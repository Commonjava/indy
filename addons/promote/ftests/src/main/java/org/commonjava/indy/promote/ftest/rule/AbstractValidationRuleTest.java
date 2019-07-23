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
package org.commonjava.indy.promote.ftest.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.promote.client.IndyPromoteClientModule;
import org.commonjava.indy.promote.model.ValidationRuleSet;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.junit.Assert.fail;

public abstract class AbstractValidationRuleTest<T extends ArtifactStore> extends AbstractIndyFunctionalTest
{
    private static final String TARGET_NAME = "target";

    protected HostedRepository source;

    protected T target;

    protected IndyPromoteClientModule module;

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

        module = client.module( IndyPromoteClientModule.class );

        HostedRepository shr = new HostedRepository( "source" );
        shr.setAllowSnapshots( true );

        System.out.println("Validation rule test client:"+ client);
        System.out.println("Validation rule test client store:"+client.stores());
        source = client.stores().create( shr, "creating test source", HostedRepository.class );
        if ( Group.class.equals( targetCls ) )
        {
            target = (T) client.stores().create( new Group( TARGET_NAME ), "creating test target", Group.class );
        }
        else if ( HostedRepository.class.equals( targetCls ) )
        {
            HostedRepository hr = new HostedRepository( TARGET_NAME );
            hr.setAllowSnapshots( true );

            target = (T) client.stores().create( hr, "creating test target", HostedRepository.class );
        }
        else
        {
            throw new IllegalStateException( targetCls.getName() + " is not currently supported as promotion targets." );
        }
    }

    protected String resourceToString( String resource )
            throws IOException
    {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream( resource ))
        {
            if ( stream == null )
            {
                fail( "Cannot find classpath resource: " + resource );
            }

            return IOUtils.toString( stream );
        }
    }

    protected void deployResource( String path, String resource )
            throws IOException, IndyClientException
    {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream( resource ))
        {
            if ( stream == null )
            {
                fail( "Cannot find classpath resource: " + resource );
            }

            client.content().store( source.getKey(), path, stream );
        }
    }

    protected void deployResource( StoreKey target, String path, String resource )
            throws IOException, IndyClientException
    {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream( resource ))
        {
            if ( stream == null )
            {
                fail( "Cannot find classpath resource: " + resource );
            }

            client.content().store( target, path, stream );
        }
    }

    protected void deploy( String path, String content)
            throws IndyClientException
    {
        client.content().store( source.getKey(), path, new ByteArrayInputStream( content.getBytes() ) );
        waitForEventPropagation();
    }

    @Override
    protected void initTestData( CoreServerFixture fixture )
            throws IOException
    {
        writeDataFile( "promote/rules/" + getRuleScriptFile(), getRuleScriptContent() );

        ValidationRuleSet rs = getRuleSet();
        String json = new ObjectMapper().writeValueAsString( rs );

        String rulesetPath = "promote/rule-sets/" + name.getMethodName() + ".json";

        logger.info( "Writing rule-set to: {}\nContents:\n\n{}\n\n", rulesetPath, json );
        writeDataFile( rulesetPath, json );

        super.initTestData( fixture );
    }

    protected ValidationRuleSet getRuleSet()
    {
        ValidationRuleSet ruleSet = new ValidationRuleSet();
        ruleSet.setName( "test" );
        ruleSet.setStoreKeyPattern( "maven:[^:]+:" + TARGET_NAME );
        ruleSet.setRuleNames( Collections.singletonList( getRuleScriptFile() ) );

        return ruleSet;
    }

    protected abstract String getRuleScriptFile();

    protected abstract String getRuleScriptContent()
            throws IOException;

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Collections.singletonList( new IndyPromoteClientModule() );
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        super.initTestConfig( fixture );
        writeConfigFile( "conf.d/threadpools.conf", "[threadpools]\nenabled=true" );
    }
}
