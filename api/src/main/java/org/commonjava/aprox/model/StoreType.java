/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.model;

import java.util.HashSet;
import java.util.Set;

//@ApiClass( description = "Enumeration of types of artifact storage on the system. This forms half of the 'primary key' for each store (the other half is the store's name).", value = "Type of artifact storage." )
public enum StoreType
{
    group( Group.class, false, "group", "groups" ),
    repository( Repository.class, false, "repository", "repositories" ),
    deploy_point( DeployPoint.class, true, "deploy", "deploys" );

    //    private static final Logger logger = new Logger( StoreType.class );

    private final boolean writable;

    private final String singular;

    private final String plural;

    private final Set<String> aliases;

    private final Class<? extends ArtifactStore> storeClass;

    private StoreType( final Class<? extends ArtifactStore> storeClass, final boolean writable, final String singular,
                       final String plural, final String... aliases )
    {
        this.storeClass = storeClass;
        this.writable = writable;
        this.singular = singular;
        this.plural = plural;

        final Set<String> a = new HashSet<String>();
        a.add( singular );
        a.add( plural );
        for ( final String alias : aliases )
        {
            a.add( alias.toLowerCase() );
        }

        this.aliases = a;
    }

    public String pluralEndpointName()
    {
        return plural;
    }

    public String singularEndpointName()
    {
        return singular;
    }

    public boolean isWritable()
    {
        return writable;
    }

    public static StoreType get( final String typeStr )
    {
        if ( typeStr == null )
        {
            return null;
        }

        final String type = typeStr.trim()
                                   .toLowerCase();
        if ( type.length() < 1 )
        {
            return null;
        }

        for ( final StoreType st : values() )
        {
            //            logger.info( "Checking '%s' vs name: '%s' and aliases: %s", type, st.name(), join( st.aliases, ", " ) );
            if ( st.name()
                   .equalsIgnoreCase( type ) || st.aliases.contains( type ) )
            {
                return st;
            }
        }

        return null;
    }

    public Class<? extends ArtifactStore> getStoreClass()
    {
        return storeClass;
    }
}
