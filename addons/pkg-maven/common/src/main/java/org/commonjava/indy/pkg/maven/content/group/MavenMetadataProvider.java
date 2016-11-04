package org.commonjava.indy.pkg.maven.content.group;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.model.core.StoreKey;

/**
 * Created by jdcasey on 11/1/16.
 *
 * Interface for add-ons that contribute "virtual" metadata that originates from potential repositories that haven't
 * been created yet, such as the Koji add-on.
 */
public interface MavenMetadataProvider
{
    /**
     * Retrieve the list of versions available for the given Maven GA (groupId:artifactId), which should be included
     * in the target store.
     * @param targetStore Usually a group, this is the target where metadata aggregation is happening.
     * @param path path of the maven-metadata.xml that was requested. Useful to determine what kind of metadata to retrieve.
     * @return a {@link Metadata} instance containing the "virtual" versions that could become available.
     * @throws IndyWorkflowException
     */
    Metadata getMetadata( StoreKey targetStore, String path )
            throws IndyWorkflowException;
}
