package org.commonjava.indy.spi.pkg;

import org.commonjava.indy.model.core.ArtifactStore;

/**
 */
public interface StoreAdvisor
{
    /**
     * Allow a packaging type to indicate that the proposed storage location is inappropriate for the supplied path
     *
     * @param path
     * @param store
     * @return
     */
    boolean vetoStorage( String path, ArtifactStore store );
}
