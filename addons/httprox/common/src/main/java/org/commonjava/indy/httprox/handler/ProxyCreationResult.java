package org.commonjava.indy.httprox.handler;

import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;

import java.util.Arrays;
import java.util.List;

/**
 * Created by ruhan on 4/12/18.
 */
public class ProxyCreationResult
{
    private Group group;

    private HostedRepository hosted;

    private RemoteRepository remote;

    // TODO: 4/16/18, is this really useful?
    public List<StoreKey> getStores()
    {
        return Arrays.asList( hosted.getKey(), remote.getKey() ); // contains (hosted, remote) in that order
    }

    public Group getGroup()
    {
        return group;
    }

    public void setGroup( Group group )
    {
        this.group = group;
    }

    public HostedRepository getHosted()
    {
        return hosted;
    }

    public void setHosted( HostedRepository hosted )
    {
        this.hosted = hosted;
    }

    public RemoteRepository getRemote()
    {
        return remote;
    }

    public void setRemote( RemoteRepository remote )
    {
        this.remote = remote;
    }
}
