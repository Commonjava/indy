package org.commonjava.aprox.content;

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
