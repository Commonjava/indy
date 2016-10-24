package org.commonjava.indy.koji;

import org.commonjava.indy.koji.content.KojiRepositoryCreator
import org.commonjava.indy.model.core.RemoteRepository

class RepoCreator implements KojiRepositoryCreator
{
    @Override
    RemoteRepository createRemoteRepository(String name, String url, Integer downloadTimeoutSeconds) {
        RemoteRepository remote = new RemoteRepository( name, url );
        remote.setTimeoutSeconds( downloadTimeoutSeconds );

        remote
    }

}