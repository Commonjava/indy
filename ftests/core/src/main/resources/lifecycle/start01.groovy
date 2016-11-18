import org.commonjava.indy.action.IndyLifecycleException
import org.commonjava.indy.action.StartupAction
import org.commonjava.indy.content.ContentManager
import org.commonjava.indy.model.core.HostedRepository
import org.commonjava.maven.galley.model.TransferOperation

import javax.inject.Inject

/**
 * Created by ruhan on 11/16/16.
 */
class StartupAction01 implements StartupAction {

    @Override
    String getId() {
        return this.getClass().getName();
    }

    @Inject
    ContentManager contentManager;

    @Override
    void start() throws IndyLifecycleException {
        println ("Start " + getId())

        contentManager.store(new HostedRepository( "test" ), "org/bar/bar.pom",
                new ByteArrayInputStream( ( "This is bar" ).getBytes() ), TransferOperation.UPLOAD)
    }

    @Override
    int getStartupPriority() {
        return 0
    }
}