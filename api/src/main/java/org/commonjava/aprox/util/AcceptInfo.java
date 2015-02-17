package org.commonjava.aprox.util;

import static org.apache.commons.lang.StringUtils.join;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AcceptInfo
{
    public static final String ACCEPT_ANY = "*/*";

    public static final Set<String> ANY = Collections.unmodifiableSet( Collections.singleton( ACCEPT_ANY ) );

    private final String raw;

    private final String base;

    private final String version;

    public static AcceptInfoParser parser( final String appId, final String defaultVersion )
    {
        return new AcceptInfoParser( appId, defaultVersion );
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

    public static final class AcceptInfoParser
    {

        private final String appId;

        private final String defaultVersion;

        private AcceptInfoParser( final String appId, final String defaultVersion )
        {
            this.appId = appId;
            this.defaultVersion = defaultVersion;
        }

        public List<AcceptInfo> parse( final String... accepts )
        {
            return parse( Arrays.asList( accepts ) );
        }

        public List<AcceptInfo> parse( final Collection<String> accepts )
        {
            final Logger logger = LoggerFactory.getLogger( AcceptInfo.class );

            final List<String> raw = new ArrayList<String>();
            for ( final String accept : accepts )
            {
                final String[] parts = accept.split( "\\s*,\\s*" );
                if ( parts.length == 1 )
                {
                    logger.info( "adding atomic accept header: '{}'", accept );
                    raw.add( accept );
                }
                else
                {
                    logger.info( "Adding split header values: '{}'", join( parts, "', '" ) );
                    raw.addAll( Arrays.asList( parts ) );
                }
            }

            logger.info( "Got raw ACCEPT header values:\n  {}", join( raw, "\n  " ) );

            if ( raw == null || raw.isEmpty() )
            {
                return Collections.singletonList( new AcceptInfo( ACCEPT_ANY, ACCEPT_ANY, defaultVersion ) );
            }

            final List<AcceptInfo> acceptInfos = new ArrayList<AcceptInfo>();
            for ( final String r : raw )
            {
                String cleaned = r.toLowerCase();
                final int qIdx = cleaned.indexOf( ';' );
                if ( qIdx > -1 )
                {
                    // FIXME: We shouldn't discard quality suffix...
                    cleaned = cleaned.substring( 0, qIdx );
                }

                logger.info( "Cleaned up: {} to: {}", r, cleaned );

                final String appPrefix = "application/" + appId + "-";

                logger.info( "Checking for ACCEPT header starting with: '{}' and containing: '+' (header value is: '{}')",
                             appPrefix, cleaned );
                if ( cleaned.startsWith( appPrefix ) && cleaned.contains( "+" ) )
                {
                    final String[] acceptParts = cleaned.substring( appPrefix.length() )
                                                        .split( "\\+" );

                    acceptInfos.add( new AcceptInfo( cleaned, "application/" + acceptParts[1], acceptParts[0] ) );
                }
                else
                {
                    acceptInfos.add( new AcceptInfo( cleaned, cleaned, defaultVersion ) );
                }
            }

            return acceptInfos;
        }

        public List<AcceptInfo> parse( final Enumeration<String> accepts )
        {
            return parse( Collections.list( accepts ) );
        }

        public String getDefaultVersion()
        {
            return defaultVersion;
        }

    }

}
