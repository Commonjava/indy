import org.commonjava.indy.action.BootupAction
import org.commonjava.indy.action.IndyLifecycleException
import org.commonjava.indy.audit.ChangeSummary
import org.commonjava.indy.data.StoreDataManager
import org.commonjava.indy.model.core.HostedRepository
import org.commonjava.maven.galley.event.EventMetadata

import javax.inject.Inject

/**
 * Created by ruhan on 11/16/16.
 */
class BootAction01 implements BootupAction {

    @Override
    String getId() {
        return this.getClass().getName();
    }

    @Inject
    StoreDataManager storeDataManager;

    @Override
    void init() throws IndyLifecycleException {
        println ("Boot " + getId())

        // Create hosted repo
        storeDataManager.storeArtifactStore(new HostedRepository( "test" ),
                new ChangeSummary("user", "Create test repo."), false, true, new EventMetadata())
    }

    @Override
    int getBootPriority() {
        return 0
    }
}