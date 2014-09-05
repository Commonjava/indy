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
package org.commonjava.aprox.subsys.flatfile.conf;

import java.io.File;
import java.nio.file.Paths;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class FlatFileManager
{

    @Inject
    private FlatFileConfiguration config;

    @Inject
    private FlatFileEventManager fileEventManager;

    protected FlatFileManager()
    {
    }

    public FlatFileManager( final File rootDir, final FlatFileEventManager fileEventManager )
    {
        this.fileEventManager = fileEventManager;
        this.config = new FlatFileConfiguration( rootDir );
    }

    public FlatFileManager( final FlatFileConfiguration config, final FlatFileEventManager fileEventManager )
    {
        this.config = config;
        this.fileEventManager = fileEventManager;
    }

    public FlatFile getDataFile( final String... pathParts )
    {
        final File base = config.getDataBasedir();
        final File f = Paths.get( base.getAbsolutePath(), pathParts )
                            .toFile();

        return new FlatFile( f, fileEventManager );
    }

    public File getDetachedDataBasedir()
    {
        return config.getDataBasedir();
    }

    public File getDetachedWorkBasedir()
    {
        return config.getWorkBasedir();
    }

}
