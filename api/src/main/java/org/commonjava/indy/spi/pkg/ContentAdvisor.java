package org.commonjava.indy.spi.pkg;

/**
 */
public interface ContentAdvisor
{
    /**
     *
     *
     * @param path - Can be null, null means "unspecified", which should allow co-existence of multiple packaging types
     * @return
     */
    ContentQuality getContentQuality( String path );
}
