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

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.promote.model.GroupPromoteRequest;
import org.commonjava.indy.promote.model.GroupPromoteResult;
import org.commonjava.indy.promote.model.ValidationResult;
import org.commonjava.indy.promote.model.ValidationRuleSet;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * When <br />
 *
 *  <ol>
 *    <li>Two hosted repositories</li>
 *    <li>One group, with ONLY ONE of the hosted repositories as a member</li>
 *    <li>same path deployed to both hosted repositories</li>
 *    <li>promotion validation rule-set that includes no-pre-existing-paths.groovy and targets the group</li>
 *    <li>by-group promotion request posted</li>
 *  </ol>
 *
 *  Then <br />
 *
 *  <ol>
 *    <li>the no-pre-existing-paths.groovy rule should be triggered with validation error</li>
 *  </ol>
 *
 */
public class NoPreExistingPaths_GroupWithOneOfTwoHosts_RuleTest
        extends AbstractValidationRuleTest<Group>
{

    private static final String RULE = "no-pre-existing-paths.groovy";

    private static final String PREFIX = "no-pre-existing-paths/";

    private HostedRepository hostTarget1;

    private HostedRepository hostTarget2;

    @Test
    @Category( EventDependent.class )
    public void run()
            throws Exception
    {
        String deploy = "org/foo/valid/1.1/valid-1.1.pom";

        deployResource( hostTarget1.getKey(), deploy, PREFIX + "valid.pom.xml" );
        try (InputStream stream = client.content().get( hostTarget1.getKey(), deploy ))
        {
            String retrieved = IOUtils.toString( stream );
            assertThat( deploy + " invalid from: " + hostTarget1.getKey(), retrieved,
                        equalTo( resourceToString( PREFIX + "valid.pom.xml" ) ) );
        }

        deployResource( hostTarget2.getKey(), deploy, PREFIX + "valid.pom.xml" );
        try (InputStream stream = client.content().get( hostTarget2.getKey(), deploy ))
        {
            String retrieved = IOUtils.toString( stream );
            assertThat( deploy + " invalid from: " + hostTarget2.getKey(), retrieved,
                        equalTo( resourceToString( PREFIX + "valid.pom.xml" ) ) );
        }

        GroupPromoteRequest request = new GroupPromoteRequest( hostTarget1.getKey(), target.getName() );
        GroupPromoteResult result = module.promoteToGroup( request );

        assertThat( result, notNullValue() );
        assertThat( result.getValidations(), notNullValue() );
        assertThat( result.getValidations().isValid(), equalTo( true ) );

//        target.addConstituent( hostTarget1 );
//        client.stores().update( target, "update target" );
//        try (InputStream stream = client.content().get( target.getKey(), deploy ))
//        {
//            String retrieved = IOUtils.toString( stream );
//            assertThat( deploy + " invalid from: " + target.getKey(), retrieved,
//                        equalTo( resourceToString( PREFIX + "valid.pom.xml" ) ) );
//        }
//
//        deployResource( deploy, PREFIX + "valid.pom.xml" );

        waitForEventPropagation();

        request = new GroupPromoteRequest( hostTarget2.getKey(), target.getName() );
        result = module.promoteToGroup( request );
        assertThat( result, notNullValue() );

        ValidationResult validations = result.getValidations();
        assertThat( validations, notNullValue() );

        Map<String, String> validatorErrors = validations.getValidatorErrors();
        assertThat( validatorErrors, notNullValue() );

        System.out.println( validatorErrors );

        String errors = validatorErrors.get( RULE );
        assertThat( errors, notNullValue() );

        System.out.println( validatorErrors );
        assertThat( errors.contains( deploy ), equalTo( true ) );
    }

    public NoPreExistingPaths_GroupWithOneOfTwoHosts_RuleTest()
    {
        super( Group.class );
    }

    @Override
    protected String getRuleScriptFile()
    {
        return RULE;
    }

    @Override
    protected String getRuleScriptContent()
            throws IOException
    {
        String path = "promote/rules/" + RULE;
        return readTestResource( path );
    }

    @Override
    protected ValidationRuleSet getRuleSet()
    {
        ValidationRuleSet ruleSet = new ValidationRuleSet();
        ruleSet.setName( "test" );
        ruleSet.setStoreKeyPattern( "group:target" );
        ruleSet.setRuleNames( Collections.singletonList( getRuleScriptFile() ) );

        return ruleSet;
    }

    @Override
    public void start()
            throws Throwable
    {
        super.start();

        hostTarget1 = new HostedRepository( "hostTarget1" );
        hostTarget1 =
                client.stores().create( hostTarget1, "Creating first host target", HostedRepository.class );

        hostTarget2 = new HostedRepository( "hostTarget2" );
        hostTarget2 =
                client.stores().create( hostTarget2, "Creating secondary host target", HostedRepository.class );

    }
}
