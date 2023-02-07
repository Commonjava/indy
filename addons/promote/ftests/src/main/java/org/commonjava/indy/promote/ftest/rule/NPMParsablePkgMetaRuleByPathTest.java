/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.promote.model.PathsPromoteRequest;
import org.commonjava.indy.promote.model.PathsPromoteResult;
import org.commonjava.indy.promote.model.ValidationResult;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This is used to test npm-parsable-package-meta rule can work correctly for parsable package.json:
 * <b>GIVEN:</b>
 * <ul>
 *     <li>Rule-set with npm-parsable-package-meta</li>
 *     <li>Source and target hosted repo with npm pkg type</li>
 *     <li>Source repo contains valid and invalid package.json</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Do paths promotion from source repo to target repo</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>Validation failed and validation errors for rule is in result</li>
 *     <li>Validation errors only contains the paths for invalid package.json</li>
 * </ul>
 */
public class NPMParsablePkgMetaRuleByPathTest
                extends AbstractValidationRuleTest<HostedRepository>
{

    private static final String RULE = "npm-parsable-package-meta.groovy";

    private static final String INVALID = "wrong/package.json";
    private static final String VALID = "valid/package.json";

    @Test
    @Category( EventDependent.class )
    public void run() throws Exception
    {
        deploy( INVALID, "This is not parsable" );
        deployResource( VALID, "npm-parsable-pkg-meta/package.json" );
        assertThat( client.content().exists( source.getKey(), INVALID ), equalTo( true ) );
        assertThat( client.content().exists( source.getKey(), VALID ), equalTo( true ) );

        waitForEventPropagation();

        PathsPromoteRequest request = new PathsPromoteRequest( source.getKey(), target.getKey(), VALID, INVALID );
        check(request);

        request = new PathsPromoteRequest( source.getKey(), target.getKey() );
        check(request);

    }

    private void check(PathsPromoteRequest request) throws Exception{
        request.setPaths( new HashSet<>( Arrays.asList( INVALID, VALID )) );

        PathsPromoteResult result = module.promoteByPath( request );
        assertThat( result, notNullValue() );

        ValidationResult validations = result.getValidations();
        assertThat( validations, notNullValue() );

        Map<String, String> validatorErrors = validations.getValidatorErrors();
        assertThat( validatorErrors, notNullValue() );

        logger.info( "Validation errors: {}", validatorErrors );

        String errors = validatorErrors.get( RULE );
        assertThat( errors, notNullValue() );

        assertThat( errors.contains( VALID ), equalTo( false ) );
        assertThat( errors.contains( INVALID ), equalTo( true ) );
    }

    public NPMParsablePkgMetaRuleByPathTest()
    {
        super( HostedRepository.class );
    }

    @Override
    protected String getRuleScriptFile()
    {
        return RULE;
    }

    @Override
    protected String getRuleScriptContent() throws IOException
    {
        String path = "promote/rules/" + RULE;
        return readTestResource( path );
    }
}
