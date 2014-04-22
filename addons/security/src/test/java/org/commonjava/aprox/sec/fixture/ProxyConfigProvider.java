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
package org.commonjava.aprox.sec.fixture;

import java.io.File;
import java.io.IOException;

import javax.annotation.PreDestroy;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.apache.commons.io.FileUtils;
import org.commonjava.aprox.conf.AproxConfiguration;
import org.commonjava.aprox.core.conf.DefaultAproxConfiguration;
import org.commonjava.aprox.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.aprox.inject.TestData;

@javax.enterprise.context.ApplicationScoped
public class ProxyConfigProvider
{
    public static final String REPO_ROOT_DIR = "repo.root.dir";

    private AproxConfiguration config;

    //    private AdminConfiguration adminConfig;

    //    @Produces
    //    @Default
    //    @TestData
    //    public synchronized AdminConfiguration getAdminConfig()
    //    {
    //        if ( adminConfig == null )
    //        {
    //            adminConfig = new DefaultAdminConfiguration();
    //        }
    //
    //        return adminConfig;
    //    }

    @Produces
    // @TestData
    @Default
    public synchronized AproxConfiguration getProxyConfiguration()
    {
        if ( config == null )
        {
            config = new DefaultAproxConfiguration();
        }

        return config;
    }

    private File storageDir;

    @PreDestroy
    public synchronized void deleteRepoDir()
        throws IOException
    {
        FileUtils.forceDelete( storageDir );
    }

    private DefaultStorageProviderConfiguration storageConfig;

    @Produces
    @TestData
    @Default
    public synchronized DefaultStorageProviderConfiguration getStorageConfig()
        throws IOException
    {
        if ( storageConfig == null )
        {
            final String path = System.getProperty( REPO_ROOT_DIR );
            if ( path == null )
            {
                storageDir = File.createTempFile( "repo.root", ".dir" );
                storageDir.delete();
                storageDir.mkdirs();
            }
            else
            {
                storageDir = new File( path );
            }
            storageConfig = new DefaultStorageProviderConfiguration( storageDir );
        }

        return storageConfig;
    }

}
