package org.commonjava.aprox.autoprox.conf;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.commonjava.aprox.core.model.StoreKey;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.ConfigNames;
import org.commonjava.web.config.annotation.SectionName;
import org.commonjava.web.config.section.ConfigurationSectionListener;

@SectionName( ConfigurationSectionListener.DEFAULT_SECTION )
public class DefaultAutoProxConfiguration
    implements AutoProxConfiguration
{

    public final Logger logger = new Logger( getClass() );

    private final String baseUrl;

    private List<StoreKey> extraGroupConstituents;

    private boolean createdWithDeployPoint;

    private boolean enabled;

    @ConfigNames( "baseUrl" )
    public DefaultAutoProxConfiguration( final String baseUrl )
    {
        this.baseUrl = baseUrl;
    }

    public DefaultAutoProxConfiguration( final String baseUrl, final boolean createdWithDeployPoint,
                                         final List<StoreKey> extraGroupConstituents )
    {
        this.baseUrl = baseUrl;
        this.createdWithDeployPoint = createdWithDeployPoint;
        this.extraGroupConstituents = extraGroupConstituents;
    }

    public DefaultAutoProxConfiguration( final String baseUrl, final boolean createdWithDeployPoint,
                                         final StoreKey... extraGroupConstituents )
    {
        this.baseUrl = baseUrl;
        this.createdWithDeployPoint = createdWithDeployPoint;
        this.extraGroupConstituents = Arrays.asList( extraGroupConstituents );
    }

    @Override
    public String getBaseUrl()
    {
        return baseUrl;
    }

    @Override
    public List<StoreKey> getExtraGroupConstituents()
    {
        return extraGroupConstituents;
    }

    @Override
    public boolean isDeploymentAllowed()
    {
        return createdWithDeployPoint;
    }

    @ConfigName( "extraGroupConstituents" )
    public void setExtraGroupConstituentsString( final String constituentList )
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

        this.extraGroupConstituents = constituents;
    }

    public void setExtraGroupConstituents( final List<StoreKey> extraGroupConstituents )
    {
        this.extraGroupConstituents = extraGroupConstituents;
    }

    @ConfigName( "allowDeployment" )
    public void setDeploymentAllowed( final boolean createdWithDeployPoint )
    {
        this.createdWithDeployPoint = createdWithDeployPoint;
    }

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    @Override
    @ConfigName( "enabled" )
    public void setEnabled( final boolean enabled )
    {
        logger.info( "Set enabled: %s for autoprox.", enabled );
        this.enabled = enabled;
    }

}
