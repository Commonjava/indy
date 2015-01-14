package org.commonjava.aprox.boot;


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

}