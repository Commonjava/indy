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
