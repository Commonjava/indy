package org.commonjava.aprox.core.rest.util;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Repository;

public interface FileManager
{

    File downloadFirst( final List<ArtifactStore> stores, final String path );

    Set<File> downloadAll( final List<ArtifactStore> stores, final String path );

    File download( final ArtifactStore store, final String path );

    boolean download( final Repository repository, final String path, final File target, final boolean suppressFailures );

    void upload( final DeployPoint deploy, final String path, final InputStream stream );

    DeployPoint upload( final List<DeployPoint> deployPoints, final String path, final InputStream stream );

    File formatStorageReference( final ArtifactStore store, final String path );

}