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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.shelflife.store.flat.FlatBlockStoreConfiguration;

@ApplicationScoped
public class DataFileShelflifeConfig
{

    @Inject
    private DataFileConfiguration config;

    @Produces
    @Default
    public FlatBlockStoreConfiguration getShelflifeConfig()
    {
        return new FlatBlockStoreConfiguration( config.getDataDir( "shelflife" ) );
    }

}
