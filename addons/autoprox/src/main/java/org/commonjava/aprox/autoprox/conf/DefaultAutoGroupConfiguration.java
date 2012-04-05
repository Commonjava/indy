package org.commonjava.aprox.autoprox.conf;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.commonjava.aprox.core.model.StoreKey;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "group" )
public class DefaultAutoGroupConfiguration
    implements AutoGroupConfiguration
{

    private final Logger logger = new Logger( getClass() );

    private List<StoreKey> extraConstituents;

    public DefaultAutoGroupConfiguration( final StoreKey... extraConstituents )
    {
        this.extraConstituents = Arrays.asList( extraConstituents );
    }

    public DefaultAutoGroupConfiguration()
    {
    }

    @Override
    public List<StoreKey> getExtraConstituents()
    {
        return extraConstituents;
    }

    @ConfigName( "append.constituents" )
    public void setExtraConstituentsString( final String constituentList )
    {
        if ( constituentList == null || constituentList.trim()
                                                       .length() < 1 )
        {
            return;
        }

        final String[] parts = constituentList.split( "\\s*,\\s*" );
        final List<StoreKey> constituents = new ArrayList<StoreKey>();
        for ( final String part : parts )
        {
            if ( part == null )
            {
                continue;
            }

            if ( part.trim()
                     .length() < 2 )
            {
                logger.error( "Invalid group constituent: not big enough to denote type and name: '%s'", part );
                continue;
            }

            final char c = part.charAt( 0 );
            final String subpart = part.substring( 1 );
            switch ( c )
            {
                case '>':
                {
                    constituents.add( new StoreKey( StoreType.deploy_point, subpart ) );
                    break;
                }
                case '<':
                {
                    constituents.add( new StoreKey( StoreType.repository, subpart ) );
                    break;
                }
                case '+':
                {
                    constituents.add( new StoreKey( StoreType.group, subpart ) );
                    break;
                }
                default:
                {
                    final int idx = part.indexOf( ':' );
                    if ( idx < 1 )
                    {
                        logger.error( "Invalid group constituent: '%s'. Valid formats include: >deploy-point, <repository, +group, repository:name, deploy_point:name, group:name",
                                      part );
                        continue;
                    }

                    final String st = part.substring( 0, idx );
                    try
                    {
                        if ( isEmpty( st ) )
                        {
                            logger.error( "Invalid group constituent: '%s'. Empty store type is not allowed (form: <type>:<name>).",
                                          part );
                            continue;
                        }

                        final StoreType type = StoreType.valueOf( st );

                        final String name = part.substring( idx + 1 );

                        if ( isEmpty( name ) )
                        {
                            logger.error( "Invalid group constituent: '%s'. Empty store name is not allowed (form: <type>:<name>).",
                                          part );
                            continue;
                        }

                        constituents.add( new StoreKey( type, name ) );
                    }
                    catch ( final IllegalArgumentException e )
                    {
                        logger.error( "Invalid group constituent: '%s'. Store type: '%s' is not valid.", part, st );
                        continue;
                    }
                }
            }
        }

        this.extraConstituents = constituents;
    }

    public void setExtraConstituents( final List<StoreKey> extraConstituents )
    {
        this.extraConstituents = extraConstituents;
    }

    public void setExtraConstituents( final StoreKey... extraConstituents )
    {
        this.extraConstituents = Arrays.asList( extraConstituents );
    }

}
