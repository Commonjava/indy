import org.commonjava.indy.koji.content.KojiContentManagerDecorator
import org.commonjava.indy.koji.content.KojiRepositoryCreator
import org.commonjava.indy.model.core.HostedRepository
import org.commonjava.indy.model.core.RemoteRepository
import org.commonjava.maven.atlas.ident.ref.ArtifactRef

class Creator implements KojiRepositoryCreator
{
    @Override
    RemoteRepository createRemoteRepository(String name, String url, Integer downloadTimeoutSeconds) {
        RemoteRepository remote = new RemoteRepository( name, url );
        remote.setTimeoutSeconds( downloadTimeoutSeconds );

        remote
    }

    @Override
    HostedRepository createHostedRepository(String name, ArtifactRef artifactRef, String nvr) {
        HostedRepository hosted = new HostedRepository( name );
        hosted.setAllowReleases( true );
        hosted.setAllowSnapshots( false );
        hosted.setMetadata( KojiContentManagerDecorator.CREATION_TRIGGER_GAV, artifactRef.toString() );
        hosted.setMetadata( KojiContentManagerDecorator.NVR, nvr );
        hosted.setDescription(
                String.format( "Koji build: %s (triggered by: %s via: %s)", build.getNvr(), originatingPath,
                        eventMetadata.get( ENTRY_POINT_STORE ) ) );
    }
}