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
package org.commonjava.aprox.stats;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.commonjava.aprox.stats.AProxVersioning;

@Singleton
public class AProxVersioningProvider
{
    
    private static final String APP_VERSION = "@project.version@";
    private static final String APP_BUILDER = "@user.name@";
    private static final String APP_COMMIT_ID = "@buildNumber@";
    private static final String APP_TIMESTAMP = "@timestamp@";
    
    private static AProxVersioning VERSIONING;
    static{
        VERSIONING = new AProxVersioning( APP_VERSION, APP_BUILDER, APP_COMMIT_ID, APP_TIMESTAMP );
    }
    
    public static AProxVersioning getVersioning()
    {
        return VERSIONING;
    }

    @Produces
    @Default
    public AProxVersioning getVersioningInstance()
    {
        return VERSIONING;
    }

}
