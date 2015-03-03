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

import java.io.File;

import org.commonjava.web.config.ConfigurationException;

/**
 * Describes a facility for loading all the configurations related to AProx, only one of which is {@link AproxConfiguration}.
 */
public interface AproxConfigFactory
{
    String CONFIG_PATH_PROP = "aprox.config";

    String CONFIG_DIR_PROP = CONFIG_PATH_PROP + ".dir";

    String DEFAULT_CONFIG_DIR = "/etc/aprox";

    String DEFAULT_CONFIG_PATH = DEFAULT_CONFIG_DIR + "/main.conf";

    /**
     * Return the configuration instance corresponding to the given class.
     */
    <T> T getConfiguration( Class<T> configCls )
        throws ConfigurationException;

    /**
     * Read all configurations and apply them to the different configuration-class instances available.
     * <br/><b>NOTE:</b>If the main.conf doesn't exist, {@link #writeDefaultConfigs(File)} will be called.
     * 
     * @param config Most commonly, a path to a configuration file.
     */
    void load( String config )
        throws ConfigurationException;

    /**
     * Query all configuration modules for their default configuration files and content, and write them to the specified configuration directory
     * structure. The given directory is equivalent to ${aprox.home}/etc/aprox, and configuration modules are allowed to return relative paths that
     * include a subdirectory (like conf.d/foo.conf).
     * 
     * @param dir The directory into which default configurations should be written.
     * @throws ConfigurationException
     */
    void writeDefaultConfigs( final File dir )
        throws ConfigurationException;

}
