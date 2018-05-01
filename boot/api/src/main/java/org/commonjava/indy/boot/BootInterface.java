/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.boot;

/**
 * <p>Interface providing standardized methods and signals used to boot Indy.</p>
 * 
 * <p>Boot sequence (contained within {@link #start(BootOptions)} method):</p>
 * <ol>
 * <li>{@link #initialize(BootOptions)}</li>
 * <li>{@link #loadConfiguration(String)}</li>
 * <li>{@link #startLifecycle()}</li>
 * <li>{@link #deploy()}</li>
 * </ol>
 * 
 * <p>The {@link #runAndWait(BootOptions)} method calls {@link #start(BootOptions)} and then {@link #wait()} on the {@link BootInterface} instance 
 * itself, such that {@link Thread#interrupt()} is necessary to make it return. This is the method normally used from the booter <tt>main()</tt> 
 * method.</li>
 * 
 * @author jdcasey
 */
public interface BootInterface
{

    String BOOT_DEFAULTS_PROP = "indy.boot.defaults";
    
    // FIXME: These two are duplicated from IndyConfigFactory!
    String INDY_HOME_PROP = "indy.home";

    String CONFIG_PATH_PROP = "indy.config";

    String CONFIG_DIR_PROP = CONFIG_PATH_PROP + ".dir";

    @Deprecated
    // FIXME: These two are duplicated from IndyConfigFactory!
    String APROX_HOME_PROP = "aprox.home";

    @Deprecated
    String APROX_CONFIG_PATH_PROP = "aprox.config";

    @Deprecated
    String APROX_CONFIG_DIR_PROP = APROX_CONFIG_PATH_PROP + ".dir";

    int ERR_CANT_LOAD_BOOT_DEFAULTS = 1;

    int ERR_CANT_PARSE_ARGS = 2;

    int ERR_CANT_LOAD_BOOT_OPTIONS = 3;

    int ERR_CANT_CONFIGURE_LOGGING = 4;

    int ERR_CANT_CONFIGURE_INDY = 5;

    int ERR_CANT_START_INDY = 6;

    int ERR_CANT_LISTEN = 7;

    int ERR_CANT_INIT_BOOTER = 8;

    int runAndWait( final BootOptions bootOptions )
        throws IndyBootException;

    BootStatus start( BootOptions bootOptions )
        throws IndyBootException;

    BootOptions getBootOptions();

    void stop();

    boolean deploy();

    boolean startLifecycle();

    boolean loadConfiguration( final String config );

    boolean initialize( final BootOptions bootOptions );

}