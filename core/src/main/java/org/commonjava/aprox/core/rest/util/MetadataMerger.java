package org.commonjava.aprox.core.rest.util;

import java.util.Set;

import org.commonjava.aprox.model.Group;
import org.commonjava.maven.galley.model.Transfer;

public interface MetadataMerger
{

    byte[] merge( final Set<Transfer> sources, final Group group, final String path );

}
