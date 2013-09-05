package org.commonjava.aprox.depgraph.maven;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Properties;

import org.commonjava.aprox.depgraph.maven.PropertyExpressionResolver;
import org.junit.Test;

public class PropertyExpressionResolverTest
{

    @Test
    public void resolveSimpleExpression()
    {
        final Properties p = new Properties();
        p.setProperty( "foo", "bar" );

        final PropertyExpressionResolver r = new PropertyExpressionResolver( p );
        final String result = r.resolve( "${foo}" );

        assertThat( result, equalTo( "bar" ) );
    }

    @Test
    public void resolveEmbeddedSimpleExpression()
    {
        final Properties p = new Properties();
        p.setProperty( "foo", "bar" );

        final PropertyExpressionResolver r = new PropertyExpressionResolver( p );
        final String result = r.resolve( "This ${foo} is a test" );

        assertThat( result, equalTo( "This bar is a test" ) );
    }

    @Test
    public void resolveTwoSimpleExpressions()
    {
        final Properties p = new Properties();
        p.setProperty( "foo", "bar" );
        p.setProperty( "bah", "blat" );

        final PropertyExpressionResolver r = new PropertyExpressionResolver( p );
        final String result = r.resolve( "${foo} ${bah}" );

        assertThat( result, equalTo( "bar blat" ) );
    }

    @Test
    public void resolveRecursiveSimpleExpressions()
    {
        final Properties p = new Properties();
        p.setProperty( "foo", "${bar}" );
        p.setProperty( "bar", "blat" );

        final PropertyExpressionResolver r = new PropertyExpressionResolver( p );
        final String result = r.resolve( "${foo}" );

        assertThat( result, equalTo( "blat" ) );
    }
}
