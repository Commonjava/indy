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
package org.commonjava.aprox.data;

import java.util.List;
import java.util.Set;

import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;

/**
 * Data manager used to access and manipulate the configurations for {@link ArtifactStore} instances.
 * @author jdcasey
 *
 */
public interface StoreDataManager
{

    /**
     * Return the {@link HostedRepository} instance corresponding to the given name.
     */
    HostedRepository getHostedRepository( final String name )
        throws ProxyDataException;

    /**
     * Return the {@link RemoteRepository} instance corresponding to the given name.
     */
    RemoteRepository getRemoteRepository( final String name )
        throws ProxyDataException;

    /**
     * Return the {@link Group} instance corresponding to the given name.
     */
    Group getGroup( final String name )
        throws ProxyDataException;

    /**
     * Return the {@link ArtifactStore} instance corresponding to the given key, where key is a composite of {@link StoreType} 
     * (hosted, remote, or group) and name.
     */
    ArtifactStore getArtifactStore( StoreKey key )
        throws ProxyDataException;

    /**
     * Return the full list of {@link ArtifactStore} instances available on the system.
     */
    List<ArtifactStore> getAllArtifactStores()
        throws ProxyDataException;

    /**
     * Return the full list of {@link ArtifactStore} instances of a given {@link StoreType} (hosted, remote, or group) available on the system.
     */
    List<? extends ArtifactStore> getAllArtifactStores( StoreType type )
        throws ProxyDataException;

    /**
     * Return the full list of {@link Group} instances available on the system.
     */
    List<Group> getAllGroups()
        throws ProxyDataException;

    /**
     * Return the full list of {@link RemoteRepository} instances available on the system.
     */
    List<RemoteRepository> getAllRemoteRepositories()
        throws ProxyDataException;

    /**
     * Return the full list of {@link HostedRepository} instances available on the system.
     */
    List<HostedRepository> getAllHostedRepositories()
        throws ProxyDataException;

    /**
     * Return the full list of non-{@link Group} instances available on the system.
     */
    List<ArtifactStore> getAllConcreteArtifactStores()
        throws ProxyDataException;

    /**
     * For a {@link Group} with the given name, return (<b>IN ORDER</b>) the list of
     * non-{@link Group} {@link ArtifactStore} instances that are members of the {@link Group}.
     * <br/>
     * <b>NOTE:</b> If any of the group's members are themselves {@link Group}'s, the method
     * recurses and substitutes that group's place in the list with the ordered, concrete stores
     * it contains.
     */
    List<ArtifactStore> getOrderedConcreteStoresInGroup( final String groupName )
        throws ProxyDataException;

    /**
     * For a {@link Group} with the given name, return (<b>IN ORDER</b>) the list of
     * non-{@link Group} {@link ArtifactStore} instances that are members of the {@link Group}.
     * <br/>
     * <b>NOTE:</b> This method does <b>not</b> perform recursion to substitute concrete stores in place
     * of any groups in the list. Groups that are members are returned along with the rest of the membership.
     */
    List<ArtifactStore> getOrderedStoresInGroup( final String groupName )
        throws ProxyDataException;

    /**
     * Return the set of {@link Group}'s that contain the {@link ArtifactStore} corresponding to the given {@link StoreKey} in their membership.
     */
    Set<Group> getGroupsContaining( final StoreKey repo )
        throws ProxyDataException;

    /**
     * Store a modified or new {@link HostedRepository} instance. This is equivalent to 
     * {@link StoreDataManager#storeHostedRepository(HostedRepository, boolean)} with skip flag <code>false</code>
     */
    boolean storeHostedRepository( final HostedRepository deploy, final ChangeSummary summary )
        throws ProxyDataException;

    /**
     * Store a modified or new {@link HostedRepository} instance. If the store already exists, and <code>skipIfExists</code> is true, abort the
     * operation.
     */
    boolean storeHostedRepository( final HostedRepository deploy, final ChangeSummary summary,
                                   final boolean skipIfExists )
        throws ProxyDataException;

    /**
     * Store a modified or new {@link RemoteRepository} instance. This is equivalent to 
     * {@link StoreDataManager#storeRemoteRepository(RemoteRepository, boolean)} with skip flag <code>false</code>
     */
    boolean storeRemoteRepository( final RemoteRepository proxy, final ChangeSummary summary )
        throws ProxyDataException;

