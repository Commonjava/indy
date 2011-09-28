package org.commonjava.web.maven.proxy.rest.util.retrieve;

import java.io.File;
import java.util.List;

import org.commonjava.web.maven.proxy.model.Group;
import org.commonjava.web.maven.proxy.model.Repository;

public interface GroupPathRetriever
{

    boolean canHandle( String path );

    File handle( Group group, List<Repository> repos, String path );

}
