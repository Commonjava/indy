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
                final String normal = normalizePathPart( subPart );
                if ( normal.length() < 1 )
                {
                    continue;
                }

                sb.append( "/" )
                  .append( normal );
            }
        }

        return sb.toString();
    }

    public static String normalizePathPart( final String path )
    {
        String result = path.trim();
        while ( result.startsWith( "/" ) && result.length() > 1 )
        {
            result = result.substring( 1 );
        }

        return result.replace( '\\', '/' );
    }
}
