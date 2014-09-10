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
package org.commonjava.aprox.subsys.datafile;

import java.io.File;
import java.nio.file.Paths;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.subsys.datafile.change.DataFileEventManager;
import org.commonjava.aprox.subsys.datafile.conf.DataFileConfiguration;

@ApplicationScoped
public class DataFileManager
{

    @Inject
    private DataFileConfiguration config;

    @Inject
    private DataFileEventManager fileEventManager;

    protected DataFileManager()
    {
    }

    public DataFileManager( final File rootDir, final DataFileEventManager fileEventManager )
    {
        this.fileEventManager = fileEventManager;
        this.config = new DataFileConfiguration( rootDir );
    }

    public DataFileManager( final DataFileConfiguration config, final DataFileEventManager fileEventManager )
    {
        this.config = config;
        this.fileEventManager = fileEventManager;
    }

    public DataFile getDataFile( final String... pathParts )
    {
        final File base = config.getDataBasedir();
        final File f = Paths.get( base.getAbsolutePath(), pathParts )
                            .toFile();

        return new DataFile( f, fileEventManager );
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
