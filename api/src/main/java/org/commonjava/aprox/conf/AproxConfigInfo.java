package org.commonjava.aprox.conf;

import java.io.InputStream;

/**
 * Registration helper for the configuration subsystem, which tells the {@link AproxConfigFactory} which section of the configuration file belongs to
 * the conifguration class associated with implemenetations of this helper. It also provides information for writing out default configuration files
 * in case no config is available, in order to setup a config directory that can be managed.
 */
public interface AproxConfigInfo
{
    String APPEND_DEFAULTS_TO_MAIN_CONF = "main.conf";

    String CONF_INCLUDES_DIR = "conf.d";

    /**
     * The name of the configuration file subsection that applies to this configuration.
     */
    String getSectionName();

    /**
     * The name of the file to be written in case no configuration is provided, to allow modification of defaults in future executions.
     * @return a filename, of the form *.conf (unless it's 'main.conf', in which case it'll be appended to the main config file), 
     *  which will be written to the etc/aprox/conf.d directory.
     */
    String getDefaultConfigFileName();

    /**
     * The actual content which should be added to the default configuration file in case no configuration is provided, to allow modification of 
     * defaults in future executions.
     * 
     * @return The content, usually as a result of loading Thread.currentThread().getContextClassLoader().getResourceAsStream("foo")
     */
    InputStream getDefaultConfig();

}
