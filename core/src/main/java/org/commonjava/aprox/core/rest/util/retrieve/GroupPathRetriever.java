package org.commonjava.aprox.core.rest.util.retrieve;

import java.io.File;
import java.util.List;

import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.Group;

public interface GroupPathRetriever
{

    boolean canHandle( String path );

    File handle( Group group, List<ArtifactStore> stores, String path );

}
