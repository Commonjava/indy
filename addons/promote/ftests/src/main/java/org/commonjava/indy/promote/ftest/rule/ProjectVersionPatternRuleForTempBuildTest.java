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

import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.promote.model.GroupPromoteRequest;
import org.commonjava.indy.promote.model.GroupPromoteResult;
import org.commonjava.indy.promote.model.ValidationResult;
import org.commonjava.indy.promote.model.ValidationRuleSet;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Validate version pattern rules for a complex pattern (of temporary build)
 * When <br />
 *
 *  <ol>
 *    <li>Hosted source and target group</li>
 *    <li>source contains bunch of jar, javadoc, source jars</li>
 *    <li>promotion validation rule-set that includes project-version-pattern.groovy, which predefine pattern
 *    to limit some of the artifacts in source</li>
 *    <li>by-group promotion request posted</li>
 *  </ol>
 *
 *  Then <br />
 *
 *  <ol>
 *    <li>promotion failed, and validation result contains errors for un-matched artifacts against pattern</li>
 *  </ol>
 *
 */
public class ProjectVersionPatternRuleForTempBuildTest
        extends AbstractValidationRuleTest<Group>
{

    private static final String RULE = "project-version-pattern.groovy";

    @Test
    @Category( EventDependent.class )
    public void run()
            throws Exception
    {
        String invalidJar = "org/foo/invalid/1.0.0.redhat-00004/invalid-1.0.0.redhat-00004.jar";
        String validJar = "org/foo/valid/1.0.0.temporary-redhat-00004/valid-1.0.0.temporary-redhat-00004.jar";
        String invalidDocJar = "org/foo/invalid/1.0.0.redhat-00004/invalid-1.0.0.redhat-00004-javadoc.jar";
        String validDocJar = "org/foo/valid/1.0.0.temporary-redhat-00004/valid-1.0.0.temporary-redhat-00004-javadoc.jar";
        String invalidSrcJar = "org/foo/invalid/1.0.0.redhat-00004/invalid-1.0.0.redhat-00004-sources.jar";
        String validSrcJar = "org/foo/valid/1.0.0.temporary-redhat-00004/valid-1.0.0.temporary-redhat-00004-sources.jar";
//        Set<String> paths = new HashSet<>( 6 );
//        paths.add(invalidJar);
//        paths.add(validJar);
//        paths.add(invalidDocJar);
//        paths.add(validDocJar);
//        paths.add(invalidSrcJar);
//        paths.add(validSrcJar);

        deploy( invalidJar, "just for test of invalid jar" );
        deploy( validJar, "just for test of valid jar" );
        deploy( invalidDocJar, "just for test of invalid javadoc jar" );
        deploy( validDocJar, "just for test of valid javadoc jar" );
        deploy( invalidSrcJar, "just for test of invalid source jar" );
        deploy( validSrcJar, "just for test of valid source jar" );

        waitForEventPropagation();

        GroupPromoteRequest request = new GroupPromoteRequest( source.getKey(), target.getName() );
        GroupPromoteResult result = module.promoteToGroup( request );
//        PathsPromoteRequest request = new PathsPromoteRequest( source.getKey(), target.getKey(), paths  );
//        PathsPromoteResult result = module.promoteByPath( request );
        assertThat( result, notNullValue() );

        ValidationResult validations = result.getValidations();
        assertThat( validations, notNullValue() );
        assertThat(validations.isValid(), equalTo( false ));

        Map<String, String> validatorErrors = validations.getValidatorErrors();
        assertThat( validatorErrors, notNullValue() );
        assertThat( validatorErrors.size(), not( 0 ) );

        logger.info( "Validation error:\n{}", validatorErrors );

        String errors = validatorErrors.get( RULE );
        assertThat( errors, notNullValue() );

        assertThat( errors.contains( validJar ), equalTo( false ) );
        assertThat( errors.contains( invalidJar ), equalTo( true ) );
        assertThat( errors.contains( validDocJar ), equalTo( false ) );
        assertThat( errors.contains( invalidDocJar ), equalTo( true ) );
        assertThat( errors.contains( validSrcJar ), equalTo( false ) );
        assertThat( errors.contains( invalidSrcJar ), equalTo( true ) );
    }

    public ProjectVersionPatternRuleForTempBuildTest()
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

    protected ValidationRuleSet getRuleSet()
    {
        ValidationRuleSet ruleSet = new ValidationRuleSet();
        ruleSet.setName( "test" );
        ruleSet.setStoreKeyPattern( "group:target" );
        ruleSet.setRuleNames( Collections.singletonList( getRuleScriptFile() ) );
        Map<String, String> params = new HashMap<>( 2 );
        params.put( "classifierAndTypeSet", "javadoc:jar, sources:jar" );
        params.put( "versionPattern",
                    "\\d+\\.\\d+\\.\\d+\\.(?:[\\w_-]+-)?(?:(?:temporary)|(?:t\\d{8}-\\d{6}-\\d{3}))-redhat-\\d+" );
        ruleSet.setValidationParameters( params );

        return ruleSet;
    }

}
