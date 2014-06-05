/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.core.change.sl;

import java.io.File;
import java.io.FilenameFilter;

import org.commonjava.maven.atlas.ident.util.ArtifactPathInfo;

public class SnapshotFilter
    implements FilenameFilter
{

    @Override
    public boolean accept( final File dir, final String name )
    {
        return ArtifactPathInfo.parse( name )
                               .isSnapshot();
    }

}
