package org.commonjava.aprox.depbase;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.aprox.core.data.StoreDataManager;
import org.commonjava.aprox.core.rest.util.FileManager;
import org.commonjava.depbase.data.DepbaseDataException;
import org.commonjava.depbase.discovery.ProjectGraphDiscoverer;
import org.commonjava.depbase.model.ProjectId;

@Singleton
public class AproxProjectGraphDiscoverer
    implements ProjectGraphDiscoverer
{

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private FileManager fileManager;

    @Override
    public void discoverProjectGraph( final ProjectId projectId )
        throws DepbaseDataException
    {
        // TODO: Configuration giving group(s) to do discovery from...
        // TODO: Create ArtifactStoreModelResolver for projectId, then recurse starting at projectId.
    }

}
