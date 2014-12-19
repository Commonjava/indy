/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.model.core;

import java.util.HashSet;
import java.util.Set;

//@ApiClass( description = "Enumeration of types of artifact storage on the system. This forms half of the 'primary key' for each store (the other half is the store's name).", value = "Type of artifact storage." )
public enum StoreType
{
    group( Group.class, false, "group", "groups", "g" ), remote( RemoteRepository.class, false, "remote", "remotes", "repository", "repositories",
        "r" ), hosted( HostedRepository.class, true, "hosted", "hosted", "deploy", "deploys", "deploy_point", "h", "d" );

    //    private static final Logger logger = new Logger( StoreType.class );

    private final boolean writable;

    private final String singular;

    private final String plural;

    private final Set<String> aliases;

    private final Class<? extends ArtifactStore> storeClass;

    private StoreType( final Class<? extends ArtifactStore> storeClass, final boolean writable, final String singular, final String plural,
                       final String... aliases )
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
            //            logger.info( "Checking '{}' vs name: '{}' and aliases: {}", type, st.name(), join( st.aliases, ", " ) );
            if ( st.name()
                   .equalsIgnoreCase( type ) || st.aliases.contains( type ) )
            {
                return st;
            }
        }

        return null;
    }

    public static StoreType get( final Class<?> storeCls )
    {
        if ( storeCls == null )
        {
            return null;
        }

        for ( final StoreType st : values() )
        {
            if ( st.storeClass.equals( storeCls ) )
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
