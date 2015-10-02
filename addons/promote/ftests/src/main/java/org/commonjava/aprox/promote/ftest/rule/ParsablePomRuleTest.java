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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.promote.model.GroupPromoteRequest;
import org.commonjava.aprox.promote.model.GroupPromoteResult;
import org.commonjava.aprox.promote.model.ValidationResult;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 9/23/15.
 */
public class ParsablePomRuleTest
        extends AbstractValidationRuleTest<Group>
{

    private static final String RULE = "parsable-pom.groovy";

    @Test
    public void run()
            throws Exception
    {
        String invalid = "org/foo/invalid/1/invalid-1.pom";
        String valid = "org/foo/valid/1/valid-1.pom";

        deploy( invalid, "This is not parsable" );
        deploy( valid, "<?xml version=\"1.0\"?>\n<project><modelVersion>4.0.0</modelVersion><groupId>org.foo</groupId>"
                        + "<artifactId>valid</artifactId><version>1</version></project>" );

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

    public ParsablePomRuleTest()
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
}
