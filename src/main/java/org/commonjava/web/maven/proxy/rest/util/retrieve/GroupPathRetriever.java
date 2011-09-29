package org.commonjava.web.maven.proxy.rest.util.retrieve;

import java.io.File;
import java.util.List;

import org.commonjava.web.maven.proxy.model.ArtifactStore;
import org.commonjava.web.maven.proxy.model.Group;

public interface GroupPathRetriever
{

    boolean canHandle( String path );

    File handle( Group group, List<ArtifactStore> stores, String path );

}
