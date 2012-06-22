package org.commonjava.aprox.indexer;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import javax.inject.Singleton;

import org.commonjava.aprox.core.io.StorageItem;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.rest.RESTWorkflowException;
import org.commonjava.aprox.core.rest.util.retrieve.GroupPathHandler;

@Singleton
public class IndexHandler
    implements GroupPathHandler
{

    @Override
    public boolean canHandle( final String path )
    {
        return new File( path ).getName()
                               .startsWith( "nexus-maven-repository-index" );
    }

    @Override
    public StorageItem retrieve( final Group group, final List<? extends ArtifactStore> stores, final String path )
        throws RESTWorkflowException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DeployPoint store( final Group group, final List<? extends ArtifactStore> stores, final String path,
                              final InputStream stream )
        throws RESTWorkflowException
    {
        return null;
    }

}
