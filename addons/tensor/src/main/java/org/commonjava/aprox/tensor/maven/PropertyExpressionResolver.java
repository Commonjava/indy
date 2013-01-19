package org.commonjava.aprox.tensor.maven;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropertyExpressionResolver
{

    private static final String EXPRESSION_PATTERN = "\\$\\{([^}]+)\\}";

    private final Properties props;

    public PropertyExpressionResolver( final Properties properties )
    {
        this.props = properties;
    }

    public String resolve( final String raw )
    {
        if ( raw == null )
        {
            return null;
        }

        final StringBuffer buffer = new StringBuffer( raw );
        final Set<String> missing = new HashSet<String>();
        boolean changed = false;

        do
        {
            changed = false;
            final Pattern pattern = Pattern.compile( EXPRESSION_PATTERN );
            final Matcher matcher = pattern.matcher( buffer.toString() );

            if ( matcher.find() )
            {
                buffer.setLength( 0 );
                matcher.reset();
            }
            else
            {
                return buffer.toString();
            }

            while ( matcher.find() )
            {
                final String k = matcher.group( 1 );
                if ( missing.contains( k ) )
                {
                    continue;
                }

                final String value = props.getProperty( k );
                if ( value != null )
                {
                    if ( value.contains( "${" ) )
                    {
                        matcher.appendReplacement( buffer, "" );
                        buffer.append( value );
                    }
                    else
                    {
                        matcher.appendReplacement( buffer, value );
                    }
                    changed = true;
                }
                else
                {
                    missing.add( k );
                }
            }

            matcher.appendTail( buffer );
        }
        while ( changed );

        return buffer.toString();
    }

}
