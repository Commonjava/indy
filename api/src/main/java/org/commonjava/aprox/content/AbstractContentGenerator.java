package org.commonjava.aprox.content;

import java.util.List;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.maven.galley.model.Transfer;

public abstract class AbstractContentGenerator
    implements ContentGenerator
{

    protected AbstractContentGenerator()
    {
    }

    @Override
    public void handleContentStorage( final ArtifactStore store, final String path, final Transfer result )
        throws AproxWorkflowException
    {
    }

    @Override
    public void handleContentDeletion( final ArtifactStore store, final String path )
        throws AproxWorkflowException
    {
    }

    @Override
    public Transfer generateFileContent( final ArtifactStore store, final String path )
        throws AproxWorkflowException
    {
        return null;
    }

    @Override
    public List<StoreResource> generateDirectoryContent( final ArtifactStore store, final String path,
                                                         final List<StoreResource> existing )
        throws AproxWorkflowException
    {
        return null;
    }

    @Override
    public Transfer generateGroupFileContent( final Group group, final List<ArtifactStore> members, final String path )
        throws AproxWorkflowException
    {
        return null;
    }

    @Override
    public List<StoreResource> generateGroupDirectoryContent( final Group group, final List<ArtifactStore> members,
                                                              final String path )
        throws AproxWorkflowException
    {
        return null;
    }

}
