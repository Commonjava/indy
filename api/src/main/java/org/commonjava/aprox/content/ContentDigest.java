package org.commonjava.aprox.content;

/**
 * Types of checksums commonly used for Maven artifacts and metadata, which may be generated via a {@link ContentGenerator} when, say, metadata files
 * are merged in a group.
 */
public enum ContentDigest
{

    MD5, SHA_256( "SHA-256" );

    private String digestName;

    private ContentDigest()
    {
        this.digestName = name();
    }

    private ContentDigest( final String digestName )
    {
        this.digestName = digestName;
    }

    public String digestName()
    {
        return digestName;
    }

}
