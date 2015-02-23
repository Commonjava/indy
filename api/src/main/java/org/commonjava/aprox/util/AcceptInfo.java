package org.commonjava.aprox.util;

import java.util.Collections;
import java.util.Set;

public class AcceptInfo
{
    public static final String ACCEPT_ANY = "*/*";

    public static final Set<String> ANY = Collections.unmodifiableSet( Collections.singleton( ACCEPT_ANY ) );

    private final String raw;

    private final String base;

    private final String version;

    public static AcceptInfoParser parser()
    {
        return new AcceptInfoParser();
    }

    public AcceptInfo( final String raw, final String base, final String version )
    {
        this.raw = raw;
        this.base = base;
        this.version = version;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( base == null ) ? 0 : base.hashCode() );
        result = prime * result + ( ( raw == null ) ? 0 : raw.hashCode() );
        result = prime * result + ( ( version == null ) ? 0 : version.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final AcceptInfo other = (AcceptInfo) obj;
        if ( base == null )
        {
            if ( other.base != null )
            {
                return false;
            }
        }
        else if ( !base.equals( other.base ) )
        {
            return false;
        }
        if ( raw == null )
        {
            if ( other.raw != null )
            {
                return false;
            }
        }
        else if ( !raw.equals( other.raw ) )
        {
            return false;
        }
        if ( version == null )
        {
            if ( other.version != null )
            {
                return false;
            }
        }
        else if ( !version.equals( other.version ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "AcceptInfo [raw=%s, base=%s, version=%s]", raw, base, version );
    }

    public String getRawAccept()
    {
        return raw;
    }

    public String getBaseAccept()
    {
        return base;
    }

    public String getVersion()
    {
        return version;
    }

}
