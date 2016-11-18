import org.commonjava.indy.action.IndyLifecycleException
import org.commonjava.indy.action.ShutdownAction

/**
 * Created by ruhan on 11/16/16.
 */
class ShutdownAction01 implements ShutdownAction
{
    @Override
    String getId() {
        return this.getClass().getName();
    }

    @Override
    void stop() throws IndyLifecycleException {
        println ("Shutdown " + getId())
    }

    @Override
    int getShutdownPriority() {
        return 0
    }

}