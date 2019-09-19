/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.conf;

import java.io.InputStream;

/**
 * Registration helper for the configuration subsystem, which tells the {@link IndyConfigFactory} which section of the configuration file belongs to
 * the conifguration class associated with implemenetations of this helper. It also provides information for writing out default configuration files
 * in case no config is available, in order to setup a config directory that can be managed.
 */
public interface IndyConfigInfo
{
    String APPEND_DEFAULTS_TO_MAIN_CONF = "main.conf";

    String CONF_INCLUDES_DIR = "conf.d";

    /**
     * The name of the file to be written in case no configuration is provided, to allow modification of defaults in future executions.
     * @return a filename, of the form *.conf (unless it's 'main.conf', in which case it'll be appended to the main config file), 
     *  which will be written to the etc/indy/conf.d directory.
     */
    String getDefaultConfigFileName();

    /**
     * The actual content which should be added to the default configuration file in case no configuration is provided, to allow modification of 
     * defaults in future executions.
     * 
     * @return The content, usually as a result of loading Thread.currentThread().getContextClassLoader().getResourceAsStream("foo")
     */
    InputStream getDefaultConfig();

//    /**
//     * Return the section name. Note that we cannot use the @SectionName annotation because CDI will proxy the class and
//     * lose the annotation.
//     * @return
//     */
//    String getSection();
}
