import org.commonjava.indy.action.IndyLifecycleException
import org.commonjava.indy.action.MigrationAction
import org.commonjava.indy.content.ContentManager
import org.commonjava.indy.model.core.HostedRepository
import org.commonjava.maven.galley.model.TransferOperation

import javax.inject.Inject

/**
 * Created by ruhan on 11/16/16.
 */
class MigrationAction01 implements MigrationAction {

    @Override
    String getId() {
        return this.getClass().getName();
    }

    @Inject
    ContentManager contentManager;

    @Override
    boolean migrate() throws IndyLifecycleException {
        println ("Migrate " + getId())

        contentManager.store(new HostedRepository( "test" ), "org/foo/foo.pom",
                new ByteArrayInputStream(( "This is foo" ).getBytes()), TransferOperation.UPLOAD)

        return false
    }

    @Override
    int getMigrationPriority() {
        return 0
    }
}