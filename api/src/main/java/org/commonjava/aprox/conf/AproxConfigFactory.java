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
package org.commonjava.aprox.conf;

import org.commonjava.web.config.ConfigurationException;

public interface AproxConfigFactory
{
    String CONFIG_PATH_PROP = "aprox.config";

    String CONFIG_DIR_PROP = CONFIG_PATH_PROP + ".dir";

    String DEFAULT_CONFIG_DIR = "/etc/aprox";

    String DEFAULT_CONFIG_PATH = DEFAULT_CONFIG_DIR + "/main.conf";

    <T> T getConfiguration( Class<T> configCls )
        throws ConfigurationException;

    void load( String config )
        throws ConfigurationException;

}
