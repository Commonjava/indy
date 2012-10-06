package org.commonjava.aprox.filer;

public final class PathUtils
{
    private PathUtils()
    {
    }

    public static String join( final String base, final String... parts )
    {
        if ( parts.length < 1 )
        {
            return base;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append( base );

        for ( final String part : parts )
        {
            final String[] subParts = part.split( "/" );
            for ( final String subPart : subParts )
            {
                if ( subPart.trim()
                            .length() < 1 )
                {
                    continue;
                }

                sb.append( "/" )
                  .append( subPart.replace( '\\', '/' ) );
            }
        }

        return sb.toString();
    }

}
