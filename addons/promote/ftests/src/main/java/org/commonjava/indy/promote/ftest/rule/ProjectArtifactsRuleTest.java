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
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 9/23/15.
 */
public class ProjectArtifactsRuleTest
        extends AbstractValidationRuleTest<Group>
{

    private static final String RULE = "project-artifacts.groovy";

    private static final String CONTENT = "this is some content";

//    @Override
//    protected int getTestTimeoutMultiplier()
//    {
//        return 3;
//    }

    @Test
    @Category( EventDependent.class )
    public void run()
            throws Exception
    {
        String invalidGav = "org.foo:invalid:1";
        String validGav = "org.foo:valid:1.1";

        String invalidPom = "org/foo/invalid/1/invalid-1.pom";
        String invalidJar = "org/foo/invalid/1/invalid-1.jar";
        String validPom = "org/foo/valid/1.1/valid-1.1.pom";
        String validJar = "org/foo/valid/1.1/valid-1.1.jar";
        String validSources = "org/foo/valid/1.1/valid-1.1-sources.jar";
        String validJavadocs = "org/foo/valid/1.1/valid-1.1-javadoc.jar";

        deploy( invalidPom, CONTENT );
        deploy( invalidJar, CONTENT );
        deploy( validPom, CONTENT );
        deploy( validJar, CONTENT );
        deploy( validSources, CONTENT );
        deploy( validJavadocs, CONTENT );

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
        assertThat( errors.contains( validGav ), equalTo( false ) );
        assertThat( errors.contains( invalidGav ), equalTo( true ) );
    }

    public ProjectArtifactsRuleTest()
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
        ruleSet.setValidationParameters( Collections.singletonMap( "classifierAndTypeSet", "sources:jar,javadoc:jar" ) );

        return ruleSet;
    }

}
