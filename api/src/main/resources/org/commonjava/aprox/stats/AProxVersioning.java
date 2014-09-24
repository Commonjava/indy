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

import javax.inject.Singleton;

@Singleton
public class AProxVersioning
{
    
    private static final String APP_VERSION = "@project.version@";
    private static final String APP_BUILDER = "@user.name@";
    private static final String APP_COMMIT_ID = "@buildNumber@";
    private static final String APP_TIMESTAMP = "@timestamp@";
    
    public String getVersion()
    {
        return APP_VERSION;
    }

    public String getBuilder()
    {
        return APP_BUILDER;
    }

    public String getCommitId()
    {
        return APP_COMMIT_ID;
    }
    
    public String getTimestamp()
    {
        return APP_TIMESTAMP;
    }

}
