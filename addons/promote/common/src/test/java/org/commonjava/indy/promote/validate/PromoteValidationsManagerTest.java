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
package org.commonjava.indy.promote.validate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.promote.conf.PromoteConfig;
import org.commonjava.indy.promote.model.ValidationRuleSet;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.indy.subsys.datafile.change.DataFileEventManager;
import org.commonjava.indy.subsys.template.ScriptEngine;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 9/14/15.
 */
public class PromoteValidationsManagerTest
{
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private ValidationRuleParser parser;

    private DataFileManager fileManager;

    private PromoteValidationsManager promoteValidations;

    private PromoteConfig config;

    @Before
    public void setUp()
            throws Exception
    {
        fileManager = new DataFileManager( temp.newFolder( "data" ), new DataFileEventManager() );
        parser = new ValidationRuleParser( new ScriptEngine( fileManager ), new ObjectMapper() );
        config = new PromoteConfig();
        config.setEnabled( true );
    }

    @Test
    public void testRuleSetParseAndMatchOnStoreKey()
            throws Exception
    {
        DataFile dataFile = fileManager.getDataFile( "promote/rule-sets/test.json" );
        dataFile.writeString( "{\"name\":\"test\",\"storeKeyPattern\":\".*\"}",
                              new ChangeSummary( ChangeSummary.SYSTEM_USER, "writing test data" ) );

        promoteValidations = new PromoteValidationsManager( fileManager, config, parser );

        ValidationRuleSet ruleSet = promoteValidations.getRuleSetMatching( new StoreKey( StoreType.hosted, "repo" ) );

        assertThat( ruleSet, notNullValue() );
        assertThat( ruleSet.matchesKey( "hosted:repo" ), equalTo( true ) );
    }
}
