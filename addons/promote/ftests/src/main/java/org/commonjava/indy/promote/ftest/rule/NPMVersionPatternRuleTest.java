/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
import org.commonjava.indy.pkg.PackageTypeConstants;
import org.commonjava.indy.promote.model.PathsPromoteRequest;
import org.commonjava.indy.promote.model.PathsPromoteResult;
import org.commonjava.indy.promote.model.ValidationResult;
import org.commonjava.indy.promote.model.ValidationRuleSet;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * This is used to test npm-version-pattern rule can work correctly for version checking:
 * <b>GIVEN:</b>
 * <ul>
 *     <li>Rule-set with specified valid version pattern</li>
 *     <li>Source and target hosted repo with npm pkg type, and target repo matches rule-set store pattern</li>
 *     <li>Source repo contains valid versioned artifacts and invalid ones</li>
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
 *     <li>Validation errors only contains the paths for invalid versioned artifacts</li>
 * </ul>
 */
public class NPMVersionPatternRuleTest
        extends AbstractValidationRuleTest<HostedRepository>
{

    private static final String RULE = "npm-version-pattern.groovy";

    public NPMVersionPatternRuleTest()
    {
        super( HostedRepository.class );
    }

    private static final String INVALID_PKG = "invalid";

    private static final String VALID_PKG = "valid";

    private static final String SCOPED_INVALID_PKG = "@scoped/invalid";

    private static final String SCOPED_VALID_PKG = "@scoped/valid";

    private static final String REDHAT_SCOPED_INVALID_PKG = "@redhat/invalid";

    private static final String REDHAT_SCOPED_VALID_PKG = "@redhat/valid";

    private static final String INVALID_TAR = "invalid/-/invalid-1.tgz";

    private static final String VALID_TAR = "valid/-/valid-1.5.tgz";

    private static final String SCOPED_INVALID_TAR = "@scoped/invalid/-/invalid-1.tgz";

    private static final String SCOPED_VALID_TAR = "@scoped/valid/-/valid-1.5.tgz";

    private static final String REDHAT_SCOPED_INVALID_TAR = "@redhat/invalid/-/invalid-1.tgz";

    private static final String REDHAT_SCOPED_VALID_TAR = "@redhat/valid/-/valid-1.5.tgz";

    @Test
    @Category( EventDependent.class )
    public void run()
            throws Exception
    {
        deployResource( INVALID_PKG, "npm-version-pattern/package-invalid.json" );
        deployResource( VALID_PKG, "npm-version-pattern/package-valid.json" );
        deployResource( SCOPED_INVALID_PKG, "npm-version-pattern/package-scoped-invalid.json" );
        deployResource( SCOPED_VALID_PKG, "npm-version-pattern/package-scoped-valid.json" );
        deployResource( REDHAT_SCOPED_INVALID_PKG, "npm-version-pattern/package-redhat-scoped-invalid.json" );
        deployResource( REDHAT_SCOPED_VALID_PKG, "npm-version-pattern/package-redhat-scoped-valid.json" );
        assertThat( client.content().exists( source.getKey(), INVALID_TAR ), equalTo( true ) );
        assertThat( client.content().exists( source.getKey(), SCOPED_INVALID_TAR ), equalTo( true ) );
        assertThat( client.content().exists( source.getKey(), INVALID_TAR ), equalTo( true ) );
        assertThat( client.content().exists( source.getKey(), VALID_TAR ), equalTo( true ) );
        assertThat( client.content().exists( source.getKey(), REDHAT_SCOPED_INVALID_TAR ), equalTo( true ) );
        assertThat( client.content().exists( source.getKey(), REDHAT_SCOPED_VALID_TAR ), equalTo( true ) );

        waitForEventPropagation();

        Thread.sleep( 30 * 1000 );

        PathsPromoteRequest request =
                new PathsPromoteRequest( source.getKey(), target.getKey(), INVALID_TAR, VALID_TAR, SCOPED_INVALID_TAR,
                                         SCOPED_VALID_TAR, REDHAT_SCOPED_INVALID_TAR, REDHAT_SCOPED_VALID_TAR );
        check( request );

        request = new PathsPromoteRequest( source.getKey(), target.getKey() );
        check( request );
    }

    private void check( PathsPromoteRequest request )
            throws Exception
    {
        PathsPromoteResult result = module.promoteByPath( request );
        assertThat( result, notNullValue() );

        ValidationResult validations = result.getValidations();
        assertThat( validations, notNullValue() );

        Map<String, String> validatorErrors = validations.getValidatorErrors();
        assertThat( validatorErrors, notNullValue() );

        System.out.println( validatorErrors );

        String errors = validatorErrors.get( RULE );
        assertThat( errors, notNullValue() );

        System.out.println( validatorErrors );
        assertThat( errors.contains( VALID_TAR ), equalTo( false ) );
        assertThat( errors.contains( INVALID_TAR ), equalTo( true ) );
        assertThat( errors.contains( SCOPED_VALID_TAR ), equalTo( false ) );
        assertThat( errors.contains( SCOPED_INVALID_TAR ), equalTo( true ) );
        assertThat( errors.contains( REDHAT_SCOPED_INVALID_TAR ), equalTo( false ) );
        assertThat( errors.contains( REDHAT_SCOPED_VALID_TAR ), equalTo( false ) );
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

    protected ValidationRuleSet getRuleSet()
    {
        ValidationRuleSet ruleSet = new ValidationRuleSet();
        ruleSet.setName( "test" );
        ruleSet.setStoreKeyPattern( "npm:hosted:target" );
        ruleSet.setRuleNames( Collections.singletonList( getRuleScriptFile() ) );
        ruleSet.setValidationParameters( Collections.singletonMap( "versionPattern", "\\d\\.\\d" ) );

        return ruleSet;
    }

    @Override
    protected String getPackageType()
    {
        return PackageTypeConstants.PKG_TYPE_NPM;
    }

}
