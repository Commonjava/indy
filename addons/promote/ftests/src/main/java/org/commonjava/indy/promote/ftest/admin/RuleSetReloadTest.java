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
package org.commonjava.indy.promote.ftest.admin;

import org.commonjava.indy.promote.model.ValidationRuleSet;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Validate rules reload functions through REST endpoints
 * When <br />
 *
 *  <ol>
 *      <li>Added a new rule-set in fs</li>
 *      <li>Changed an existed rule-set in fs</li>
 *      <li>Deleted an existed rule-set in fs</li>
 *  </ol>
 *
 *  Then <br />
 *
 *  <ol>
 *      <li>Reloaded rule-sets through rest can get new added rule-set</li>
 *      <li>Reloaded rule-sets through rest can get changed rule-set's content</li>
 *      <li>Reloaded rule-sets through rest can detect deleted rule-set</li>
 *  </ol>
 *
 */
public class RuleSetReloadTest
        extends AbstractAdminValidationTest
{
    protected Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String RULE_SET_NEW = "new-rule-set.json";

    private static final String RULE_SET_CHANGE = "change-rule-set.json";

    private static final String RULE_SET_CHANGE_INIT_CONTENT_STR = "This is a changing test rule";

    private static final String RULE_SET_CHANGE_CHANGED_CONTENT_STR = "This is a changed test rule";

    @Test
    public void run()
            throws Exception
    {
        List<String> allRuleSets = module.getAllRuleSets();
        assertNotNull( allRuleSets );
        assertThat( allRuleSets.size(), equalTo( 1 ) );
        assertThat( allRuleSets, hasItem( RULE_SET_CHANGE ) );
        assertThat( allRuleSets.contains( RULE_SET_NEW ), equalTo( false ) );

        ValidationRuleSet ruleSet = module.getRuleSetByName( RULE_SET_CHANGE );
        assertThat( ruleSet.getName(), equalTo( RULE_SET_CHANGE ) );
        assertThat( ruleSet.getStoreKeyPattern(), equalTo( "group:test" ) );
        assertThat( ruleSet.getRuleNames(), hasItem( "change-rule.groovy" ) );
        assertThat( ruleSet.getRuleNames().contains( "changed-rule.groovy" ), equalTo( false ) );

        ruleSet = module.getRuleSetByName( RULE_SET_NEW );
        assertThat( ruleSet, nullValue() );

        deployRuleSet( RULE_SET_NEW, getNewRuleSet() );
        deployRuleSet( RULE_SET_CHANGE, getChangedRuleSet() );
        module.reloadRuleSets();

        allRuleSets = module.getAllRuleSets();
        assertThat( allRuleSets.size(), equalTo( 2 ) );
        assertThat( allRuleSets, hasItems( RULE_SET_NEW, RULE_SET_CHANGE ) );

        ruleSet = module.getRuleSetByName( RULE_SET_NEW );
        assertThat( ruleSet.getName(), equalTo( RULE_SET_NEW ) );
        assertThat( ruleSet.getStoreKeyPattern(), equalTo( "hosted:test" ) );
        assertThat( ruleSet.getRuleNames(), hasItem( "new-rule.groovy" ) );

        ruleSet = module.getRuleSetByName( RULE_SET_CHANGE );
        assertThat( ruleSet.getName(), equalTo( RULE_SET_CHANGE ) );
        assertThat( ruleSet.getStoreKeyPattern(), equalTo( "remote:test" ) );
        assertThat( ruleSet.getRuleNames(), hasItem( "changed-rule.groovy" ) );
        assertThat( ruleSet.getRuleNames().contains( "change-rule.groovy" ), equalTo( false ) );

        deleteRuleSet( RULE_SET_NEW );
        module.reloadRuleSets();

        allRuleSets = module.getAllRuleSets();
        assertNotNull( allRuleSets );
        assertThat( allRuleSets.size(), equalTo( 1 ) );
        assertThat( allRuleSets, hasItem( RULE_SET_CHANGE ) );
        assertThat( allRuleSets.contains( RULE_SET_NEW ), equalTo( false ) );

        ruleSet = module.getRuleSetByName( RULE_SET_NEW );
        assertThat( ruleSet, nullValue() );

        ruleSet = module.getRuleSetByName( RULE_SET_CHANGE );
        assertThat( ruleSet.getName(), equalTo( RULE_SET_CHANGE ) );
        assertThat( ruleSet.getStoreKeyPattern(), equalTo( "remote:test" ) );
        assertThat( ruleSet.getRuleNames(), hasItem( "changed-rule.groovy" ) );
        assertThat( ruleSet.getRuleNames().contains( "change-rule.groovy" ), equalTo( false ) );

    }

    private ValidationRuleSet getNewRuleSet()
    {
        ValidationRuleSet ruleSet = new ValidationRuleSet();
        ruleSet.setName( "Rule for " + RULE_SET_NEW );
        ruleSet.setStoreKeyPattern( "hosted:test" );
        ruleSet.setRuleNames( Collections.singletonList( "new-rule.groovy" ) );
        return ruleSet;

    }

    private ValidationRuleSet getInitChangeRuleSet()
    {
        ValidationRuleSet ruleSet = new ValidationRuleSet();
        ruleSet.setName( "Rule for " + RULE_SET_CHANGE );
        ruleSet.setStoreKeyPattern( "group:test" );
        ruleSet.setRuleNames( Collections.singletonList( "change-rule.groovy" ) );
        return ruleSet;
    }

    private ValidationRuleSet getChangedRuleSet()
    {
        ValidationRuleSet ruleSet = new ValidationRuleSet();
        ruleSet.setName( "Rule for " + RULE_SET_CHANGE + " with changing" );
        ruleSet.setStoreKeyPattern( "remote:test" );
        ruleSet.setRuleNames( Collections.singletonList( "changed-rule.groovy" ) );
        return ruleSet;
    }

    @Override
    protected Map<String, String> getRuleScriptFiles()
    {
        return Collections.emptyMap();

    }

    @Override
    protected Map<String, ValidationRuleSet> getRuleSets()
    {
        Map<String, ValidationRuleSet> ruleSets = new HashMap<>();
        ruleSets.put( RULE_SET_CHANGE, getInitChangeRuleSet() );
        return ruleSets;
    }

}
