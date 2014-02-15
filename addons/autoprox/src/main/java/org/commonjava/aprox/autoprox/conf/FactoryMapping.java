package org.commonjava.aprox.autoprox.conf;

public class FactoryMapping
{

    public static final String DEFAULT_MATCH = "default";

    private final String match;

    private final AutoProxFactory factory;

    public FactoryMapping( final String match, final AutoProxFactory factory )
    {
        this.match = match;
        this.factory = factory;
    }

    public String getMatch()
    {
        return match;
    }

    public AutoProxFactory getFactory()
    {
        return factory;
    }

    public boolean matchesName( final String name )
    {
        if ( match.length() > 2 && match.charAt( 0 ) == '/' && match.charAt( match.length() - 1 ) == '/' )
        {
            return name.matches( match.substring( 1, match.length() - 1 ) );
        }
        else if ( match.endsWith( "*" ) )
        {
            if ( match.length() == 1 )
            {
                return true;
            }
            else
            {
                return name.startsWith( match.substring( 0, match.length() - 1 ) );
            }
        }
        else if ( DEFAULT_MATCH.equalsIgnoreCase( match ) )
        {
            return true;
        }

        return false;
    }

}
