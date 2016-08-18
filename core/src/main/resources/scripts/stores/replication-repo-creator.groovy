package org.commonjava.indy.replication;

import org.commonjava.indy.core.ctl.ReplicationRepositoryCreator
import org.commonjava.indy.model.core.RemoteRepository
import org.commonjava.indy.model.core.dto.EndpointView

class RepoCreator implements ReplicationRepositoryCreator
{

    @Override
    RemoteRepository createRemoteRepository(String name, EndpointView view) {
        new RemoteRepository( name, view.getResourceUri() )
    }
}