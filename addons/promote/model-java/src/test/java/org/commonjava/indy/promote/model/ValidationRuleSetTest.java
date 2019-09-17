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
package org.commonjava.indy.promote.model;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 9/14/15.
 */
public class ValidationRuleSetTest
{
    @Test
    public void matchesHostedRepoWithDotStarPattern()
    {
        ValidationRuleSet ruleSet = new ValidationRuleSet( "test", ".*", Collections.emptyList(), Collections.emptyMap() );

        assertThat( ruleSet.matchesKey( new StoreKey( StoreType.hosted, "repo" ).toString() ), equalTo( true ) );
    }

}
