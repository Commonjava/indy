package org.commonjava.indy.core.expire;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduleManagerUtils
{

    public static String groupName( final StoreKey key, final String jobType )
    {
        return key.toString() + groupNameSuffix( jobType );
    }

    public static String groupNameSuffix( final String jobType )
    {
        return "#" + jobType;
    }

    public static StoreKey storeKeyFrom( final String group )
    {
        final String[] parts = group.split( "#" );
        if ( parts.length > 1 )
        {
            final Logger logger = LoggerFactory.getLogger( DefaultScheduleManager.class );
            StoreKey storeKey = null;
            try
            {
                storeKey = StoreKey.fromString( parts[0] );
            }
            catch ( IllegalArgumentException e )
            {
                logger.warn( "Not a store key for string: {}", parts[0] );
            }

            //TODO this part of code may be obsolete, will need further check then remove
            if ( storeKey == null )
            {
                logger.info( "Not a store key for string: {}, will parse as store type", parts[0] );
                final StoreType type = StoreType.get( parts[0] );
                if ( type != null )
                {
                    storeKey = new StoreKey( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, type, parts[1] );
                }
            }
            return storeKey;
        }

        return null;
    }

}
