package org.commonjava.aprox.util;

public class StringFormat
{

    private final String format;

    public StringFormat( final String format, final Object... params )
    {
        this.format = format;
    }

    @Override
    public String toString()
    {
        return String.format( format.replaceAll( "\\{\\}", "%s" ) );
    }

}
