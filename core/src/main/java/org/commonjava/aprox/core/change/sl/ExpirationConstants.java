package org.commonjava.aprox.core.change.sl;

public final class ExpirationConstants
{

    private ExpirationConstants()
    {
    }

    public static final String APROX_EVENT = "aprox";

    public static final String APROX_FILE_EVENT = "file";

    // FIXME: Configurable timeout for "non-cached" files.
    //
    // Even non-cached files need to be cached for a short period, to avoid thrashing connections to the remote
    // proxy target.
    //
    // Therefore, non-cached really means cached with a very short timeout.
    public static final long NON_CACHED_TIMEOUT = 5000;

}
