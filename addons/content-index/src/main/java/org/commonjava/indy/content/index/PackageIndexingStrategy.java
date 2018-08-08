package org.commonjava.indy.content.index;

public interface PackageIndexingStrategy
{
    String getPackageType();

    String getIndexPath( String rawPath );
}
