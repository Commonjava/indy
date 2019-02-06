package org.commonjava.indy.pkg.maven.content;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;

import java.util.Map;
import java.util.Set;

@FunctionalInterface
public interface MetadataMergeFunction
{
    Set<ArtifactStore> merge( Set<ArtifactStore> missing, final Map<StoreKey, Metadata> memberMetas,
                              final Set<Metadata> providerMetadata, String toMergePath )
            throws IndyWorkflowException;
}
