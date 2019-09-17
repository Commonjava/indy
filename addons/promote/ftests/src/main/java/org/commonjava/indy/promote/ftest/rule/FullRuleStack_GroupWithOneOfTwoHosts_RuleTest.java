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
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.promote.model.GroupPromoteRequest;
import org.commonjava.indy.promote.model.GroupPromoteResult;
import org.commonjava.indy.promote.model.ValidationResult;
import org.commonjava.indy.promote.model.ValidationRuleSet;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

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
public class FullRuleStack_GroupWithOneOfTwoHosts_RuleTest
        extends AbstractValidationRuleTest<Group>
{

    private static final String RULE = "no-pre-existing-paths.groovy";

    private static final String PREFIX = "no-pre-existing-paths/";

    /* @formatter:off */
    private static final String[] RULES = {
        "parsable-pom.groovy",
        "artifact-refs-via.groovy",
        "no-pre-existing-paths.groovy",
        "no-snapshots.groovy",
        "no-version-ranges.groovy",
        "project-version-pattern.groovy",
        "project-artifacts.groovy"
    };
    /* @formatter:on */

    private HostedRepository hostTarget1;

    private HostedRepository hostTarget2;

    @Test
    @Category( EventDependent.class )
    public void run()
            throws Exception
    {
        List<String> deploy = Arrays.asList( "org/foo/valid/1.1.0-redhat-1/valid-1.1.0-redhat-1.pom", "org/foo/valid/1.1.0-redhat-1/valid-1.1.0-redhat-1.jar", "org/foo/valid/1.1.0-redhat-1/valid-1.1.0-redhat-1-sources.jar", "org/foo/valid/1.1.0-redhat-1/valid-1.1.0-redhat-1-javadoc.jar" );

        Stream.of( hostTarget1, hostTarget2 ).forEach( (repo)->{
            deploy.forEach( (path)->{
                try
                {
                    deployResource( repo.getKey(), path, PREFIX + "valid.pom.xml" );
                    try (InputStream stream = client.content().get( repo.getKey(), path ))
                    {
                        String retrieved = IOUtils.toString( stream );
                        assertThat( path + " invalid from: " + repo.getKey(), retrieved,
                                    equalTo( resourceToString( PREFIX + "valid.pom.xml" ) ) );
                    }
                }
                catch ( Exception e )
                {
                    fail( "Failed to deploy: " + path + " to: " + repo );
                }
            } );
        } );

//        GroupPromoteRequest request = new GroupPromoteRequest( hostTarget1.getKey(), target.getName() );
//        GroupPromoteResult result = module.promoteToGroup( request );
//
//        assertThat( result, notNullValue() );
//        assertThat( result.getValidations(), notNullValue() );
//        assertThat( result.getValidations().isValid(), equalTo( true ) );

        target.addConstituent( hostTarget1 );
        client.stores().update( target, "update target" );
        deploy.forEach((path)->{
            try
            {
                try (InputStream stream = client.content().get( target.getKey(), path ))
                {
                    String retrieved = IOUtils.toString( stream );
                    assertThat( path + " invalid from: " + target.getKey(), retrieved,
                                equalTo( resourceToString( PREFIX + "valid.pom.xml" ) ) );
                }
            }
            catch ( Exception e )
            {
                fail( "Failed to verify: " + path + " in: " + target.getKey() );
            }
        });

        waitForEventPropagation();

        GroupPromoteRequest request = new GroupPromoteRequest( hostTarget2.getKey(), target.getName() );
        GroupPromoteResult result = module.promoteToGroup( request );
        assertThat( result, notNullValue() );

        ValidationResult validations = result.getValidations();
        assertThat( validations, notNullValue() );

        Map<String, String> validatorErrors = validations.getValidatorErrors();
        assertThat( validatorErrors, notNullValue() );

        System.out.println( validatorErrors );

        String errors = validatorErrors.get( RULE );
        assertThat( errors, notNullValue() );

        System.out.println( validatorErrors );
//        assertThat( errors.contains( deploy ), equalTo( true ) );
    }

    public FullRuleStack_GroupWithOneOfTwoHosts_RuleTest()
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

    @Override
    protected void initTestData( final CoreServerFixture fixture )
            throws IOException
    {
        writeDataFile( "promote/rules/" + getRuleScriptFile(), getRuleScriptContent() );

        String json = new ObjectMapper().writeValueAsString( getRuleSet() );
        writeDataFile( "promote/rule-sets/" + name.getMethodName() + ".json", json );

        Stream.of( RULES ).forEach( ( rule ) -> {
            try
            {
                writeDataFile( "promote/rule-sets/" + rule, readTestResource( "promote/rules/" + rule ) );
            }
            catch ( IOException e )
            {
                fail( "Cannot read / write rule file: " + rule );
            }
        } );

        super.initTestData( fixture );
    }

    @Override
    protected ValidationRuleSet getRuleSet()
    {
        ValidationRuleSet ruleSet = new ValidationRuleSet();
        ruleSet.setName( "test" );
        ruleSet.setStoreKeyPattern( "group:target" );

        ruleSet.setRuleNames( Arrays.asList( RULES ) );

        Map<String, String> params = new HashMap<>();
        params.put( "availableInStores", "group:public, group:target" );
        params.put( "classifierAndTypeSet", "javadoc:jar, sources:jar" );
        params.put( "versionPattern", "\\d+\\.\\d+\\.\\d+[.-]redhat-\\d+" );

        ruleSet.setValidationParameters( params );

        return ruleSet;
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        super.initTestConfig( fixture );
        writeConfigFile( "conf.d/promote.conf", "[promote]\nlock.timeout.seconds=60" );
        writeConfigFile( "conf.d/threadpools.conf", "[threadpools]\nenabled=true" );
    }
}
