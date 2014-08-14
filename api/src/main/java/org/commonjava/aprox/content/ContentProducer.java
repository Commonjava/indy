package org.commonjava.aprox.content;

import java.util.List;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.maven.galley.model.Transfer;

public interface ContentProducer
{

    Transfer produceFileContent( ArtifactStore store, String path )
        throws AproxWorkflowException;

    List<StoreResource> produceDirectoryContent( ArtifactStore store, String path )
        throws AproxWorkflowException;

}
