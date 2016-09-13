package org.commonjava.indy.koji;

import org.commonjava.indy.koji.content.KojiContentManagerDecorator
import org.commonjava.indy.koji.content.KojiRepositoryCreator
import org.commonjava.indy.model.core.HostedRepository
import org.commonjava.indy.model.core.RemoteRepository
import org.commonjava.maven.atlas.ident.ref.ArtifactRef
import org.commonjava.maven.galley.event.EventMetadata;

class RepoCreator implements KojiRepositoryCreator
{
    @Override
    RemoteRepository createRemoteRepository(String name, String url, Integer downloadTimeoutSeconds) {
        RemoteRepository remote = new RemoteRepository( name, url );
        remote.setTimeoutSeconds( downloadTimeoutSeconds );

        remote
    }

    @Override
    HostedRepository createHostedRepository(String name, ArtifactRef artifactRef, String nvr, EventMetadata eventMetadata) {
        HostedRepository hosted = new HostedRepository( name );
        hosted.setAllowReleases( true );
        hosted.setAllowSnapshots( false );
        hosted.setMetadata( KojiContentManagerDecorator.CREATION_TRIGGER_GAV, artifactRef.toString() );
        hosted.setMetadata( KojiContentManagerDecorator.NVR, nvr );
        hosted.setDescription(
                String.format( "Koji build: %s (triggered by: %s via: %s)", nvr, artifactRef.toString(),
                        eventMetadata.get( ENTRY_POINT_STORE ) ) );
    }
}