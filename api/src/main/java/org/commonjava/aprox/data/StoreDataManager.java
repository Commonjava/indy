/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
