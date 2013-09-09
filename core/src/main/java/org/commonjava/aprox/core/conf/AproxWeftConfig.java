package org.commonjava.aprox.core.conf;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.ws.rs.Produces;

import org.commonjava.aprox.conf.AbstractAproxMapConfig;
import org.commonjava.cdi.util.weft.config.DefaultWeftConfig;
import org.commonjava.web.config.ConfigurationException;

@ApplicationScoped
@Named( AproxWeftConfig.SECTION_NAME )
public class AproxWeftConfig
    extends AbstractAproxMapConfig
{

    public static final String SECTION_NAME = "threadpools";

    public static final String DEFAULT_THREADS = "defaultThreads";

    public static final String DEFAULT_PRIORITY = "defaultPriority";

    public static final String THREADS_SUFFIX = ".threads";

    public static final String PRIORITY_SUFFIX = ".priority";

    private final DefaultWeftConfig weftConfig = new DefaultWeftConfig();

    private final String numericPattern = "[0-9]+";

    public AproxWeftConfig()
    {
        super( SECTION_NAME );
    }

    @Override
    public void parameter( final String name, final String value )
        throws ConfigurationException
    {

        if ( !value.matches( numericPattern ) )
        {
            throw new ConfigurationException( "Invalid value: '%s' for parameter: '%s'. Only numeric values are accepted for section: '%s'.", value,
                                              name, SECTION_NAME );
        }

        final int v = Integer.parseInt( value );

        switch ( name )
        {
            case DEFAULT_THREADS:
            {
                weftConfig.configureDefaultThreads( v );
                break;
            }
            case DEFAULT_PRIORITY:
            {
                weftConfig.configureDefaultPriority( v );
                break;
            }
            default:
            {
                final int lastIdx = name.lastIndexOf( '.' );
                if ( lastIdx > -1 && name.length() > lastIdx )
                {
                    final String pool = name.substring( 0, lastIdx );
                    final String suffix = name.substring( lastIdx );

                    switch ( suffix )
                    {
                        case THREADS_SUFFIX:
                        {
                            weftConfig.configureThreads( pool, v );
                            break;
                        }
                        case PRIORITY_SUFFIX:
                        {
                            weftConfig.configurePriority( pool, v );
                            break;
                        }
                    }
                }
            }
        }
    }

    @Produces
    public DefaultWeftConfig getWeftConfig()
    {
        return weftConfig;
    }

    @Override
    public void sectionStarted( final String name )
        throws ConfigurationException
    {
        // NOP; just block map init in the underlying implementation.
    }

}