    /**
     * Store a modified or new {@link RemoteRepository} instance. If the store already exists, and <code>skipIfExists</code> is true, abort the
     * operation.
     */
    boolean storeRemoteRepository( final RemoteRepository repository, final ChangeSummary summary,
                                   final boolean skipIfExists )
        throws ProxyDataException;

    /**
     * Store a modified or new {@link Group} instance. This is equivalent to 
     * {@link StoreDataManager#storeGroup(Group, boolean)} with skip flag <code>false</code>
     */
    boolean storeGroup( final Group group, final ChangeSummary summary )
        throws ProxyDataException;

    /**
     * Store a modified or new {@link Group} instance. If the store already exists, and <code>skipIfExists</code> is true, abort the
     * operation.
     */
    boolean storeGroup( final Group group, final ChangeSummary summary, final boolean skipIfExists )
        throws ProxyDataException;

    /**
     * Store a modified or new {@link ArtifactStore} instance. This is equivalent to 
     * {@link StoreDataManager#storeArtifactStore(ArtifactStore, boolean)} with skip flag <code>false</code>
     */
    boolean storeArtifactStore( ArtifactStore key, final ChangeSummary summary )
        throws ProxyDataException;

    /**
     * Store a modified or new {@link ArtifactStore} instance. If the store already exists, and <code>skipIfExists</code> is true, abort the
     * operation.
     */
    boolean storeArtifactStore( ArtifactStore key, final ChangeSummary summary, boolean skipIfExists )
        throws ProxyDataException;

    /**
     * Delete the given {@link HostedRepository}.
     */
    void deleteHostedRepository( final HostedRepository deploy, final ChangeSummary summary )
        throws ProxyDataException;

    /**
     * Delete the {@link HostedRepository} corresponding to the given name.
     */
    void deleteHostedRepository( final String name, final ChangeSummary summary )
        throws ProxyDataException;

    /**
     * Delete the given {@link RemoteRepository}.
     */
    void deleteRemoteRepository( final RemoteRepository repo, final ChangeSummary summary )
        throws ProxyDataException;

    /**
     * Delete the {@link RemoteRepository} corresponding to the given name.
     */
    void deleteRemoteRepository( final String name, final ChangeSummary summary )
        throws ProxyDataException;

    /**
     * Delete the given {@link Group}.
     */
    void deleteGroup( final Group group, final ChangeSummary summary )
        throws ProxyDataException;

    /**
     * Delete the {@link Group} corresponding to the given name.
     */
    void deleteGroup( final String name, final ChangeSummary summary )
        throws ProxyDataException;

    /**
     * Delete the {@link ArtifactStore} corresponding to the given {@link StoreKey}. If the store doesn't exist, simply return (don't fail).
     */
    void deleteArtifactStore( StoreKey key, final ChangeSummary summary )
        throws ProxyDataException;

    /**
     * Delete all {@link ArtifactStore} instances currently in the system.
     */
    void clear( final ChangeSummary summary )
        throws ProxyDataException;

    /**
     * If no {@link ArtifactStore}'s exist in the system, install a couple of defaults:
     * <ul>
     * <li>Remote <code>central</code> pointing to the Maven central repository at http://repo.maven.apache.org/maven2/</li>
     * <li>Hosted <code>local-deployments</code> that can host both releases and snapshots</li>
     * <li>Group <code>public</code> containing <code>central</code> and <code>local-deployments</code> as members</li>
     * </ul>
     */
    void install()
        throws ProxyDataException;

    /**
     * Mechanism for clearing all cached {@link ArtifactStore} instances and reloading them from some backing store.
     */
    void reload()
        throws ProxyDataException;

    /**
     * Return true if the system contains a {@link RemoteRepository} with the given name; false otherwise.
     */
    boolean hasRemoteRepository( String name );

    /**
     * Return true if the system contains a {@link Group} with the given name; false otherwise.
     */
    boolean hasGroup( String name );

    /**
     * Return true if the system contains a {@link HostedRepository} with the given name; false otherwise.
     */
    boolean hasHostedRepository( String name );

    /**
     * Return true if the system contains a {@link ArtifactStore} with the given key (combination of {@link StoreType} and name); false otherwise.
     */
    boolean hasArtifactStore( StoreKey key );

}
