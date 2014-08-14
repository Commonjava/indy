package org.commonjava.aprox.core.content;

import java.util.List;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.content.ContentProducer;
import org.commonjava.aprox.content.StoreResource;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.maven.galley.model.Transfer;

public class MavenMetadataProducer
    implements ContentProducer
{

    @Override
    public Transfer produceFileContent( final ArtifactStore store, final String path )
        throws AproxWorkflowException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<StoreResource> produceDirectoryContent( final ArtifactStore store, final String path )
        throws AproxWorkflowException
    {
        // TODO Auto-generated method stub
        return null;
    }

}
