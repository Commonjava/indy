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

import org.commonjava.indy.promote.model.ValidationRuleDTO;
import org.commonjava.indy.promote.model.ValidationRuleSet;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
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
 *      <li>Added a new rule in fs</li>
 *      <li>Changed an existed rule in fs</li>
 *      <li>Deleted an existed rule in fs</li>
 *  </ol>
 *
 *  Then <br />
 *
 *  <ol>
 *      <li>Reloaded rules through rest can get new added rule</li>
 *      <li>Reloaded rules through rest can get changed rule's content</li>
 *      <li>Reloaded rules through rest can detect deleted rule</li>
 *  </ol>
 *
 */
public class RuleReloadTest
        extends AbstractAdminValidationTest
{
    protected Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String RULE_NEW = "new-rule.groovy";

    private static final String RULE_CHNAGE = "change-rule.groovy";

    private static final String RULE_CHANGE_INIT_CONTENT_STR = "This is a changing test rule";
    private static final String RULE_CHANGE_CHANGED_CONTENT_STR = "This is a changed test rule";

    @Test
    public void run()
            throws Exception
    {
        List<String> allRules = module.getAllRules();
        assertNotNull( allRules );
        assertThat( allRules.size(), equalTo( 1 ) );
        assertThat( allRules, hasItem( RULE_CHNAGE ) );
        assertThat( allRules.contains( RULE_NEW ), equalTo( false ) );

        ValidationRuleDTO rule = module.getRuleByName( RULE_CHNAGE );
        assertThat( rule.getName(), equalTo( RULE_CHNAGE ) );
        assertThat( rule.getSpec(), containsString( RULE_CHANGE_INIT_CONTENT_STR ) );
        assertThat( rule.getSpec().contains( RULE_CHANGE_CHANGED_CONTENT_STR ), equalTo( false ) );

        rule = module.getRuleByName( RULE_NEW );
        assertThat( rule, nullValue() );

        deployRule( RULE_NEW, getNewRuleContent() );
        deployRule( RULE_CHNAGE, getChangedRuleContent() );
        module.reloadRules();

        allRules = module.getAllRules();
        assertThat( allRules.size(), equalTo( 2 ) );
        assertThat( allRules, hasItems( RULE_NEW, RULE_CHNAGE ) );

        rule = module.getRuleByName( RULE_NEW );
        assertThat( rule.getName(), equalTo( RULE_NEW ) );
        assertThat( rule.getSpec(), containsString( "This is a new test rule" ) );

        rule = module.getRuleByName( RULE_CHNAGE );
        assertThat( rule.getName(), equalTo( RULE_CHNAGE ) );
        assertThat( rule.getSpec(), containsString( RULE_CHANGE_CHANGED_CONTENT_STR ) );
        assertThat( rule.getSpec().contains( RULE_CHANGE_INIT_CONTENT_STR ), equalTo( false ) );

        deleteRule( RULE_NEW );
        module.reloadRules();

        allRules = module.getAllRules();
        assertNotNull( allRules );
        assertThat( allRules.size(), equalTo( 1 ) );
        assertThat( allRules, hasItem( RULE_CHNAGE ) );
        assertThat( allRules.contains( RULE_NEW ), equalTo( false ) );

        rule = module.getRuleByName( RULE_NEW );
        assertThat( rule, nullValue() );

        rule = module.getRuleByName( RULE_CHNAGE );
        assertThat( rule.getName(), equalTo( RULE_CHNAGE ) );
        assertThat( rule.getSpec(), containsString( RULE_CHANGE_CHANGED_CONTENT_STR ) );
        assertThat( rule.getSpec().contains( RULE_CHANGE_INIT_CONTENT_STR ), equalTo( false ) );

    }

    private String getNewRuleContent()
    {
        /* @formatter:off */
        return "package org.commonjava.indy.promote.rules;"
            + "import org.commonjava.indy.promote.validate.model.ValidationRequest;"
            + "import org.commonjava.indy.promote.validate.model.ValidationRule;"
            + "class NewRule implements ValidationRule {"
            + " String validate(ValidationRequest request) {"
            + "     return \"This is a new test rule\";"
            + " }"
            + "}";
        /* @formatter:on */
    }

    private String getInitChangeRuleContent()
    {
        /* @formatter:off */
        return "package org.commonjava.indy.promote.rules;"
                + "import org.commonjava.indy.promote.validate.model.ValidationRequest;"
                + "import org.commonjava.indy.promote.validate.model.ValidationRule;"
                + "class ChangeRule implements ValidationRule {"
                + " String validate(ValidationRequest request) {"
                + "     return \"" + RULE_CHANGE_INIT_CONTENT_STR + "\";"
                + " }"
                + "}";
        /* @formatter:on */
    }

    private String getChangedRuleContent()
    {
        /* @formatter:off */
        return "package org.commonjava.indy.promote.rules;"
                + "import org.commonjava.indy.promote.validate.model.ValidationRequest;"
                + "import org.commonjava.indy.promote.validate.model.ValidationRule;"
                + "class ChangeRule implements ValidationRule {"
                + " String validate(ValidationRequest request) {"
                + "     return \"" + RULE_CHANGE_CHANGED_CONTENT_STR + "\";"
                + " }"
                + "}";
        /* @formatter:on */
    }

    @Override
    protected Map<String, String> getRuleScriptFiles()
    {
        Map<String, String> scripts = new HashMap<>();
        scripts.put( RULE_CHNAGE, getInitChangeRuleContent() );
        return scripts;
    }

    @Override
    protected Map<String, ValidationRuleSet> getRuleSets()
    {
        return Collections.emptyMap();
    }

}
