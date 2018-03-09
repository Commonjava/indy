package org.commonjava.indy.content;

import org.commonjava.maven.galley.io.SpecialPathSet;

/**
 * Furnish special path information for package types, which will be registered with
 * {@link org.commonjava.maven.galley.spi.io.SpecialPathManager} when DefaultGalleyStorageProvider
 * (in indy-filer-default) initializes.
 */
public interface SpecialPathSetProducer
{
    SpecialPathSet getSpecialPathSet();
}
