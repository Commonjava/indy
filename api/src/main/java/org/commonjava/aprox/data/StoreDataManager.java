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
package org.commonjava.aprox.data;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;

public interface StoreDataManager
{

    DeployPoint getDeployPoint( final String name )
        throws ProxyDataException;

    Repository getRepository( final String name )
        throws ProxyDataException;

    Group getGroup( final String name )
        throws ProxyDataException;

    ArtifactStore getArtifactStore( StoreKey key )
        throws ProxyDataException;

    List<ArtifactStore> getAllArtifactStores()
        throws ProxyDataException;

    List<? extends ArtifactStore> getAllArtifactStores( StoreType type )
        throws ProxyDataException;

    List<Group> getAllGroups()
        throws ProxyDataException;

    List<Repository> getAllRepositories()
        throws ProxyDataException;

    List<DeployPoint> getAllDeployPoints()
        throws ProxyDataException;

    List<ArtifactStore> getAllConcreteArtifactStores()
        throws ProxyDataException;

    List<ArtifactStore> getOrderedConcreteStoresInGroup( final String groupName )
        throws ProxyDataException;

    List<ArtifactStore> getOrderedStoresInGroup( final String groupName )
        throws ProxyDataException;

    Set<Group> getGroupsContaining( final StoreKey repo )
        throws ProxyDataException;

    void storeDeployPoints( final Collection<DeployPoint> deploys )
        throws ProxyDataException;

    boolean storeDeployPoint( final DeployPoint deploy )
        throws ProxyDataException;

    boolean storeDeployPoint( final DeployPoint deploy, final boolean skipIfExists )
        throws ProxyDataException;

    void storeRepositories( final Collection<Repository> repos )
        throws ProxyDataException;

    boolean storeRepository( final Repository proxy )
        throws ProxyDataException;

    boolean storeRepository( final Repository repository, final boolean skipIfExists )
        throws ProxyDataException;

    void storeGroups( final Collection<Group> groups )
        throws ProxyDataException;

    boolean storeGroup( final Group group )
        throws ProxyDataException;

    boolean storeGroup( final Group group, final boolean skipIfExists )
        throws ProxyDataException;

    boolean storeArtifactStore( ArtifactStore key )
        throws ProxyDataException;

    boolean storeArtifactStore( ArtifactStore key, boolean skipIfExists )
        throws ProxyDataException;

    void deleteDeployPoint( final DeployPoint deploy )
        throws ProxyDataException;

    void deleteDeployPoint( final String name )
        throws ProxyDataException;

    void deleteRepository( final Repository repo )
        throws ProxyDataException;

    void deleteRepository( final String name )
        throws ProxyDataException;

    void deleteGroup( final Group group )
        throws ProxyDataException;

    void deleteGroup( final String name )
        throws ProxyDataException;

    void deleteArtifactStore( StoreKey key )
        throws ProxyDataException;

    void clear()
        throws ProxyDataException;

    void install()
        throws ProxyDataException;

    boolean hasRepository( String name );

    boolean hasGroup( String name );

    boolean hasDeployPoint( String name );

    boolean hasArtifactStore( StoreKey key );

}
