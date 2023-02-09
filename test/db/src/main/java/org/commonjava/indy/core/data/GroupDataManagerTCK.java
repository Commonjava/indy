/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.core.data;

import static java.util.Arrays.asList;
import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.maven.galley.event.EventMetadata;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;

public abstract class GroupDataManagerTCK
    extends AbstractProxyDataManagerTCK
{

    private StoreDataManager manager;

    private final ChangeSummary summary = new ChangeSummary( "test-user", "test" );

    @Before
    public void setup()
        throws Exception
    {
        doSetup();
        seedRepositoriesForGroupTests();
    }

    protected void doSetup()
        throws Exception
    {
    }

    protected void seedRepositoriesForGroupTests()
        throws Exception
    {
        manager = getFixtureProvider().getDataManager();

        manager.storeArtifactStore( new RemoteRepository( MAVEN_PKG_KEY, "central", "http://repo1.maven.apache.org/maven2/" ),
                                       summary, false, false, new EventMetadata() );

        manager.storeArtifactStore( new RemoteRepository( MAVEN_PKG_KEY, "repo2", "http://repo1.maven.org/maven2/" ), summary, false, false, new EventMetadata() );
    }

    @Test
    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public void createAndRetrieveEmptyGroup()
        throws Exception
    {
        final Group grp = new Group( "test" );

        store( grp );

        final Group result = manager.query().getGroup( MAVEN_PKG_KEY, grp.getName() );

        assertThat( result, notNullValue() );
        assertThat( result.getName(), equalTo( grp.getName() ) );
        assertThat( result.getConstituents(), anyOf( nullValue(), new BaseMatcher<List>()
        {
            @Override
            public boolean matches( final Object item )
            {
                return ( item instanceof List ) && ( (List) item ).isEmpty();
            }

            @Override
            public void describeTo( final Description description )
            {
                description.appendText( "empty list" );
            }
        } ) );
    }

    private void store( final ArtifactStore... stores )
            throws Exception
    {
        for ( final ArtifactStore store : stores )
        {
            manager.storeArtifactStore( store, summary, false, false, new EventMetadata() );
        }
    }

    private void removeStore(final ArtifactStore... stores) throws Exception{

        for ( final ArtifactStore store: stores )
        {
            manager.deleteArtifactStore( store.getKey(), summary,  new EventMetadata() );
        }
    }

    @Test
    public void createAndDeleteGroup()
        throws Exception
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final Group grp = new Group( "test" );
        store( grp );

        manager.deleteArtifactStore( grp.getKey(), summary, new EventMetadata() );

        final Group result = manager.query().getGroup( MAVEN_PKG_KEY, grp.getName() );

        assertThat( result, nullValue() );
    }

    @Test
    public void createAndRetrieveGroupWithTwoConstituents()
        throws Exception
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final Group grp =
            new Group( "test", new StoreKey( remote, "central" ), new StoreKey( remote, "repo2" ) );

        store( grp );

        final Group result = manager.query().getGroup( MAVEN_PKG_KEY, grp.getName() );

        assertThat( result, notNullValue() );
        assertThat( result.getName(), equalTo( grp.getName() ) );

        final List<StoreKey> repos = result.getConstituents();
        assertThat( repos, notNullValue() );
        assertThat( repos.size(), equalTo( 2 ) );

        assertThat( repos.get( 0 ), equalTo( new StoreKey( remote, "central" ) ) );
        assertThat( repos.get( 1 ), equalTo( new StoreKey( remote, "repo2" ) ) );
    }

    @Test
    public void createGroupAndRetrieveReposForThatGroupInOrder()
        throws Exception
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        manager.storeArtifactStore( new HostedRepository("pnc-builds"), summary, false, false, new EventMetadata() );

        final Group subGrp = new Group( "subGroup", new StoreKey( hosted, "pnc-builds" ) );

        final Group grp =
            new Group( "test",  subGrp.getKey(), new StoreKey( remote, "repo2" ), new StoreKey( remote, "central" ) );

        store( grp, subGrp );

        final List<ArtifactStore> repos = manager.query().getOrderedConcreteStoresInGroup( MAVEN_PKG_KEY, grp.getName() );

        assertThat( repos, notNullValue() );
        assertThat( repos.size(), equalTo( 3 ) );

        assertThat( repos.get( 0 )
                         .getName(), equalTo( "pnc-builds" ) );
        assertThat( repos.get( 1 )
                         .getName(), equalTo( "repo2" ) );
        assertThat( repos.get( 2 )
                .getName(), equalTo( "central" ) );
    }

    @Test
    public void createGroupAndRetrieveRepositoryConstituents()
        throws Exception
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final Group grp =
            new Group( "test", new StoreKey( remote, "central" ), new StoreKey( remote, "repo2" ) );

        store( grp );

        final List<ArtifactStore> result = manager.query().getOrderedConcreteStoresInGroup( MAVEN_PKG_KEY, grp.getName() );

        assertThat( result, notNullValue() );
        assertThat( result.size(), equalTo( 2 ) );

        ArtifactStore repo = result.get( 0 );
        assertThat( repo, notNullValue() );
        assertThat( repo.getName(), equalTo( "central" ) );

        repo = result.get( 1 );
        assertThat( repo, notNullValue() );
        assertThat( repo.getName(), equalTo( "repo2" ) );
    }

    @Test
    public void createSameGroupTwiceAndRetrieveOne()
        throws Exception
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final Group grp = new Group( "test" );

        store( grp, grp );

        final List<Group> result = manager.query().getAllGroups( MAVEN_PKG_KEY );

        assertThat( result, notNullValue() );
        assertThat( result.size(), equalTo( 1 ) );
    }

    @Test
    public void createTwoGroupsAndRetrieveBoth()
        throws Exception
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final Group grp = new Group( "test" );
        final Group grp2 = new Group( "test2" );

        store( grp, grp2 );

        final List<Group> result = manager.query().getAllGroups( MAVEN_PKG_KEY );

        assertThat( result, notNullValue() );
        assertThat( result.size(), equalTo( 2 ) );

        Collections.sort( result, new Comparator<Group>()
        {
            @Override
            public int compare( final Group g1, final Group g2 )
            {
                return g1.getName()
                         .compareTo( g2.getName() );
            }
        } );

        Group g = result.get( 0 );
        assertThat( g.getName(), equalTo( grp.getName() ) );

        g = result.get( 1 );
        assertThat( g.getName(), equalTo( grp2.getName() ) );
    }

    @Test
    public void createTwoGroupsAndAffectedByForOneLevel() throws Exception{
        final StoreDataManager manager = getFixtureProvider().getDataManager();
        final StoreKey central = new StoreKey( remote, "central" );
        final StoreKey repo2 = new StoreKey( remote, "repo2" );
        Group g1 = new Group( "g1", central );
        Group g2 = new Group( "g2", repo2 );

        store( g1, g2 );

        List<StoreKey> keys = asList( central, repo2 );

        Set<StoreKey> gKeys =
                manager.query().getGroupsAffectedBy( keys ).stream().map( Group::getKey ).collect( Collectors.toSet() );

        assertThat( gKeys.contains( g1.getKey() ), equalTo( Boolean.TRUE ) );
        assertThat( gKeys.contains( g2.getKey() ), equalTo( Boolean.TRUE ) );
    }

    @Test
    public void createTwoGroupsAndAffectedByForTwoLevel() throws Exception{
        final StoreDataManager manager = getFixtureProvider().getDataManager();
        final StoreKey central = new StoreKey( remote, "central" );
        Group g1 = new Group( "g1", central );
        Group g2 = new Group( "g2", g1.getKey() );

        store(g1, g2);

        List<StoreKey> keys = Collections.singletonList( central );

        Set<StoreKey> gKeys =
                manager.query().getGroupsAffectedBy( keys ).stream().map( Group::getKey ).collect( Collectors.toSet() );

        assertThat( gKeys.contains( g1.getKey() ), equalTo( Boolean.TRUE ) );
        //FIXME: should this group:g2 be in result?
        assertThat( gKeys.contains( g2.getKey() ), equalTo( Boolean.TRUE )  );
    }

    @Test
    public void complexGroupsAffectedBy()
            throws Exception
    {
        // The original tree is like below(r:c->remote:central, h:1->hosted:1, h:2->hosted:2)
        //                          A
        //              ---------------------------
        //              B                         C
        //          ----------                ---------
        //          D        E                F      h:1
        //       ------    ------        ----------
        //     r:c   h:1  h:1  h:2      r:c      h:2
        final StoreDataManager manager = getFixtureProvider().getDataManager();
        final StoreKey central = new StoreKey( PKG_TYPE_MAVEN, remote, "central" );
        final HostedRepository hosted1 = new HostedRepository( PKG_TYPE_MAVEN, "hosted1" );
        final HostedRepository hosted2 = new HostedRepository( PKG_TYPE_MAVEN, "hosted2" );
        final Group groupA = new Group( PKG_TYPE_MAVEN, "groupA" );
        final Group groupB = new Group( PKG_TYPE_MAVEN, "groupB" );
        final Group groupC = new Group( PKG_TYPE_MAVEN, "groupC" );
        groupA.setConstituents( asList( groupB.getKey(), groupC.getKey() ) );
        final Group groupD = new Group( PKG_TYPE_MAVEN, "groupD" );
        groupD.setConstituents( asList( central, hosted1.getKey() ) );
        final Group groupE = new Group( PKG_TYPE_MAVEN, "groupE" );
        groupE.setConstituents( asList( hosted1.getKey(), hosted2.getKey() ) );
        groupB.setConstituents( asList( groupD.getKey(), groupE.getKey() ) );
        final Group groupF = new Group( PKG_TYPE_MAVEN, "groupF" );
        groupF.setConstituents( asList( central, hosted2.getKey() ) );
        groupC.setConstituents( asList( hosted1.getKey(), groupF.getKey() ) );

        store( hosted1, hosted2, groupA, groupB, groupC, groupD, groupE, groupF );
        assertAffectedBy( groupB.getKey(), groupA );
        assertAffectedBy( groupC.getKey(), groupA );
        assertAffectedBy( groupD.getKey(), groupB, groupA );
        assertAffectedBy( groupE.getKey(), groupB, groupA );
        assertAffectedBy( groupF.getKey(), groupC, groupA );
        assertAffectedBy( central, groupF, groupD, groupC, groupB, groupA );
        assertAffectedBy( hosted1.getKey(), groupE, groupD, groupC, groupB, groupA );
        assertAffectedBy( hosted2.getKey(), groupE, groupF, groupC, groupB, groupA );
        assertAffectedBy( groupD.getKey(), groupB, groupA );

        removeStore( groupD );
        assertAffectedBy( groupB.getKey(), groupA );
        assertAffectedBy( groupC.getKey(), groupA );
        assertAffectedBy( groupE.getKey(), groupB, groupA );
        assertAffectedBy( groupF.getKey(), groupC, groupA );
        assertAffectedBy( central, groupF, groupC, groupA );
        assertAffectedBy( hosted1.getKey(), groupE, groupC, groupB, groupA );
        assertAffectedBy( hosted2.getKey(), groupE, groupF, groupC, groupB, groupA );
        //FIXME: From case level this should be correct, but unit-testing can not trigger post deletion for parent group constituents
        //       removal, so the parents' constituents are still containing the removed one, which will cause this failed
        //assertAffectedBy( groupD.getKey(), new StoreKey[]{} );

        // Need to use a copy here to avoid same object updating which impacts memory one
        Group groupECopy = groupE.copyOf();
        groupECopy.addConstituent( central );
        store( groupECopy );
        assertAffectedBy( groupB.getKey(), groupA );
        assertAffectedBy( groupC.getKey(), groupA );
        assertAffectedBy( groupE.getKey(), groupB, groupA );
        assertAffectedBy( groupF.getKey(), groupC, groupA );
        assertAffectedBy( central, groupE, groupF, groupC, groupB, groupA );
        assertAffectedBy( hosted1.getKey(), groupE, groupC, groupB, groupA );
        assertAffectedBy( hosted2.getKey(), groupE, groupF, groupC, groupB, groupA );

        // Copy & update again
        groupECopy = groupECopy.copyOf();
        groupECopy.removeConstituent( hosted1 );
        groupECopy.removeConstituent( hosted2 );
        store( groupECopy );
        assertAffectedBy( groupB.getKey(), groupA );
        assertAffectedBy( groupC.getKey(), groupA );
        assertAffectedBy( groupE.getKey(), groupB, groupA );
        assertAffectedBy( groupF.getKey(), groupC, groupA );
        assertAffectedBy( central, groupE, groupF, groupC, groupB, groupA );
        assertAffectedBy( hosted1.getKey(), groupC, groupA );
        assertAffectedBy( hosted2.getKey(), groupF, groupC, groupA );
    }

    private void assertAffectedBy( StoreKey affectedByKey, ArtifactStore... expectedStores )
            throws Exception
    {
        final Set<StoreKey> gKeys = manager.query()
                                           .getGroupsAffectedBy( Collections.singletonList( affectedByKey ) )
                                           .stream()
                                           .map( Group::getKey )
                                           .collect( Collectors.toSet() );

        assertThat( gKeys, equalTo(
                Arrays.stream( expectedStores ).map( ArtifactStore::getKey ).collect( Collectors.toSet() ) ) );
    }
}
