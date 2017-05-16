import org.commonjava.indy.model.core.StoreKey
import org.commonjava.indy.model.core.StoreType
import org.junit.Assert
import org.commonjava.indy.action.BootupAction
import org.commonjava.indy.action.IndyLifecycleException
import org.commonjava.indy.data.StoreDataManager

import javax.inject.Inject

/**
 * Created by ruhan on 11/16/16.
 */
class BootAction02 implements BootupAction {

    @Override
    String getId() {
        return this.getClass().getName();
    }

    @Inject
    StoreDataManager storeDataManager;

    @Override
    void init() throws IndyLifecycleException {
        println ("Boot " + getId())

        // Hosted repo is created in boot01.groovy
        Assert.assertNotNull(storeDataManager.getArtifactStore(new StoreKey(StoreType.hosted, "test")))
    }

    @Override
    int getBootPriority() {
        return -1
    }
}