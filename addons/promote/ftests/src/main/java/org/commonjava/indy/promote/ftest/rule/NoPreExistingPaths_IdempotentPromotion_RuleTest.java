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

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.promote.client.IndyPromoteClientModule;
import org.commonjava.indy.promote.ftest.AbstractAsyncPromotionManagerTest;
import org.commonjava.indy.promote.ftest.AbstractPromotionManagerTest;
import org.commonjava.indy.promote.model.GroupPromoteRequest;
import org.commonjava.indy.promote.model.GroupPromoteResult;
import org.commonjava.indy.promote.model.PathsPromoteRequest;
import org.commonjava.indy.promote.model.PathsPromoteResult;
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
import java.util.Set;

import static org.commonjava.indy.promote.model.AbstractPromoteResult.ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class NoPreExistingPaths_IdempotentPromotion_RuleTest
                extends AbstractValidationRuleTest<HostedRepository>
{

    private static final String RULE = "maven-no-pre-existing-paths.groovy";

    private static final String PREFIX = "no-pre-existing-paths/";

    public NoPreExistingPaths_IdempotentPromotion_RuleTest()
    {
        super( HostedRepository.class );
    }

    @Test
    public void run()
                    throws Exception
    {

        String path = "org/foo/valid/1.1/valid-1.1.pom";

        // Deploy the resource before promotion
        deployResource( target.getKey(), path, PREFIX + "valid.pom.xml");

        try(InputStream stream = client.content().get( target.getKey(), path ))
        {
            String retrieved = IOUtils.toString( stream );
            assertThat( path + " path from: " + target.getKey(), retrieved,
                        equalTo( resourceToString( PREFIX + "valid.pom.xml" ) ) );
        }

        deployResource( path, PREFIX + "valid.pom.xml" );

        waitForEventPropagation();

        // Promotion the same file(with the same checksum)
        PathsPromoteResult result =
                        client.module( IndyPromoteClientModule.class )
                              .promoteByPath( new PathsPromoteRequest( source.getKey(), target.getKey(), path ).setPurgeSource( true ) );

        assertThat( result.getRequest()
                          .getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest()
                          .getTarget(), equalTo( target.getKey() ) );

        assertThat( result.getError(), nullValue() );

        Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        Set<String> skipped = result.getSkippedPaths();

        assertThat( skipped, notNullValue() );
        assertThat( skipped.size(), equalTo( 1 ) );

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
        ruleSet.setStoreKeyPattern( "hosted:target" );
        ruleSet.setRuleNames( Collections.singletonList( getRuleScriptFile() ) );
        return ruleSet;
    }

}
