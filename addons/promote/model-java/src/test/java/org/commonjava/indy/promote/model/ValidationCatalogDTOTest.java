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

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 4/25/16.
 */
public class ValidationCatalogDTOTest
{
    @Test
    public void jsonRoundTrip()
            throws IOException
    {
        ValidationRuleDTO firstRule = readRule( "no-snapshots.groovy" );
        ValidationRuleDTO secondRule = readRule( "parsable-pom.groovy" );

        Map<String, ValidationRuleDTO> rules = Stream.of( firstRule, secondRule )
                                                       .collect( Collectors.toMap( ValidationRuleDTO::getName,
                                                                                   Function.identity() ) );

        String rsName = "test";
        ValidationRuleSet rs =
                new ValidationRuleSet( rsName, "remote:.*", Arrays.asList( firstRule.getName(), secondRule.getName() ),
                                       Collections.emptyMap() );

        ValidationCatalogDTO in = new ValidationCatalogDTO( true, rules,
                                                            Collections.singletonMap( rsName, rs ) );

        IndyObjectMapper mapper = new IndyObjectMapper( true );

        String json = mapper.writeValueAsString( in );

        ValidationCatalogDTO out = mapper.readValue( json, ValidationCatalogDTO.class );

        assertThat( out, notNullValue() );
        assertThat( out.isEnabled(), equalTo( in.isEnabled() ) );

        Map<String, ValidationRuleDTO> outRules = out.getRules();

        assertThat( rules, notNullValue() );
        assertThat( rules.size(), equalTo( 2 ) );

        assertThat( rules.get( firstRule.getName() ), equalTo( firstRule ) );
        assertThat( rules.get( secondRule.getName() ), equalTo( secondRule ) );

        Map<String, ValidationRuleSet> outRuleSets = out.getRuleSets();

        assertThat( outRuleSets, notNullValue() );
        assertThat( outRuleSets.size(), equalTo( 1 ) );

        assertThat( outRuleSets.get( rsName ), equalTo( rs ) );
    }

    private ValidationRuleDTO readRule( String resource )
            throws IOException
    {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream( resource ))
        {
            String spec = IOUtils.toString( stream );
            return new ValidationRuleDTO( resource, spec );
        }
    }
}
