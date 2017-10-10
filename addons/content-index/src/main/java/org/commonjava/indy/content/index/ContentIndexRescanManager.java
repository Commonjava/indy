package org.commonjava.indy.content.index;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.change.event.ArtifactStorePostRescanEvent;
import org.commonjava.indy.change.event.ArtifactStorePreRescanEvent;

/**
 * A ContentIndexRescanManager is used to handle all related things with content index
 * for a store during the rescan processing of this store.
 */
public interface ContentIndexRescanManager
{
    void indexPreRescan( final ArtifactStorePreRescanEvent e )
            throws IndyWorkflowException;

    void indexPostRescan( final ArtifactStorePostRescanEvent e )
            throws IndyWorkflowException;
}
