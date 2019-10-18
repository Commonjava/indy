package org.commonjava.indy.implrepo.skim;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.implrepo.data.ImpliedRepoMetadataManager;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>A pre-existing remote repo pr with path p</li>
 *     <li>A pom in remote repo test with path p same as above existing repo path</li>
 *     <li>The pom contains declaration of a repo r</li>
 *     <li>Group pub contains remote repo test</li>
 *     <li>No remote repo point to repo r contained in group pub at first</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Access pom through path p in group pub</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>A pre-existing remote repo pr will be added into group pub</li>
 *     <li>Remote test will have metatada "implied_stores" point to "remote:pr"</li>
 *     <li>This remote pr will have metatada "implied_by_stores" point to "remote:test"</li>
 * </ul>
 */
public class ReuseExistingRepoIntoConfiguredGroupTest
                extends AbstractSkimFunctionalTest
{

    private static final String REPO = "i-repo-one";

    @Test
    public void skimPomForExistingRepoAndAddItInGroup() throws Exception
    {
        RemoteRepository repo = new RemoteRepository( REPO, server.formatUrl( REPO ) );
        repo = client.stores().create( repo, "Pre stored remote repo", RemoteRepository.class );

        final StoreKey remoteRepoKey = repo.getKey();
        final PomRef ref = loadPom( "one-repo", Collections.singletonMap( "one-repo.url", server.formatUrl( REPO ) ) );

        server.expect( "HEAD", server.formatUrl( REPO, "/" ), 200, (String) null );
        server.expect( server.formatUrl( TEST_REPO, ref.path ), 200, ref.pom );

        final StoreKey pubGroupKey = new StoreKey( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, StoreType.group, PUBLIC );
        Group g = client.stores().load( pubGroupKey, Group.class );
        assertThat( "Group membership should not contain implied before getting pom.",
                    g.getConstituents().contains( remoteRepoKey ), equalTo( false ) );

        logger.debug( "Start fetching pom!" );
        final InputStream stream = client.content().get( pubGroupKey, ref.path );
        final String downloaded = IOUtils.toString( stream );
        IOUtils.closeQuietly( stream );

        System.out.println( "Waiting 5s for events to run." );
        Thread.sleep( 5000 );

        g = client.stores().load( pubGroupKey, Group.class );
        assertThat( "Group membership does not contain implied repository",
                    g.getConstituents().contains( remoteRepoKey ), equalTo( true ) );

        repo = client.stores()
                     .load( new StoreKey( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, StoreType.remote, TEST_REPO ),
                            RemoteRepository.class );
        String metadata = repo.getMetadata( ImpliedRepoMetadataManager.IMPLIED_STORES );
        assertThat( "Reference to repositories implied by POMs in this repo is missing from metadata.",
                    metadata.contains( "remote:" + REPO ), equalTo( true ) );

        repo = client.stores().load( remoteRepoKey, RemoteRepository.class );
        metadata = repo.getMetadata( ImpliedRepoMetadataManager.IMPLIED_BY_STORES );
        assertThat( "Backref to repo with pom that implies this repo is missing from metadata.",
                    metadata.contains( "remote:" + TEST_REPO ), equalTo( true ) );
    }

}
