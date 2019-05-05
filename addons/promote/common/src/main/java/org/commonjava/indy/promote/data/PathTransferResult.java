package org.commonjava.indy.promote.data;

class PathTransferResult
{
    String error;

    boolean skipped;

    final String path;

    public PathTransferResult( final String path )
    {
        this.path = path;
    }
}
