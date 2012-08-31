package org.commonjava.aprox.core.change.sl;

import java.io.File;
import java.io.FilenameFilter;

import org.commonjava.aprox.rest.util.ArtifactPathInfo;

public class SnapshotFilter
    implements FilenameFilter
{

    @Override
    public boolean accept( final File dir, final String name )
    {
        return ArtifactPathInfo.isSnapshot( name );
    }

}
