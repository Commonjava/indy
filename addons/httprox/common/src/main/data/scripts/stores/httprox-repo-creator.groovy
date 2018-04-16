package org.commonjava.indy.httprox

import org.commonjava.indy.httprox.handler.ProxyCreationResult
import org.commonjava.indy.httprox.handler.AbstractProxyRepositoryCreator
import org.commonjava.indy.model.core.HostedRepository
import org.commonjava.indy.model.core.RemoteRepository
import org.commonjava.indy.model.core.StoreType
import org.commonjava.indy.subsys.http.util.UserPass
import org.commonjava.indy.util.UrlInfo
import org.slf4j.Logger

class RepoCreator extends AbstractProxyRepositoryCreator {

    @Override
    ProxyCreationResult create(String trackingID, String name, String baseUrl, UrlInfo urlInfo, UserPass userPass, Logger logger) {
        ProxyCreationResult ret = new ProxyCreationResult()
        if (trackingID == null) {
            RemoteRepository remote = createRemote(name, baseUrl, urlInfo, userPass, logger)
            ret.setRemote(remote)
        } else {
            String host = urlInfo.getHost();
            int port = urlInfo.getPort();

            String remoteName = formatId(host, port, 0, trackingID, StoreType.remote)
            RemoteRepository remote = createRemote(trackingID, remoteName, baseUrl, urlInfo, userPass, logger);
            ret.setRemote(remote)

            String hostedName = formatId(host, port, 0, trackingID, StoreType.hosted)
            HostedRepository hosted = createHosted(trackingID, hostedName, urlInfo, logger);
            ret.setHosted(hosted)

            String groupName = formatId(host, port, 0, trackingID, StoreType.group)
            ret.setGroup(createGroup(trackingID, groupName, urlInfo, logger, remote.getKey(), hosted.getKey()))
        }
        ret
    }

}