package org.commonjava.aprox.boot;


public interface BootInterface
{

    String APROX_HOME_PROP = "aprox.home";

    String BOOT_DEFAULTS_PROP = "aprox.boot.defaults";

    int ERR_CANT_LOAD_BOOT_DEFAULTS = 1;

    int ERR_CANT_PARSE_ARGS = 2;

    int ERR_CANT_INTERP_BOOT_DEFAULTS = 3;

    int ERR_CANT_CONFIGURE_LOGGING = 4;

    BootStatus start( BootOptions bootOptions )
        throws AproxBootException;

    BootOptions getBootOptions();

    void stop();

}