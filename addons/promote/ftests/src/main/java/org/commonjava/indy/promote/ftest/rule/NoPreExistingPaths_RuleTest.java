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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class NoPreExistingPaths_RuleTest
        extends AbstractValidationRuleTest<Group>
{

    private static final String RULE = "no-pre-existing-paths.groovy";

    private static final String PREFIX = "no-pre-existing-paths/";

    private HostedRepository otherSource;

    private Group other;

    @Test
    @Category( EventDependent.class )
    public void run()
            throws Exception
    {
        String invalid = "org/foo/invalid/1/invalid-1.pom";
        String valid = "org/foo/valid/1.1/valid-1.1.pom";

        String content = "this is some content";

        deployResource( other.getKey(), invalid, PREFIX + "invalid.pom.xml");

        try(InputStream stream = client.content().get( other.getKey(), invalid ))
        {
            String retrieved = IOUtils.toString( stream );
            assertThat( invalid + " invalid from: " + other.getKey(), retrieved,
                        equalTo( resourceToString( PREFIX + "invalid.pom.xml" ) ) );
        }

        deployResource( invalid, PREFIX + "invalid.pom.xml");
        deployResource( valid, PREFIX + "valid.pom.xml" );

        waitForEventPropagation();

        GroupPromoteRequest request = new GroupPromoteRequest( source.getKey(), target.getName() );
        GroupPromoteResult result = module.promoteToGroup( request );
        assertThat( result, notNullValue() );

        ValidationResult validations = result.getValidations();
        assertThat( validations, notNullValue() );

        Map<String, String> validatorErrors = validations.getValidatorErrors();
        assertThat( validatorErrors, notNullValue() );

        System.out.println(validatorErrors);

        String errors = validatorErrors.get( RULE );
        assertThat( errors, notNullValue() );

        System.out.println(validatorErrors);
        assertThat( errors.contains( valid ), equalTo( false ) );
        assertThat( errors.contains( invalid ), equalTo( true ) );
    }

    public NoPreExistingPaths_RuleTest()
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

        ruleSet.setValidationParameters( Collections.singletonMap( "availableInStores", "group:other" ) );

        return ruleSet;
    }

    @Override
    public void start()
            throws Throwable
    {
        super.start();

        otherSource = new HostedRepository( "otherSource" );
        otherSource = client.stores().create( otherSource, "Creating secondary content source", HostedRepository.class );

        other = new Group( "other", otherSource.getKey() );
        other = client.stores().create( other, "Creating secondary content group", Group.class );

        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "{} contains members: {}", other, other.getConstituents() );
    }
}
