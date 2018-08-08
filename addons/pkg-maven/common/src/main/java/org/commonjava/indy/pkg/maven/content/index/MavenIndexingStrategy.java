package org.commonjava.indy.pkg.maven.content.index;

import org.commonjava.indy.content.index.PackageIndexingStrategy;
import org.commonjava.indy.util.PathUtils;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;

@ApplicationScoped
@Named
public class MavenIndexingStrategy
        implements PackageIndexingStrategy
{
    @Inject
    private SpecialPathManager specialPathManager;

    protected MavenIndexingStrategy(){}

    public MavenIndexingStrategy( SpecialPathManager specialPathManager )
    {
        this.specialPathManager = specialPathManager;
    }

    @Override
    public String getPackageType()
    {
        return PKG_TYPE_MAVEN;
    }

    @Override
    public String getIndexPath( final String rawPath )
    {
        final SpecialPathInfo info = specialPathManager.getSpecialPathInfo( rawPath );
        if ( info == null || !info.isMetadata() )
        {
            return PathUtils.getCurrentDirPath( rawPath );
        }
        else
        {
            return rawPath;
        }
    }
}
