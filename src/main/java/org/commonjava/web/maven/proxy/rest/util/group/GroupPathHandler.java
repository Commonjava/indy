package org.commonjava.web.maven.proxy.rest.util.group;

import java.io.File;
import java.util.List;

import org.commonjava.web.maven.proxy.conf.ProxyConfiguration;
import org.commonjava.web.maven.proxy.model.Group;
import org.commonjava.web.maven.proxy.model.Repository;
import org.commonjava.web.maven.proxy.rest.util.Downloader;

public interface GroupPathHandler
{

    boolean canHandle( String path );

    File handle( Group group, List<Repository> repos, String path, Downloader downloader,
                 ProxyConfiguration config );

}
