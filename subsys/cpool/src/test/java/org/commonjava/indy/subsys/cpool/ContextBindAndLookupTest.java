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
package org.commonjava.indy.subsys.cpool;

import org.junit.Test;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

import static javax.naming.Context.INITIAL_CONTEXT_FACTORY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ContextBindAndLookupTest
{
    @Test
    public void bindAndLookup()
            throws NamingException
    {
        String key = "java:/comp/env/foo";
        String val = "BAR";

        Properties properties = System.getProperties();
        properties.setProperty( INITIAL_CONTEXT_FACTORY, CPInitialContextFactory.class.getName() );
        System.setProperties( properties );

        bind( key, val );
        lookup( key, val );
    }

    private void lookup( final String key, final String val )
            throws NamingException
    {
        InitialContext ctx = new InitialContext();
        Object value = ctx.lookup( key );
        assertThat( value == val, equalTo( true ) );
    }

    private void bind( final String key, final String val )
            throws NamingException
    {
        InitialContext ctx = new InitialContext();
        ctx.bind( key, val );
    }
}
