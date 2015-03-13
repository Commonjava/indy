package org.commonjava.aprox.boot;

/**
 * <p>Interface providing standardized methods and signals used to boot AProx.</p>
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

    String APROX_HOME_PROP = "aprox.home";

    String BOOT_DEFAULTS_PROP = "aprox.boot.defaults";

    // FIXME: These two are duplicated from AproxConfigFactory!
    String CONFIG_PATH_PROP = "aprox.config";

    String CONFIG_DIR_PROP = CONFIG_PATH_PROP + ".dir";

    int ERR_CANT_LOAD_BOOT_DEFAULTS = 1;

    int ERR_CANT_PARSE_ARGS = 2;

    int ERR_CANT_LOAD_BOOT_OPTIONS = 3;

    int ERR_CANT_CONFIGURE_LOGGING = 4;

    int ERR_CANT_CONFIGURE_APROX = 5;

    int ERR_CANT_START_APROX = 6;

    int ERR_CANT_LISTEN = 7;

    int ERR_CANT_INIT_BOOTER = 8;

    int runAndWait( final BootOptions bootOptions )
        throws AproxBootException;

    BootStatus start( BootOptions bootOptions )
        throws AproxBootException;

    BootOptions getBootOptions();

    void stop();

    boolean deploy();

    boolean startLifecycle();

    boolean loadConfiguration( final String config );

    boolean initialize( final BootOptions bootOptions );

}