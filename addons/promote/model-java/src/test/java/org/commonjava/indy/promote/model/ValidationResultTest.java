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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 4/25/16.
 */
public class ValidationResultTest
{

    @Test
    public void jsonRoundTrip_Passing()
            throws IOException
    {
        ValidationResult in = new ValidationResult();
        in.setValid( true );
        in.setRuleSet( "some-rules.json" );

        assertRoundTrip( in, null );
    }

    @Test
    public void jsonRoundTrip_FailedOneError()
            throws IOException
    {
        ValidationResult in = new ValidationResult();
        in.setValid( false );
        in.setRuleSet( "some-rules.json" );

        String rule = "my-validator.groovy";
        in.addValidatorError( rule, "This is a test error." );

        assertRoundTrip( in, (out)->{
            Map<String, String> errors = out.getValidatorErrors();
            assertThat( errors.size(), equalTo( 1 ) );
            assertThat( errors.get( rule ), equalTo( in.getValidatorErrors().get( rule ) ) );
        } );
    }

    private void assertRoundTrip( ValidationResult in, Consumer<ValidationResult> extraAssertions )
            throws IOException
    {
        IndyObjectMapper mapper = new IndyObjectMapper( true );
        String json = mapper.writeValueAsString( in );

        ValidationResult out = mapper.readValue( json, ValidationResult.class );

        assertThat( out, notNullValue() );
        assertThat( out.isValid(), equalTo( in.isValid() ) );
        assertThat( out.getRuleSet(), equalTo( in.getRuleSet() ) );

        if ( extraAssertions != null )
        {
            extraAssertions.accept( out );
        }

        assertThat( out, equalTo( in ) );
    }
}
