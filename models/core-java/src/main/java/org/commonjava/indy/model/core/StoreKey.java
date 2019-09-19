/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.model.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;

public final class StoreKey
    implements Serializable, Comparable<StoreKey>
{
    private static final long serialVersionUID = 1L;

    private String packageType;

    private final StoreType type;

    private final String name;

    public StoreKey( final String packageType, final StoreType type, final String name )
    {
        if ( !PackageTypes.contains( packageType ) )
        {
            throw new IllegalArgumentException( "Unsupported package type: " + packageType + ". Valid values are: "
                                                        + PackageTypes.getPackageTypes() );
        }

        this.packageType = packageType;
        this.type = type;
        this.name = name;
    }

    @Deprecated
    public StoreKey( final StoreType type, final String name )
    {
        this.packageType = MAVEN_PKG_KEY;
        this.type = type;
        this.name = name;
    }

    public String getPackageType()
    {
        return packageType;
    }

    public StoreType getType()
    {
        return type;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return packageType + ":" + type.name() + ":" + name;
    }

    @Override
    public final int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( packageType == null ) ? 7 : packageType.hashCode() );
        result = prime * result + ( ( name == null ) ? 13 : name.hashCode() );
        result = prime * result + ( ( type == null ) ? 17 : type.hashCode() );
        return result;
    }

    @Override
    public final boolean equals( final Object obj )
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
        final StoreKey other = (StoreKey) obj;
        if ( packageType == null )
        {
            if ( other.packageType != null )
            {
                return false;
            }
        }
        else if ( !packageType.equals( other.packageType ) )
        {
            return false;
        }
        if ( name == null )
        {
            if ( other.name != null )
            {
                return false;
            }
        }
        else if ( !name.equals( other.name ) )
        {
            return false;
        }
        return type == other.type;
    }

    public static StoreKey fromString( final String id )
    {
        Logger logger = LoggerFactory.getLogger( StoreKey.class );
        logger.debug( "Parsing raw string: '{}' to StoreKey", id );

        String[] parts = id.split(":");

        logger.debug( "Got {} parts: {}", parts.length, Arrays.asList( parts ) );

        String packageType = null;
        String name;
        StoreType type = null;

        // FIXME: We need to get to a point where it's safe for this to be an error and not default to maven.
        if ( parts.length < 3 || isBlank(parts[0]) )
        {
            packageType = MAVEN_PKG_KEY;
            type = StoreType.get(parts[0]);
            name = parts[1];
        }
        else if ( parts.length < 2 )
        {
            packageType = MAVEN_PKG_KEY;
            type = StoreType.remote;
            name = id;
        }
        else
        {
            packageType = parts[0];
            type = StoreType.get( parts[1] );
            name = parts[2];
        }

        if ( type == null )
        {
            throw new IllegalArgumentException( "Invalid StoreType: " + parts[1] );
        }

        // logger.info( "parsed store-key with type: '{}' and name: '{}'", type, name );

        return new StoreKey( packageType, type, name );
    }

    @Override
    public int compareTo( final StoreKey o )
    {
        int comp = packageType.compareTo( o.packageType );
        if ( comp == 0 )
        {
            comp = type.compareTo( o.type );
        }

        if ( comp == 0 )
        {
            comp = name.compareTo( o.name );
        }

        return comp;
    }

    private static ConcurrentHashMap<StoreKey, StoreKey> deduplications = new ConcurrentHashMap<>();

    public static StoreKey dedupe( StoreKey key )
    {
        StoreKey result = deduplications.get( key );
        if ( result == null )
        {
            deduplications.put( key, key );
            result = key;
        }

        return result;
    }

}
