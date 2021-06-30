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
package org.commonjava.indy.promote.ftest.admin;

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.promote.model.ValidationRuleDTO;
import org.commonjava.indy.promote.model.ValidationRuleSet;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Validate rules & ruleset get functions through REST endpoints
 * When <br />
 *
 *  <ol>
 *      <li>Deployed rules files and rule-sets files</li>
 *  </ol>
 *
 *  Then <br />
 *
 *  <ol>
 *      <li>Can get all rule & rule-set file names through REST endpoints</li>
 *      <li>Can get rule file content through REST endpoints</li>
 *      <li>Can get rule-set file content through REST endpoints</li>
 *  </ol>
 *
 */
public class RuleAndRuleSetGetTest
        extends AbstractAdminValidationTest
{
    protected Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String RULE1 = "maven-artifact-refs-via.groovy";

    private static final String RULE2 = "maven-no-pre-existing-paths.groovy";

    private static final String PREFIX = "artifact-refs-via/";

    private static final String RULESET_TEST_NAME = "test";

    private static final String RULESET_TEST_STOREKEY = "maven:group:test";

    @Test
    public void run()
            throws Exception
    {
        getRuleScriptFiles().forEach( ( name, content ) -> {
            ValidationRuleDTO rule = null;
            try
            {
                rule = module.getRuleByName( name );
            }
            catch ( IndyClientException e )
            {
                logger.error( "Exception happened!", e );
            }
            assertNotNull( rule );
            assertThat( rule.getName(), equalTo( name ) );
            assertNotNull( rule.getSpec() );
            assertThat( rule.getSpec().length() > 0, equalTo( true ) );
        } );

        getRuleSets().forEach( ( name, set ) -> {
            ValidationRuleSet ruleSet = null;
            try
            {
                ruleSet = module.getRuleSetByName( name );
            }
            catch ( IndyClientException e )
            {
                logger.error( "Exception happened!", e );
            }
            assertNotNull( ruleSet );
            assertThat( ruleSet.getName(), containsString( name ) );
            assertThat( ruleSet.getStoreKeyPattern(), equalTo( RULESET_TEST_STOREKEY ) );
        } );

        ValidationRuleSet ruleSet = null;
        try
        {
            ruleSet = module.getRuleSetByStoreKey( StoreKey.fromString( RULESET_TEST_STOREKEY ) );
        }
        catch ( IndyClientException e )
        {
            logger.error( "Exception happened!", e );
        }
        assertNotNull( ruleSet );
        assertThat( ruleSet.getName(), containsString( RULESET_TEST_NAME ) );
        assertThat( ruleSet.getStoreKeyPattern(), equalTo( RULESET_TEST_STOREKEY ) );

        List<String> ruleNames = module.getAllRules();
        assertThat( ruleNames.size(), equalTo( 2 ) );
        assertThat( ruleNames, CoreMatchers.hasItems( RULE1, RULE2 ) );

        List<String> ruleSetNames = module.getAllRuleSets();
        assertThat( ruleSetNames.size(), equalTo( 1 ) );
        assertThat( ruleSetNames.get( 0 ), containsString( "test" ) );
    }

    @Override
    protected Map<String, String> getRuleScriptFiles()
            throws IOException
    {
        String basePath = "promote/rules/";
        Map<String, String> scripts = new HashMap<>();
        scripts.put( RULE1, readTestResource( basePath + RULE1 ) );
        scripts.put( RULE2, readTestResource( basePath + RULE2 ) );
        return scripts;
    }

    @Override
    protected Map<String, ValidationRuleSet> getRuleSets()
            throws IOException
    {
        ValidationRuleSet ruleSet = new ValidationRuleSet();
        ruleSet.setName( RULESET_TEST_NAME );
        ruleSet.setStoreKeyPattern( RULESET_TEST_STOREKEY );
        ruleSet.setRuleNames( new ArrayList<>( getRuleScriptFiles().keySet() ) );

        ruleSet.setValidationParameters( Collections.singletonMap( "availableInStores", "group:other" ) );

        return Collections.singletonMap( ruleSet.getName(), ruleSet );
    }

}
